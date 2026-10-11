package com.example.UmirHack_Flagman.models;

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
