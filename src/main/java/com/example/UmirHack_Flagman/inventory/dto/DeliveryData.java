package com.example.UmirHack_Flagman.inventory.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
public class DeliveryData {
    @NotBlank
    @Size(max = 100)
    private String deliveryNumber;

    @NotNull
    private Instant receivedAt;

    @NotEmpty
    @Size(max = 500)
    private List<@Valid @NotNull Item> items;

    @Setter 
    @Getter 
    public static class Item {
        @NotNull
        private UUID ingredientId;

        @NotNull
        @Positive
        @Digits(integer = 11, fraction = 3)
        private BigDecimal quantity;

        @NotNull
        private Instant expiresAt;
    }
}
