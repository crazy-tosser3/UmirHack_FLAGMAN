package com.example.UmirHack_Flagman.Entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.Getter;

// Фиксируется при отказе в заказе из-за нехватки ингредиента
@Entity
@Table(name = "stock_deficits")
@Getter
public class StockDeficitEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id")
    private IngredientEntity ingredient;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal requiredQuantity;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal availableQuantity;

    @Column(length = 100)
    private String externalOrderId;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected StockDeficitEntity() {
    }

    public StockDeficitEntity(IngredientEntity ingredient, BigDecimal required, BigDecimal available, String externalOrderId) {
        this.ingredient = ingredient;
        this.requiredQuantity = required;
        this.availableQuantity = available;
        this.externalOrderId = externalOrderId;
    }
}
