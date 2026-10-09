package com.example.UmirHack_Flagman.models;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

// event: offers.updated
@Setter 
@Getter 
public class OffersUpdatedEvent {
    private Instant generatedAt;
}
