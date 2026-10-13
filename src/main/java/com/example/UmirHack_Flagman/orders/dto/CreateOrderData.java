package com.example.UmirHack_Flagman.orders.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter 
@Getter 
public class CreateOrderData {
    @Size(max = 100)
    private String externalOrderId;

    @NotEmpty
    @Size(max = 100)
    private List<@Valid @NotNull Item> items;

    @Setter 
    @Getter 
    public static class Item {
        @NotNull
        private UUID dishId;

        @Min(1)
        @Max(1000)
        private int quantity;

        // Ингредиенты блюда, которые не нужно класть (и списывать со склада)
        @Size(max = 50)
        private List<@NotNull UUID> excludedIngredientIds;
    }
}
