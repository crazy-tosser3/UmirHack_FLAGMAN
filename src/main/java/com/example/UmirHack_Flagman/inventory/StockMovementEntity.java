package com.example.UmirHack_Flagman.inventory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.example.UmirHack_Flagman.catalog.IngredientEntity;
import com.example.UmirHack_Flagman.orders.CustomerOrderEntity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

// Неизменяемый журнал движений склада. quantity всегда положительное, направление задает type
@Entity
@Table(name = "stock_movements",
        indexes = @Index(name = "idx_movements_type_created", columnList = "type, created_at"))
@Getter
public class StockMovementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id")
    private StockBatchEntity batch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id")
    private IngredientEntity ingredient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MovementType type;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private CustomerOrderEntity order;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected StockMovementEntity() {
    }

    public StockMovementEntity(StockBatchEntity batch, MovementType type, BigDecimal quantity) {
        this.batch = batch;
        this.ingredient = batch.getIngredient();
        this.type = type;
        this.quantity = quantity;
    }
}
