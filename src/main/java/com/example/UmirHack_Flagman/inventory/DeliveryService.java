package com.example.UmirHack_Flagman.inventory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.UmirHack_Flagman.catalog.IngredientEntity;
import com.example.UmirHack_Flagman.catalog.IngredientRepoDB;
import com.example.UmirHack_Flagman.common.ApiException;
import com.example.UmirHack_Flagman.common.Hashes;
import com.example.UmirHack_Flagman.common.IdCount;
import com.example.UmirHack_Flagman.common.IdempotencyService;
import com.example.UmirHack_Flagman.common.Paging;
import com.example.UmirHack_Flagman.common.dto.ValidationErrorDetail;
import com.example.UmirHack_Flagman.events.InventoryEvents;
import com.example.UmirHack_Flagman.inventory.dto.AllDeliveryResponse;
import com.example.UmirHack_Flagman.inventory.dto.DeliveryData;
import com.example.UmirHack_Flagman.inventory.dto.DeliveryResponse;

import jakarta.persistence.criteria.Predicate;

@Service
public class DeliveryService {
    private static final String SCOPE = "delivery";
    private static final Duration MAX_CLOCK_SKEW = Duration.ofMinutes(5);

    private final DeliveryRepoDB deliveryRepo;
    private final StockBatchRepoDB batchRepo;
    private final StockMovementRepoDB movementRepo;
    private final IngredientRepoDB ingredientRepo;
    private final IdempotencyService idempotency;
    private final StockVersionService versions;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate tx;
    private final TransactionTemplate readTx;

    public DeliveryService(DeliveryRepoDB deliveryRepo, StockBatchRepoDB batchRepo, StockMovementRepoDB movementRepo,
            IngredientRepoDB ingredientRepo, IdempotencyService idempotency, StockVersionService versions,
            ApplicationEventPublisher events, PlatformTransactionManager txManager) {
        this.deliveryRepo = deliveryRepo;
        this.batchRepo = batchRepo;
        this.movementRepo = movementRepo;
        this.ingredientRepo = ingredientRepo;
        this.idempotency = idempotency;
        this.versions = versions;
        this.events = events;
        this.tx = new TransactionTemplate(txManager);
        this.readTx = new TransactionTemplate(txManager);
        this.readTx.setReadOnly(true);
    }

    private record Result(UUID deliveryId, boolean created, Set<UUID> ingredientIds) {
    }

    public DeliveryResponse create(DeliveryData req, String idempotencyKey) {
        Instant now = Instant.now();
        validate(req, now);
        String number = req.getDeliveryNumber().trim();
        String hash = requestHash(number, req);

        Result result = tx.execute(status -> {
            var previous = idempotency.findPrevious(SCOPE, idempotencyKey, hash);
            if (previous.isPresent()) {
                return new Result(previous.get(), false, Set.of());
            }
            if (deliveryRepo.existsByDeliveryNumber(number)) {
                throw new ApiException(HttpStatus.CONFLICT, "DELIVERY_ALREADY_EXISTS",
                        "Поставка с номером " + number + " уже зарегистрирована");
            }

            Set<UUID> ingredientIds = req.getItems().stream()
                    .map(DeliveryData.Item::getIngredientId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            Map<UUID, IngredientEntity> ingredients = ingredientRepo.findAllById(ingredientIds).stream()
                    .collect(Collectors.toMap(IngredientEntity::getId, Function.identity()));
            List<UUID> missing = ingredientIds.stream().filter(id -> !ingredients.containsKey(id)).toList();
            if (!missing.isEmpty()) {
                throw new ApiException(HttpStatus.NOT_FOUND, "INGREDIENT_NOT_FOUND", "Ингредиент не найден", missing);
            }

            DeliveryEntity delivery = deliveryRepo.save(new DeliveryEntity(number, req.getReceivedAt()));
            for (DeliveryData.Item item : req.getItems()) {
                StockBatchEntity batch = batchRepo.save(new StockBatchEntity(
                        delivery, ingredients.get(item.getIngredientId()), item.getQuantity(), item.getExpiresAt()));
                movementRepo.save(new StockMovementEntity(batch, MovementType.RECEIPT, item.getQuantity()));
            }
            idempotency.remember(SCOPE, idempotencyKey, hash, delivery.getId());
            return new Result(delivery.getId(), true, ingredientIds);
        });

        if (result.created()) {
            events.publishEvent(new InventoryEvents.DeliveryCreated(result.deliveryId(), number, result.ingredientIds()));
            events.publishEvent(new InventoryEvents.StockChanged(result.ingredientIds(), versions.next()));
        }
        return getById(result.deliveryId());
    }

    public AllDeliveryResponse getDeliveries(int page, int limit, String search, Instant from, Instant to) {
        Paging.checkRange(from, to);
        Pageable pageable = Paging.pageable(page, limit, Sort.by(Sort.Direction.DESC, "receivedAt"));

        Specification<DeliveryEntity> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                p.add(cb.like(cb.lower(root.get("deliveryNumber")), Paging.likePattern(search), '\\'));
            }
            if (from != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("receivedAt"), from));
            }
            if (to != null) {
                p.add(cb.lessThanOrEqualTo(root.get("receivedAt"), to));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };

        return readTx.execute(status -> {
            Page<DeliveryEntity> result = deliveryRepo.findAll(spec, pageable);
            Map<UUID, Long> counts = IdCount.toMap(batchRepo.countByDeliveryIds(
                    result.getContent().stream().map(DeliveryEntity::getId).toList()));

            AllDeliveryResponse response = new AllDeliveryResponse();
            response.setItems(result.map(d -> {
                AllDeliveryResponse.Item item = new AllDeliveryResponse.Item();
                item.setId(d.getId());
                item.setDeliveryNumber(d.getDeliveryNumber());
                item.setReceivedAt(d.getReceivedAt());
                item.setStatus(d.getStatus().name().toLowerCase(Locale.ROOT));
                item.setItemsCount(counts.getOrDefault(d.getId(), 0L).intValue());
                return item;
            }).getContent());
            response.setPage(page);
            response.setLimit(limit);
            response.setTotal(result.getTotalElements());
            return response;
        });
    }

    public DeliveryResponse getById(UUID deliveryId) {
        return readTx.execute(status -> {
            DeliveryEntity delivery = deliveryRepo.findById(deliveryId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DELIVERY_NOT_FOUND", "Поставка не найдена"));

            DeliveryResponse response = new DeliveryResponse();
            response.setId(delivery.getId());
            response.setDeliveryNumber(delivery.getDeliveryNumber());
            response.setCreatedAt(delivery.getCreatedAt());
            response.setReceivedAt(delivery.getReceivedAt());
            response.setStatus(delivery.getStatus().name().toLowerCase());
            response.setItems(batchRepo.findByDeliveryIdOrderByExpiresAt(deliveryId).stream().map(b -> {
                DeliveryResponse.Item item = new DeliveryResponse.Item();
                item.setBatchId(b.getId());
                item.setIngredientId(b.getIngredient().getId());
                item.setReceivedQuantity(b.getReceivedQuantity());
                item.setRemainingQuantity(b.getRemainingQuantity());
                item.setExpiresAt(b.getExpiresAt());
                return item;
            }).toList());
            return response;
        });
    }

    private static void validate(DeliveryData req, Instant now) {
        if (req.getReceivedAt().isAfter(now.plus(MAX_CLOCK_SKEW))) {
            throw Paging.validation("receivedAt не может быть в будущем");
        }
        // Политика приемки: просроченный товар не принимается
        List<ValidationErrorDetail> expired = new ArrayList<>();
        for (int i = 0; i < req.getItems().size(); i++) {
            if (!req.getItems().get(i).getExpiresAt().isAfter(now)) {
                ValidationErrorDetail d = new ValidationErrorDetail();
                d.setField("items[" + i + "].expiresAt");
                d.setReason("already_expired");
                expired.add(d);
            }
        }
        if (!expired.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Поставка содержит просроченные партии", expired);
        }
    }

    private static String requestHash(String number, DeliveryData req) {
        StringBuilder sb = new StringBuilder(number).append('|').append(req.getReceivedAt());
        for (DeliveryData.Item item : req.getItems()) {
            sb.append('|').append(item.getIngredientId())
                    .append(':').append(item.getQuantity().stripTrailingZeros().toPlainString())
                    .append(':').append(item.getExpiresAt());
        }
        return Hashes.sha256(sb.toString());
    }
}
