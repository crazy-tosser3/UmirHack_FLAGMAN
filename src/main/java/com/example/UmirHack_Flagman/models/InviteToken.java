package com.example.UmirHack_Flagman.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class InviteToken {
    @NotBlank
    @Size(max = 200)
    private String token;
}
