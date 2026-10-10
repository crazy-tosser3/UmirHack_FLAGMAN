package com.example.UmirHack_Flagman.models;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostIngredientsList {
    @NotEmpty
    @Size(max = 100)
    private List<@Valid @NotNull Item> items;

    @Getter
    @Setter
    public static class Item {
        @NotBlank
        @Size(max = 100)
        private String name;

        @NotBlank
        @Pattern(regexp = "g|ml|pcs", message = "допустимы g, ml, pcs")
        private String unit;
    }
}