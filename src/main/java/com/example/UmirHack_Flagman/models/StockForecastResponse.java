package com.example.UmirHack_Flagman.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class StockForecastResponse {
    private Instant generatedAt;
    private int horizonDays;
    private int consumptionWindowDays;
    private List<Item> items;
    private int page;
    private int limit;
    private long total;

    @Setter
    @Getter
    public static class Item {
        private UUID dishId;
        private String dishName;
        private int availablePortions;
        // null — стоп-лист не ожидается в пределах горизонта
        private Instant stopAt;
        // already_stopped | consumption | expiry
        private String cause;
        private LimitingIngredient limitingIngredient;
    }

    @Setter
    @Getter
    public static class LimitingIngredient {
        private UUID ingredientId;
        private String ingredientName;
        private String unit;
        private BigDecimal availableQuantity;
        private BigDecimal requiredPerPortion;
        private BigDecimal dailyConsumption;
    }
}
