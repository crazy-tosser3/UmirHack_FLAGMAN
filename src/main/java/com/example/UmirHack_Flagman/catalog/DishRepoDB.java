package com.example.UmirHack_Flagman.catalog;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface DishRepoDB extends JpaRepository<DishEntity, UUID>, JpaSpecificationExecutor<DishEntity> {

    @Query("select distinct d from DishEntity d left join fetch d.ingredients di left join fetch di.ingredient")
    List<DishEntity> findAllWithIngredients();

    @Query("""
            select distinct d from DishEntity d
            left join fetch d.ingredients di left join fetch di.ingredient
            where d.id in :ids""")
    List<DishEntity> findWithIngredients(Collection<UUID> ids);

    boolean existsByNameIgnoreCase(String name);
}
