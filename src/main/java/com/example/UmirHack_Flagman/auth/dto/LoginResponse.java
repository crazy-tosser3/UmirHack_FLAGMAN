package com.example.UmirHack_Flagman.auth.dto;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Setter 
@Getter 
public class LoginResponse extends TokenResponse {
    private User user;

    @Setter 
    @Getter 
    public static class User {
        private UUID id;
        private String email;
        private String name;
        private Role role;
    }
}
