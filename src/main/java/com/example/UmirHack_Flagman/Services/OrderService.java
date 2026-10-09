package com.example.UmirHack_Flagman.Services;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.UmirHack_Flagman.Entity.CustomerOrderEntity;
import com.example.UmirHack_Flagman.Entity.DishEntity;
import com.example.UmirHack_Flagman.Entity.DishIngredientEntity;
import com.example.UmirHack_Flagman.Entity.IngredientEntity;
import com.example.UmirHack_Flagman.Entity.MovementType;
import com.example.UmirHack_Flagman.Entity.OrderItemEntity;
import com.example.UmirHack_Flagman.Entity.StockBatchEntity;
import com.example.UmirHack_Flagman.Entity.StockDeficitEntity;
import com.example.UmirHack_Flagman.Entity.StockMovementEntity;
import com.example.UmirHack_Flagman.Exceptions.ApiException;
import com.example.UmirHack_Flagman.Repositories.CustomerOrderRepoDB;
import com.example.UmirHack_Flagman.Repositories.DishRepoDB;
import com.example.UmirHack_Flagman.Repositories.StockBatchRepoDB;
import com.example.UmirHack_Flagman.Repositories.StockDeficitRepoDB;
import com.example.UmirHack_Flagman.Repositories.StockMovementRepoDB;
import com.example.UmirHack_Flagman.models.ConsumptionItem;
import com.example.UmirHack_Flagman.models.ConsuptionData;
import com.example.UmirHack_Flagman.models.CreateOrderData;
import com.example.UmirHack_Flagman.models.InsufficientStockDetail;
import com.example.UmirHack_Flagman.models.OrderData;

import jakarta.persistence.EntityManager;

@Service
public class OrderService {
    private static final String SCOPE = "order";
    private static final String LOCK_TIMEOUT = "3s";

    private final CustomerOrderRepoDB orderRepo;
    private final DishRepoDB dishRepo;
    private final StockBatchRepoDB batchRepo;
    private final StockMovementRepoDB movementRepo;
    private final StockDeficitRepoDB deficitRepo;
    private final IdempotencyService idempotency;
    private final StockVersionService versions;
    private final ApplicationEventPublisher events;
    private final EntityManager em;
    private final TransactionTemplate tx;
    private final TransactionTemplate readTx;

    public OrderService(CustomerOrderRepoDB orderRepo, DishRepoDB dishRepo, StockBatchRepoDB batchRepo,
            StockMovementRepoDB movementRepo, StockDeficitRepoDB deficitRepo, IdempotencyService idempotency,
            StockVersionService versions, ApplicationEventPublisher events, EntityManager em,
            PlatformTransactionManager txManager) {
        this.orderRepo = orderRepo;
        this.dishRepo = dishRepo;
        this.batchRepo = batchRepo;
        this.movementRepo = movementRepo;
        this.deficitRepo = deficitRepo;
        this.idempotency = idempotency;
        this.versions = versions;
        this.events = events;
        this.em = em;
        this.tx = new TransactionTemplate(txManager);
        this.readTx = new TransactionTemplate(txManager);
        this.readTx.setReadOnly(true);
    }

    private sealed interface Outcome permits Confirmed, Replayed, Rejected {
    }

    private record Confirmed(UUID orderId, String externalOrderId, List<UUID> ingredientIds) implements Outcome {
    }

    private record Replayed(UUID orderId) implements Outcome {
    }

    // Отказ фиксируется коммитом (журнал дефицита), а клиенту уходит 409
    private record Rejected(List<InsufficientStockDetail> details) implements Outcome {
    }

    public OrderData create(CreateOrderData req, String idempotencyKey) {
        String externalId = req.getExternalOrderId() == null || req.getExternalOrderId().isBlank()
                ? null : req.getExternalOrderId().trim();
        Map<UUID, Integer> lines = new LinkedHashMap<>();
        req.getItems().forEach(i -> lines.merge(i.getDishId(), i.getQuantity(), Integer::sum));
        String hash = requestHash(externalId, lines);

        Outcome outcome = tx.execute(status -> place(externalId, lines, idempotencyKey, hash));

        return switch (outcome) {
            case Rejected r -> throw new ApiException(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK",
                    "Недостаточно ингредиентов для создания заказа", r.details());
            case Replayed r -> load(r.orderId(), null);
            case Confirmed c -> {
                Long version = versions.next();
                events.publishEvent(new InventoryEvents.OrderConfirmed(c.orderId(), c.externalOrderId()));
                events.publishEvent(new InventoryEvents.StockChanged(c.ingredientIds(), version));
                yield load(c.orderId(), version);
            }
        };
    }

    private Outcome place(String externalId, Map<UUID, Integer> lines, String idempotencyKey, String hash) {
        em.createNativeQuery("select set_config('lock_timeout', :timeout, true)")
                .setParameter("timeout", LOCK_TIMEOUT).getSingleResult();

        var previous = idempotency.findPrevious(SCOPE, idempotencyKey, hash);
        if (previous.isPresent()) {
            return new Replayed(previous.get());
        }
        if (externalId != null && orderRepo.existsByExternalOrderId(externalId)) {
            throw new ApiException(HttpStatus.CONFLICT, "ORDER_ALREADY_EXISTS",
                    "Заказ " + externalId + " уже зарегистрирован");
        }

        Map<UUID, DishEntity> dishes = dishRepo.findWithIngredients(lines.keySet()).stream()
                .collect(Collectors.toMap(DishEntity::getId, Function.identity()));
        List<UUID> missing = lines.keySet().stream().filter(id -> !dishes.containsKey(id)).toList();
        if (!missing.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "DISH_NOT_FOUND", "Блюдо не найдено", missing);
        }
        List<UUID> unavailable = dishes.values().stream()
                .filter(d -> !d.isActive() || d.isManualStop()).map(DishEntity::getId).toList();
        if (!unavailable.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "DISH_UNAVAILABLE", "Блюдо недоступно для заказа", unavailable);
        }

        // Суммарная потребность по ингредиентам с учетом повторов в разных блюдах
        Map<UUID, BigDecimal> need = new LinkedHashMap<>();
        Map<UUID, IngredientEntity> ingredients = new LinkedHashMap<>();
        lines.forEach((dishId, portions) -> {
            for (DishIngredientEntity di : dishes.get(dishId).getIngredients()) {
                UUID id = di.getIngredient().getId();
                need.merge(id, di.getQuantity().multiply(BigDecimal.valueOf(portions)), BigDecimal::add);
                ingredients.put(id, di.getIngredient());
            }
        });

        Map<UUID, List<StockBatchEntity>> batches = new LinkedHashMap<>();
        if (!need.isEmpty()) {
            for (StockBatchEntity b : batchRepo.lockUsable(need.keySet(), Instant.now())) {
                batches.computeIfAbsent(b.getIngredient().getId(), k -> new ArrayList<>()).add(b);
            }
        }

        List<InsufficientStockDetail> shortages = new ArrayList<>();
        need.forEach((id, required) -> {
            BigDecimal available = batches.getOrDefault(id, List.of()).stream()
                    .map(StockBatchEntity::getRemainingQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (available.compareTo(required) < 0) {
                InsufficientStockDetail d = new InsufficientStockDetail();
                d.setIngredientId(id);
                d.setRequiredQuantity(required);
                d.setAvailableQuantity(available);
                d.setMissingQuantity(required.subtract(available));
                d.setUnit(ingredients.get(id).getUnit());
                shortages.add(d);
            }
        });
        if (!shortages.isEmpty()) {
            deficitRepo.saveAll(shortages.stream().map(d -> new StockDeficitEntity(
                    ingredients.get(d.getIngredientId()), d.getRequiredQuantity(), d.getAvailableQuantity(), externalId))
                    .toList());
            return new Rejected(shortages);
        }

        CustomerOrderEntity order = new CustomerOrderEntity(externalId);
        lines.forEach((dishId, portions) -> order.getItems().add(new OrderItemEntity(order, dishes.get(dishId), portions)));
        orderRepo.save(order);

        // FEFO: партии уже отсортированы по сроку годности
        need.forEach((id, required) -> {
            BigDecimal left = required;
            for (StockBatchEntity batch : batches.get(id)) {
                if (left.signum() == 0) {
                    break;
                }
                BigDecimal take = batch.getRemainingQuantity().min(left);
                batch.setRemainingQuantity(batch.getRemainingQuantity().subtract(take));
                StockMovementEntity movement = new StockMovementEntity(batch, MovementType.ORDER_CONSUMPTION, take);
                movement.setOrder(order);
                movementRepo.save(movement);
                left = left.subtract(take);
            }
        });

        idempotency.remember(SCOPE, idempotencyKey, hash, order.getId());
        return new Confirmed(order.getId(), externalId, List.copyOf(need.keySet()));
    }

    public OrderData getById(UUID orderId) {
        return load(orderId, null);
    }

    public ConsuptionData getConsumption(UUID orderId) {
        return readTx.execute(status -> {
            if (!orderRepo.existsById(orderId)) {
                throw notFound();
            }
            ConsuptionData response = new ConsuptionData();
            response.setOrderId(orderId);
            response.setItems(consumption(orderId));
            return response;
        });
    }

    private OrderData load(UUID orderId, Long stockVersion) {
        return readTx.execute(status -> {
            CustomerOrderEntity order = orderRepo.findWithItems(orderId).orElseThrow(OrderService::notFound);

            OrderData response = new OrderData();
            response.setId(order.getId());
            response.setExternalOrderId(order.getExternalOrderId());
            response.setStatus(order.getStatus().name().toLowerCase());
            response.setCreatedAt(order.getCreatedAt());
            response.setItems(order.getItems().stream().map(i -> {
                OrderData.Item item = new OrderData.Item();
                item.setDishId(i.getDish().getId());
                item.setDishName(i.getDish().getName());
                item.setQuantity(i.getQuantity());
                return item;
            }).toList());
            response.setConsumption(consumption(orderId));
            response.setStockVersion(stockVersion);
            return response;
        });
    }

    private List<ConsumptionItem> consumption(UUID orderId) {
        Map<UUID, ConsumptionItem> byIngredient = new LinkedHashMap<>();
        for (StockMovementEntity m : movementRepo.findByOrder(orderId, MovementType.ORDER_CONSUMPTION)) {
            ConsumptionItem item = byIngredient.computeIfAbsent(m.getIngredient().getId(), id -> {
                ConsumptionItem ci = new ConsumptionItem();
                ci.setIngredientId(id);
                ci.setQuantity(BigDecimal.ZERO);
                ci.setUnit(m.getIngredient().getUnit());
                ci.setAllocations(new ArrayList<>());
                return ci;
            });
            ConsumptionItem.Allocation allocation = new ConsumptionItem.Allocation();
            allocation.setBatchId(m.getBatch().getId());
            allocation.setQuantity(m.getQuantity());
            item.getAllocations().add(allocation);
            item.setQuantity(item.getQuantity().add(m.getQuantity()));
        }
        return List.copyOf(byIngredient.values());
    }

    private static String requestHash(String externalId, Map<UUID, Integer> lines) {
        StringBuilder sb = new StringBuilder(String.valueOf(externalId));
        lines.forEach((id, qty) -> sb.append('|').append(id).append(':').append(qty));
        return Hashes.sha256(sb.toString());
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Заказ не найден");
    }
}
