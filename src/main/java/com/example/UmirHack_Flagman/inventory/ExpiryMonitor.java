package com.example.UmirHack_Flagman.inventory;

import java.math.BigDecimal;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.UmirHack_Flagman.catalog.IngredientEntity;
import com.example.UmirHack_Flagman.catalog.IngredientRepoDB;
import com.example.UmirHack_Flagman.common.IdempotencyKeyRepoDB;
import com.example.UmirHack_Flagman.events.InventoryEvents;
import com.example.UmirHack_Flagman.events.dto.BatchExpiryEvent;

// Сроки годности истекают сами по себе, без операций, поэтому проверяем их по таймеру
@Component
public class ExpiryMonitor {
    private static final Logger log = LoggerFactory.getLogger(ExpiryMonitor.class);
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    private final StockBatchRepoDB batchRepo;
    private final StockMovementRepoDB movementRepo;
    private final IngredientRepoDB ingredientRepo;
    private final IdempotencyKeyRepoDB idempotencyRepo;
    private final StockVersionService versions;
    private final ApplicationEventPublisher events;
    private final InventoryProperties props;
    private final TransactionTemplate tx;

    public ExpiryMonitor(StockBatchRepoDB batchRepo, StockMovementRepoDB movementRepo, IngredientRepoDB ingredientRepo,
            IdempotencyKeyRepoDB idempotencyRepo, StockVersionService versions, ApplicationEventPublisher events,
            InventoryProperties props, PlatformTransactionManager txManager) {
        this.batchRepo = batchRepo;
        this.movementRepo = movementRepo;
        this.ingredientRepo = ingredientRepo;
        this.idempotencyRepo = idempotencyRepo;
        this.versions = versions;
        this.events = events;
        this.props = props;
        this.tx = new TransactionTemplate(txManager);
    }

    @Scheduled(fixedDelayString = "${app.inventory.expiry-check-interval-ms:60000}", initialDelay = 5_000)
    public void check() {
        try {
            run(Instant.now());
        } catch (RuntimeException e) {
            log.error("Ошибка проверки сроков годности", e);
        }
    }

    public void run(Instant now) {
        List<BatchExpiryEvent> expired = tx.execute(status -> processExpired(now));
        List<BatchExpiryEvent> expiring = tx.execute(status -> processExpiring(now));
        tx.executeWithoutResult(status -> idempotencyRepo.deleteOlderThan(now.minus(IDEMPOTENCY_TTL)));

        expiring.forEach(e -> events.publishEvent(new InventoryEvents.BatchExpiring(e)));
        expired.forEach(e -> events.publishEvent(new InventoryEvents.BatchExpired(e)));
        if (!expired.isEmpty()) {
            Set<UUID> ids = expired.stream().map(BatchExpiryEvent::getIngredientId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            events.publishEvent(new InventoryEvents.StockChanged(ids, versions.next()));
            log.info("Истек срок годности у {} партий", expired.size());
        }
    }

    private List<BatchExpiryEvent> processExpired(Instant now) {
        List<StockBatchEntity> batches = batchRepo.lockExpiredUnprocessed(now);
        Map<UUID, IngredientEntity> ingredients = ingredients(batches);

        List<BatchExpiryEvent> result = new ArrayList<>();
        for (StockBatchEntity batch : batches) {
            batch.setExpiredProcessed(true);
            BigDecimal quantity = batch.getRemainingQuantity();
            if (quantity.signum() == 0) {
                continue;
            }
            if (props.autoWriteOff()) {
                movementRepo.save(new StockMovementEntity(batch, MovementType.EXPIRED_WRITE_OFF, quantity));
                batch.setRemainingQuantity(BigDecimal.ZERO);
            }
            BatchExpiryEvent event = payload(batch, ingredients, quantity);
            event.setWrittenOff(props.autoWriteOff());
            result.add(event);
        }
        return result;
    }

    private List<BatchExpiryEvent> processExpiring(Instant now) {
        List<StockBatchEntity> batches = batchRepo.findExpiringToNotify(now, now.plus(Duration.ofHours(props.expiryWarningHours())));
        Map<UUID, IngredientEntity> ingredients = ingredients(batches);

        List<BatchExpiryEvent> result = new ArrayList<>();
        for (StockBatchEntity batch : batches) {
            batch.setExpiringNotified(true);
            result.add(payload(batch, ingredients, batch.getRemainingQuantity()));
        }
        return result;
    }

    private Map<UUID, IngredientEntity> ingredients(List<StockBatchEntity> batches) {
        Set<UUID> ids = batches.stream().map(b -> b.getIngredient().getId()).collect(Collectors.toSet());
        return ingredientRepo.findAllById(ids).stream().collect(Collectors.toMap(IngredientEntity::getId, Function.identity()));
    }

    private static BatchExpiryEvent payload(StockBatchEntity batch, Map<UUID, IngredientEntity> ingredients, BigDecimal quantity) {
        IngredientEntity ingredient = ingredients.get(batch.getIngredient().getId());
        BatchExpiryEvent event = new BatchExpiryEvent();
        event.setBatchId(batch.getId());
        event.setIngredientId(ingredient.getId());
        event.setIngredientName(ingredient.getName());
        event.setUnit(ingredient.getUnit());
        event.setQuantity(quantity);
        event.setExpiresAt(batch.getExpiresAt());
        return event;
    }
}
