package com.example.UmirHack_Flagman.orders;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StockDeficitRepoDB extends JpaRepository<StockDeficitEntity, UUID> {

    @Query(value = """
            select i.id as ingredientId, i.name as ingredientName, i.unit as unit,
                   count(d) as deficitCount,
                   sum(d.requiredQuantity - d.availableQuantity) as missingQuantity,
                   max(d.createdAt) as lastDeficitAt
            from StockDeficitEntity d join d.ingredient i
            where d.createdAt >= :from and d.createdAt <= :to
              and (:ingredientId is null or i.id = :ingredientId)
            group by i.id, i.name, i.unit
            order by count(d) desc, max(d.createdAt) desc""",
            countQuery = """
            select count(distinct d.ingredient.id) from StockDeficitEntity d
            where d.createdAt >= :from and d.createdAt <= :to
              and (:ingredientId is null or d.ingredient.id = :ingredientId)""")
    Page<DeficitRow> aggregateByIngredient(Instant from, Instant to, UUID ingredientId, Pageable pageable);
}
