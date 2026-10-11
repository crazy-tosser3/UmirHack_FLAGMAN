package com.example.UmirHack_Flagman.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RegisterData {
    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @NotBlank
    @Size(min = 8, max = 128)
    private String password;

    @NotBlank
    @Size(max = 100)
    private String name;

    // Код из ссылки-приглашения, без него зарегистрироваться нельзя. Роль берется из приглашения
    @NotBlank
    @Size(max = 100)
    private String code;
}
