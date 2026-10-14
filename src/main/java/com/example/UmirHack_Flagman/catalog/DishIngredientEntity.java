package com.example.UmirHack_Flagman.catalog;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

// Количество ингредиента на одну порцию блюда
@Entity
@Table(name = "dish_ingredients",
        uniqueConstraints = @UniqueConstraint(columnNames = {"dish_id", "ingredient_id"}))
@Setter
@Getter
public class DishIngredientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dish_id")
    private DishEntity dish;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id")
    private IngredientEntity ingredient;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    protected DishIngredientEntity() {
    }

    public DishIngredientEntity(DishEntity dish, IngredientEntity ingredient, BigDecimal quantity) {
        this.dish = dish;
        this.ingredient = ingredient;
        this.quantity = quantity;
    }
}
