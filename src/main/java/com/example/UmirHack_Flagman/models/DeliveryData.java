package com.example.UmirHack_Flagman.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Setter 
@Getter 
public class DeliveryData {
    private String deliveryNumber;
    private Instant receivedAt;
    private List<Item> items;

    @Setter 
    @Getter 
    public static class Item {
        private UUID ingredientId;
        private BigDecimal quantity;
        private Instant expiresAt;
    }
}
