package com.example.UmirHack_Flagman.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DeficitsResponse {
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
        private long deficitCount;
        private BigDecimal missingQuantity;
        private Instant lastDeficitAt;
    }
}
