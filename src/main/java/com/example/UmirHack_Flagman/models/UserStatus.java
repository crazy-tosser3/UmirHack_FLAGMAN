package com.example.UmirHack_Flagman.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum UserStatus {
    @JsonProperty("active") ACTIVE,
    @JsonProperty("pending") PENDING_VERIFICATION,
    @JsonProperty("blocked") DISABLED
}
