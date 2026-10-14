package com.example.UmirHack_Flagman.menu.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Setter 
@Getter 
public class OffersResponse {
    private List<Item> items;
    private Instant generatedAt;

    @Setter 
    @Getter 
    public static class Item {
        private UUID dishId;
        private String dishName;
        private double priority;
        private int availablePortions;
        private List<Reason> reasons;
        private Instant calculatedAt;
    }

    @Setter 
    @Getter 
    public static class Reason {
        private UUID ingredientId;
        private String ingredientName;
        private Instant expiresAt;
        private BigDecimal availableQuantity;
        private String unit;
        private String explanation;
    }
}
