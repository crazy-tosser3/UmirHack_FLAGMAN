package com.example.UmirHack_Flagman.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum UserStatus {
    @JsonProperty("active") ACTIVE,
    @JsonProperty("pending_verification") PENDING_VERIFICATION,
    @JsonProperty("disabled") DISABLED
}
