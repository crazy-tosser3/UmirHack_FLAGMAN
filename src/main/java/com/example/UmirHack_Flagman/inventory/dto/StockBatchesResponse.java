package com.example.UmirHack_Flagman.inventory.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Setter 
@Getter 
public class StockBatchesResponse {
    private List<Item> items;
    private int page;
    private int limit;
    private long total;

    @Setter 
    @Getter 
    public static class Item {
        private UUID batchId;
        private UUID deliveryId;
        private UUID ingredientId;
        private String ingredientName;
        private BigDecimal receivedQuantity;
        private BigDecimal remainingQuantity;
        private BigDecimal reservedQuantity;
        private BigDecimal availableQuantity;
        private Instant expiresAt;
        private BatchStatus expiryStatus;
        // 0 для просроченной партии
        private long remainingShelfLifeSeconds;
    }
}
