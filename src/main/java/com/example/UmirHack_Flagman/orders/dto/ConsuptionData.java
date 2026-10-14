package com.example.UmirHack_Flagman.orders.dto;

import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Setter 
@Getter 
public class ConsuptionData {
    private UUID orderId;
    private List<ConsumptionItem> items;
}
