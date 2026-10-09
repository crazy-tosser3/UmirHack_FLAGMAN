package com.example.UmirHack_Flagman.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Setter 
@Getter 
public class StockResponse {
    private List<Item> items;
    private int page;
    private int limit;
    private long total;

    @Setter 
    @Getter 
    public static class Item {
        private UUID ingredientId;
        private String name;
        private String unit;
        private BigDecimal totalQuantity;
        private BigDecimal availableQuantity;
        private BigDecimal reservedQuantity;
        private BigDecimal expiredQuantity;
        private Instant nearestExpiryAt;
    }
}
