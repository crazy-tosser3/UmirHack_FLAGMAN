package com.example.UmirHack_Flagman.models;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Setter 
@Getter 
public class OrderData {
    private UUID id;
    private String externalOrderId;
    private String status;
    private Instant createdAt;
    private List<Item> items;
    private List<ConsumptionItem> consumption;
    private Long stockVersion;

    @Setter 
    @Getter 
    public static class Item {
        private UUID dishId;
        private String dishName;
        private int quantity;
    }
}
