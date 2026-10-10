package com.example.UmirHack_Flagman.models;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class InviteInfo {
    private String email;
    private String name;
    private Role role;
    private Instant expiresAt;
}
