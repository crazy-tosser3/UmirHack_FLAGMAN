package com.example.UmirHack_Flagman.auth.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Setter 
@Getter 
public class UserResponse {
    private UUID id;
    private String email;
    private String name;
    private Role role;
    private UserStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    // null — Telegram не привязан
    private String telegramUsername;
    private boolean telegramLinked;
}
