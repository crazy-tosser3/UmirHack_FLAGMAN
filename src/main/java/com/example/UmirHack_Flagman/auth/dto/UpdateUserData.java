package com.example.UmirHack_Flagman.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// PATCH: не переданное поле не меняется
@Setter
@Getter
public class UpdateUserData {
    @Size(max = 100)
    @Pattern(regexp = ".*\\S.*", message = "must not be blank")
    private String name;

    @Email
    @Size(max = 255)
    @Pattern(regexp = ".*\\S.*", message = "must not be blank")
    private String email;
}
