package com.example.UmirHack_Flagman.inventory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.UmirHack_Flagman.common.IdSum;

public interface StockMovementRepoDB extends JpaRepository<StockMovementEntity, UUID> {

    @Query(value = """
            select i.id as ingredientId, i.name as ingredientName, i.unit as unit,
                   sum(m.quantity) as quantity, count(distinct m.batch.id) as batchesCount
            from StockMovementEntity m join m.ingredient i
            where m.type = :type and m.createdAt >= :from and m.createdAt <= :to
              and (:ingredientId is null or i.id = :ingredientId)
            group by i.id, i.name, i.unit
            order by sum(m.quantity) desc""",
            countQuery = """
            select count(distinct m.ingredient.id) from StockMovementEntity m
            where m.type = :type and m.createdAt >= :from and m.createdAt <= :to
              and (:ingredientId is null or m.ingredient.id = :ingredientId)""")
    Page<WriteOffRow> sumByIngredient(MovementType type, Instant from, Instant to, UUID ingredientId, Pageable pageable);

    @Query("""
            select m.ingredient.id as id, sum(m.quantity) as total
            from StockMovementEntity m
            where m.type = :type and m.createdAt >= :since
            group by m.ingredient.id""")
    List<IdSum> totalsSince(MovementType type, Instant since);

    @Query("select min(m.createdAt) from StockMovementEntity m where m.type = :type")
    Instant firstAt(MovementType type);

    @Query("""
            select m from StockMovementEntity m join fetch m.ingredient
            where m.order.id = :orderId and m.type = :type
            order by m.ingredient.name, m.createdAt""")
    List<StockMovementEntity> findByOrder(UUID orderId, MovementType type);

    @Query("""
            select i.id as ingredientId, i.name as ingredientName, i.unit as unit,
                   coalesce(sum(case when m.type = :consumption then m.quantity else 0 end), 0) as consumed,
                   coalesce(sum(case when m.type = :writeOff then m.quantity else 0 end), 0) as writtenOff
            from StockMovementEntity m join m.ingredient i
            where m.createdAt >= :from and m.createdAt < :to and m.type in (:consumption, :writeOff)
            group by i.id, i.name, i.unit
            order by sum(m.quantity) desc, i.name""")
    List<IngredientDayRow> usageByIngredient(Instant from, Instant to, MovementType consumption, MovementType writeOff);
}
