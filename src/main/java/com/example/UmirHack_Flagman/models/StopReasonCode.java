package com.example.UmirHack_Flagman.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum StopReasonCode {
    @JsonProperty("insufficient_stock") INSUFFICIENT_STOCK,
    @JsonProperty("expired_ingredient") EXPIRED_INGREDIENT,
    @JsonProperty("manual") MANUAL
}
