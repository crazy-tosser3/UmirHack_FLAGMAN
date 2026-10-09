package com.example.UmirHack_Flagman.models;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Setter 
@Getter 
public class OrderList {
    private List<Item> items;
    private int page;
    private int limit;
    private long total;

    @Setter 
    @Getter 
    public static class Item {
        private UUID id;
        private String externalOrderId;
        private String status;
        private Instant createdAt;
        private int itemsCount;
    }
}
