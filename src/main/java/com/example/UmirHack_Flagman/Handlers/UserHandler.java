package com.example.UmirHack_Flagman.Handlers;

import java.net.ResponseCache;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.UmirHack_Flagman.models.LoginData;
import com.example.UmirHack_Flagman.models.RegisterData;

@RestController 
@RequestMapping("/api/auth/")
public class UserHandler {
    @PostMapping("register")
    public ResponseEntity<RegisterData> register(@RequestBody RegisterData req) {
        // TODO регистрация пользователей
    }

    @PostMapping("login")
    public ResponseEntity<LoginData> login(@RequestBody LoginData req) {
        // TODO авторизация пользователей
    }

    @PostMapping("refresh")
    public refresh(){
        // TODO Обновление JWT пользователя
    }

    @PostMapping("logout")
    public logout(){
        // TODO выход пользователя
    }

    @PostMapping("me")
    public me(){
        // TODO получение инфы о юзере
    }
}
