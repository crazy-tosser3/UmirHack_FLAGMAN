package com.example.UmirHack_Flagman.menu.dto;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ManualStopResponse {
    private UUID dishId;
    private String dishName;
    private boolean manualStop;
    @JsonProperty("isAvailable")
    private boolean available;
    private List<StopListResponse.Reason> reasons;
}
