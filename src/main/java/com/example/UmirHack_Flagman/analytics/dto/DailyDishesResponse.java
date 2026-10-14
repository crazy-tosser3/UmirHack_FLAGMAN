package com.example.UmirHack_Flagman.analytics.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DailyDishesResponse {
    private LocalDate date;
    // Границы дня в часовом поясе ресторана
    private Instant from;
    private Instant to;
    private long totalPortions;
    private long totalOrders;
    private List<Item> items;

    @Setter
    @Getter
    public static class Item {
        private UUID dishId;
        private String dishName;
        private long portions;
        // В скольких заказах встречалось блюдо
        private long orders;
    }
}
