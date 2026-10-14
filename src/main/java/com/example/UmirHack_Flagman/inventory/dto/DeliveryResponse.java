package com.example.UmirHack_Flagman.inventory.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Setter 
@Getter 
public class DeliveryResponse {
    private UUID id;
    private String deliveryNumber;
    private Instant createdAt;
    private Instant receivedAt;
    private String status;
    private List<Item> items;

    @Setter 
    @Getter 
    public static class Item {
        private UUID batchId;
        private UUID ingredientId;
        private BigDecimal receivedQuantity;
        private BigDecimal remainingQuantity;
        private Instant expiresAt;
    }
}
