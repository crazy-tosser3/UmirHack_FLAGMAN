package com.example.UmirHack_Flagman.orders;

import java.util.HashSet;
import java.util.Set;
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

    // Ингредиенты, исключенные из блюда в этой позиции заказа
    @ElementCollection
    @CollectionTable(name = "order_item_excluded_ingredients", joinColumns = @JoinColumn(name = "order_item_id"))
    @Column(name = "ingredient_id", nullable = false)
    private Set<UUID> excludedIngredientIds = new HashSet<>();

    protected OrderItemEntity() {
    }

    public OrderItemEntity(CustomerOrderEntity order, DishEntity dish, int quantity, Set<UUID> excludedIngredientIds) {
        this.order = order;
        this.dish = dish;
        this.quantity = quantity;
        this.excludedIngredientIds = new HashSet<>(excludedIngredientIds);
    }
}
