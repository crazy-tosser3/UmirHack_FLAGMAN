package com.example.UmirHack_Flagman.catalog.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CreateDish {
    @NotEmpty
    @Size(max = 100)
    private List<@Valid @NotNull Item> items;

    @Setter
    @Getter
    public static class Item {
        @NotBlank
        @Size(max = 100)
        private String name;

        @JsonProperty("isActive")
        private boolean active = true;

        @NotEmpty
        @Size(max = 50)
        private List<@Valid @NotNull Ingredient> ingredients;
    }

    @Setter
    @Getter
    public static class Ingredient {
        @NotNull
        private UUID ingredientId;

        @NotNull
        @Positive
        @Digits(integer = 11, fraction = 3)
        private BigDecimal quantity;
    }
}
