package com.example.UmirHack_Flagman.models;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

// элемент error.details для 409 INSUFFICIENT_STOCK
@Setter 
@Getter 
public class InsufficientStockDetail {
    private UUID ingredientId;
    private BigDecimal requiredQuantity;
    private BigDecimal availableQuantity;
    private BigDecimal missingQuantity;
    private String unit;
}
