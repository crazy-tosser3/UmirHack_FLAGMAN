package com.example.UmirHack_Flagman.models;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

// Для страницы регистрации по ссылке: какую роль получит пользователь
@Setter
@Getter
public class InviteInfo {
    private Role role;
    private Instant expiresAt;
    // null — без ограничений
    private Integer remainingClicks;
}
