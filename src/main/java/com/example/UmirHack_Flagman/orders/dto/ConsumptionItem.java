package com.example.UmirHack_Flagman.orders.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Setter 
@Getter 
public class ConsumptionItem {
    private UUID ingredientId;
    private BigDecimal quantity;
    private String unit;
    private List<Allocation> allocations;

    @Setter 
    @Getter 
    public static class Allocation {
        private UUID batchId;
        private BigDecimal quantity;
    }
}
