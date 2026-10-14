package com.example.UmirHack_Flagman.auth.dto;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class InviteResponse {
    private String inviteUrl;
    private Instant expiresAt;
    private int clicks;
}
