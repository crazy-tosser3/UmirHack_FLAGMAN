package com.example.UmirHack_Flagman.inventory.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum BatchStatus {
    @JsonProperty("valid") VALID,
    @JsonProperty("expiring") EXPIRING,
    @JsonProperty("expired") EXPIRED
}
