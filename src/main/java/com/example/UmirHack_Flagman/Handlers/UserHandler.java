package com.example.UmirHack_Flagman.Handlers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.UmirHack_Flagman.models.LoginData;
import com.example.UmirHack_Flagman.models.LoginResponse;
import com.example.UmirHack_Flagman.models.RegisterData;
import com.example.UmirHack_Flagman.models.RegisterResponse;
import com.example.UmirHack_Flagman.models.TokenResponse;
import com.example.UmirHack_Flagman.models.UserResponse;

@RestController
@RequestMapping("/api/v1/auth/")
public class UserHandler {
    private static final String REFRESH_COOKIE = "refresh_token";

    @PostMapping("register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterData req) {
        // TODO регистрация пользователей
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @PostMapping("login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginData req) {
        // TODO авторизация пользователей
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @PostMapping("refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @PostMapping("logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        // TODO выход пользователя
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @GetMapping("me")
    public ResponseEntity<UserResponse> me() {
        // TODO получение инфы о юзере
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
