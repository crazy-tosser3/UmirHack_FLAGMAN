package com.example.UmirHack_Flagman.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class InviteToken {
    @NotBlank
    @Size(max = 100)
    private String code;
}
