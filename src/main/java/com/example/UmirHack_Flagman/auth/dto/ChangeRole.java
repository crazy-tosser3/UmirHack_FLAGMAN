package com.example.UmirHack_Flagman.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ChangeRole {
    @NotNull
    private Role role;
}
