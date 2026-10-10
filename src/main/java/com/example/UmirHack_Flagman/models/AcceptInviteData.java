package com.example.UmirHack_Flagman.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AcceptInviteData {
    @NotBlank
    @Size(max = 200)
    private String token;

    @NotBlank
    @Size(min = 12, max = 128)
    private String password;
}
