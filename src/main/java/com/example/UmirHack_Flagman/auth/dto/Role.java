package com.example.UmirHack_Flagman.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Role {
    @JsonProperty("admin") ADMIN,
    @JsonProperty("manager") MANAGER,
    @JsonProperty("employee") EMPLOYEE
}
