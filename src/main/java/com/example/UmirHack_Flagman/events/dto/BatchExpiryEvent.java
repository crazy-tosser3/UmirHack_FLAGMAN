package com.example.UmirHack_Flagman.events.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

// event: batch.expiring | batch.expired
@Setter
@Getter
public class BatchExpiryEvent {
    private UUID batchId;
    private UUID ingredientId;
    private String ingredientName;
    private String unit;
    private BigDecimal quantity;
    private Instant expiresAt;
    // только для batch.expired
    private Boolean writtenOff;
}
