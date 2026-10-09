package com.example.UmirHack_Flagman.models;

import lombok.Getter;
import lombok.Setter;

@Setter 
@Getter 
public class TokenResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private long expiresIn;
}
