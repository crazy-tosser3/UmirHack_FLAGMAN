package com.example.UmirHack_Flagman.models;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Setter 
@Getter 
public class DishList {
    private List<Item> items;
    private int page;
    private int limit;
    private long total;

    @Setter 
    @Getter 
    public static class Item {
        private UUID id;
        private String name;
        @JsonProperty("isActive")
        private boolean active;
        private List<Ingredient> ingredients;
    }

    @Setter 
    @Getter 
    public static class Ingredient {
        private UUID ingredientId;
        private String ingredientName;
        private BigDecimal quantity;
        private String unit;
    }
}
