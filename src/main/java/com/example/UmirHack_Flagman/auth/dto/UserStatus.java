package com.example.UmirHack_Flagman.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum UserStatus {
    @JsonProperty("active") ACTIVE,
    @JsonProperty("pending") PENDING_VERIFICATION,
    @JsonProperty("blocked") DISABLED
}
