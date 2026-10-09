package com.example.UmirHack_Flagman.Entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "stock_batches",
        indexes = @Index(name = "idx_batches_ingredient_expires", columnList = "ingredient_id, expires_at"))
@Setter
@Getter
public class StockBatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_id")
    private DeliveryEntity delivery;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id")
    private IngredientEntity ingredient;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal receivedQuantity;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal remainingQuantity;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private boolean expiringNotified = false;

    @Column(nullable = false)
    private boolean expiredProcessed = false;

    @Version
    private long version;

    protected StockBatchEntity() {
    }

    public StockBatchEntity(DeliveryEntity delivery, IngredientEntity ingredient, BigDecimal quantity, Instant expiresAt) {
        this.delivery = delivery;
        this.ingredient = ingredient;
        this.receivedQuantity = quantity;
        this.remainingQuantity = quantity;
        this.expiresAt = expiresAt;
    }
}
