package com.example.UmirHack_Flagman.orders;

import java.util.UUID;

import com.example.UmirHack_Flagman.catalog.DishEntity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "order_items")
@Setter
@Getter
public class OrderItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private CustomerOrderEntity order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dish_id")
    private DishEntity dish;

    @Column(nullable = false)
    private int quantity;

    protected OrderItemEntity() {
    }

    public OrderItemEntity(CustomerOrderEntity order, DishEntity dish, int quantity) {
        this.order = order;
        this.dish = dish;
        this.quantity = quantity;
    }
}
