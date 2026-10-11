package com.example.UmirHack_Flagman.orders.dto;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Setter 
@Getter 
public class InsufficientStockDetail {
    private UUID ingredientId;
    private BigDecimal requiredQuantity;
    private BigDecimal availableQuantity;
    private BigDecimal missingQuantity;
    private String unit;
}
