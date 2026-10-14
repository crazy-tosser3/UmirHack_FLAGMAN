package com.example.UmirHack_Flagman.events.dto;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

// event: order.confirmed
@Setter
@Getter
public class OrderConfirmedEvent {
    private UUID orderId;
    private String externalOrderId;
}
