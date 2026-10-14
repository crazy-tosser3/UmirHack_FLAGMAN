package com.example.UmirHack_Flagman.catalog;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface IngredientRepoDB extends JpaRepository<IngredientEntity, UUID>, JpaSpecificationExecutor<IngredientEntity> {

    boolean existsByNameIgnoreCase(String name);
}
