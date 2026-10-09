package com.example.UmirHack_Flagman.models;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LoginData {
    @NotBlank
    private String email;

    @NotBlank
    private String password;
}
