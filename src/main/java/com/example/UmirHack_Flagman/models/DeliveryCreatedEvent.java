package com.example.UmirHack_Flagman.models;

import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

// event: delivery.created
@Setter
@Getter
public class DeliveryCreatedEvent {
    private UUID deliveryId;
    private String deliveryNumber;
    private List<UUID> ingredientIds;
}
