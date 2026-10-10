package com.example.UmirHack_Flagman.Repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.example.UmirHack_Flagman.Entity.IngredientEntity;

public interface IngredientRepoDB extends JpaRepository<IngredientEntity, UUID>, JpaSpecificationExecutor<IngredientEntity> {

    boolean existsByNameIgnoreCase(String name);
}
