package com.example.UmirHack_Flagman.events.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

// event: stop-list.updated
@Setter 
@Getter 
public class StopListUpdatedEvent {
    private Instant updatedAt;
    private List<UUID> affectedDishIds;
}
