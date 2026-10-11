package com.example.UmirHack_Flagman.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class WriteOffsResponse {
    private Instant from;
    private Instant to;
    private List<Item> items;
    private int page;
    private int limit;
    private long total;

    @Setter
    @Getter
    public static class Item {
        private UUID ingredientId;
        private String ingredientName;
        private String unit;
        private BigDecimal quantity;
        private long batchesCount;
    }
}
