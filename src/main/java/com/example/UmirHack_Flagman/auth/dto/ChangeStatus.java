package com.example.UmirHack_Flagman.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ChangeStatus {
    @NotBlank
    private String status;
}
