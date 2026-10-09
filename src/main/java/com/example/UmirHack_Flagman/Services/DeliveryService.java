package com.example.UmirHack_Flagman.Services;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.UmirHack_Flagman.Entity.DeliveryEntity;
import com.example.UmirHack_Flagman.Entity.IngredientEntity;
import com.example.UmirHack_Flagman.Entity.MovementType;
import com.example.UmirHack_Flagman.Entity.StockBatchEntity;
import com.example.UmirHack_Flagman.Entity.StockMovementEntity;
import com.example.UmirHack_Flagman.Exceptions.ApiException;
import com.example.UmirHack_Flagman.Repositories.DeliveryRepoDB;
import com.example.UmirHack_Flagman.Repositories.IngredientRepoDB;
import com.example.UmirHack_Flagman.Repositories.StockBatchRepoDB;
import com.example.UmirHack_Flagman.Repositories.StockMovementRepoDB;
import com.example.UmirHack_Flagman.models.DeliveryData;
import com.example.UmirHack_Flagman.models.DeliveryResponse;
import com.example.UmirHack_Flagman.models.ValidationErrorDetail;

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
