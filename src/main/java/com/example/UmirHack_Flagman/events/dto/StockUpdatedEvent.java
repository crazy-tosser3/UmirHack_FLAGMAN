package com.example.UmirHack_Flagman.events.dto;

import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

// event: stock.updated
@Setter 
@Getter 
public class StockUpdatedEvent {
    private long stockVersion;
    private List<UUID> ingredientIds;
}
