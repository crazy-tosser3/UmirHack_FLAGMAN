package com.example.UmirHack_Flagman.models;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;

// Ответ на смену роли или статуса: только измененное поле
@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserChangeResponse {
    private UUID id;
    private Role role;
    private UserStatus status;
    private Instant updatedAt;
}
