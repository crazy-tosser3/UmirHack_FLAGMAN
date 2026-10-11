package com.example.UmirHack_Flagman.models;

import java.time.Instant;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Setter 
@Getter 
public class CreateInvite {
    @NotNull
    private Role role;

    private Instant expiresAt;

    @Min(0)
    @Max(1000)
    private Integer clicks;
}
