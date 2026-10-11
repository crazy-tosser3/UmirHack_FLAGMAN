package com.example.UmirHack_Flagman.Entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ingredients")
@Setter
@Getter
public class IngredientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, length = 8)
    private String unit;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected IngredientEntity() {
    }

    public IngredientEntity(String name, String unit) {
        this.name = name;
        this.unit = unit;
    }
}
