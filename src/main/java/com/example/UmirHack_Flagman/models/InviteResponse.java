package com.example.UmirHack_Flagman.models;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class InviteResponse {
    private String code;
    private String inviteUrl;
    private Role role;
    private Instant expiresAt;
    // 0 — без ограничений
    private int clicks;
}
