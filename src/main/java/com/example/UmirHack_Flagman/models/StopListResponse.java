package com.example.UmirHack_Flagman.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Setter 
@Getter 
public class StopListResponse {
    private List<Item> items;
    private int page;
    private int limit;
    private long total;

    @Setter 
    @Getter 
    public static class Item {
        private UUID dishId;
        private String dishName;
        @JsonProperty("isAvailable")
        private boolean available;
        private List<Reason> reasons;
        private Instant updatedAt;
    }

    @Setter 
    @Getter 
    public static class Reason {
        private StopReasonCode code;
        private UUID ingredientId;
        private String ingredientName;
        private BigDecimal requiredQuantity;
        private BigDecimal availableQuantity;
        private String unit;
    }
}
