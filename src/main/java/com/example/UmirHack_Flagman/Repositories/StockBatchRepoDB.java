package com.example.UmirHack_Flagman.Repositories;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.example.UmirHack_Flagman.Entity.StockBatchEntity;

import jakarta.persistence.LockModeType;

public interface StockBatchRepoDB extends JpaRepository<StockBatchEntity, UUID>, JpaSpecificationExecutor<StockBatchEntity> {

    @Query("""
            select b.delivery.id as id, count(b) as cnt
            from StockBatchEntity b
            where b.delivery.id in :ids
            group by b.delivery.id""")
    List<IdCount> countByDeliveryIds(Collection<UUID> ids);

    @Query("""
            select b from StockBatchEntity b
            where b.expiresAt > :now and b.remainingQuantity > 0
            order by b.expiresAt""")
    List<StockBatchEntity> findUsable(Instant now);

    List<StockBatchEntity> findByDeliveryIdOrderByExpiresAt(UUID deliveryId);

    // Порядок блокировки фиксирован, чтобы параллельные заказы не ловили deadlock
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select b from StockBatchEntity b
            where b.ingredient.id in :ingredientIds and b.expiresAt > :now and b.remainingQuantity > 0
            order by b.expiresAt, b.id""")
    List<StockBatchEntity> lockUsable(Collection<UUID> ingredientIds, Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select b from StockBatchEntity b
            where b.expiresAt <= :now and b.expiredProcessed = false
            order by b.id""")
    List<StockBatchEntity> lockExpiredUnprocessed(Instant now);

    @Query("""
            select b from StockBatchEntity b
            where b.expiresAt > :now and b.expiresAt <= :until
              and b.expiringNotified = false and b.remainingQuantity > 0""")
    List<StockBatchEntity> findExpiringToNotify(Instant now, Instant until);

    @Query("""
            select b.ingredient.id as id,
                   coalesce(sum(case when b.expiresAt > :now then b.remainingQuantity else 0 end), 0) as usable,
                   coalesce(sum(case when b.expiresAt <= :now then b.remainingQuantity else 0 end), 0) as expired,
                   min(case when b.expiresAt > :now then b.expiresAt end) as nearestExpiryAt
            from StockBatchEntity b
            where b.remainingQuantity > 0
            group by b.ingredient.id""")
    List<IngredientStockRow> aggregateAll(Instant now);

    @Query("""
            select b.ingredient.id as id,
                   coalesce(sum(case when b.expiresAt > :now then b.remainingQuantity else 0 end), 0) as usable,
                   coalesce(sum(case when b.expiresAt <= :now then b.remainingQuantity else 0 end), 0) as expired,
                   min(case when b.expiresAt > :now then b.expiresAt end) as nearestExpiryAt
            from StockBatchEntity b
            where b.remainingQuantity > 0 and b.ingredient.id in :ids
            group by b.ingredient.id""")
    List<IngredientStockRow> aggregateFor(Instant now, Collection<UUID> ids);
}
