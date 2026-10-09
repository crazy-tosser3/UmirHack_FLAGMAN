package com.example.UmirHack_Flagman.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SupplySuggestionsResponse {
    private Instant generatedAt;
    private int coverDays;
    private List<Item> items;

    @Setter
    @Getter
    public static class Item {
        private UUID ingredientId;
        private String ingredientName;
        private String unit;
        private BigDecimal availableQuantity;
        private BigDecimal dailyConsumption;
        // когда пригодный остаток опустится ниже нормы на порцию; null — не в пределах горизонта
        private Instant depletionAt;
        private BigDecimal suggestedQuantity;
    }
}
