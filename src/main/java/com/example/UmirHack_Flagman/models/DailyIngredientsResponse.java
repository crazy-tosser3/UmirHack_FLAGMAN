package com.example.UmirHack_Flagman.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DailyIngredientsResponse {
    private LocalDate date;
    // Границы дня в часовом поясе ресторана
    private Instant from;
    private Instant to;
    private List<Item> items;

    @Setter
    @Getter
    public static class Item {
        private UUID ingredientId;
        private String ingredientName;
        private String unit;
        // Списано под заказы
        private BigDecimal consumedQuantity;
        // Списано как просроченное
        private BigDecimal writtenOffQuantity;
        private BigDecimal totalQuantity;
    }
}
