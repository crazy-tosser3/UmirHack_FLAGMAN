package com.example.UmirHack_Flagman.auth;

import java.time.Duration;
import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.UmirHack_Flagman.auth.dto.InviteInfo;
import com.example.UmirHack_Flagman.auth.dto.InviteToken;
import com.example.UmirHack_Flagman.auth.dto.LoginData;
import com.example.UmirHack_Flagman.auth.dto.LoginResponse;
import com.example.UmirHack_Flagman.auth.dto.RegisterData;
import com.example.UmirHack_Flagman.auth.dto.RegisterResponse;
import com.example.UmirHack_Flagman.auth.dto.TokenResponse;
import com.example.UmirHack_Flagman.auth.dto.UserResponse;
import com.example.UmirHack_Flagman.common.ApiException;
import com.example.UmirHack_Flagman.common.dto.ErrorResponse;
import com.example.UmirHack_Flagman.telegram.TelegramAuthVerifier;
import com.example.UmirHack_Flagman.telegram.dto.TelegramAuthDto;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth/")
public class UserHandler {
    private static final String REFRESH_COOKIE = "refresh_token";
    private static final String REFRESH_COOKIE_PATH = "/api/v1/auth";

    private final AuthService authService;
    private final AuthProperties authProperties;
    private final TelegramAuthVerifier telegramVerifier;

    public UserHandler(AuthService authService, AuthProperties authProperties, TelegramAuthVerifier telegramVerifier) {
        this.authService = authService;
        this.authProperties = authProperties;
        this.telegramVerifier = telegramVerifier;
    }

    @PostMapping("register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterData req) {
        RegisterResponse response = new RegisterResponse();
        response.setUser(authService.registerUser(req));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginData req) {
        AuthService.Session<LoginResponse> session = authService.loginUser(req);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(session.refreshToken()).toString())
                .cacheControl(CacheControl.noStore())
                .body(session.body());
    }

    @PostMapping("invite/verify")
    public ResponseEntity<InviteInfo> verifyInvite(@Valid @RequestBody InviteToken req) {
        return ResponseEntity.ok(authService.inviteInfo(req.getCode()));
    }

    // Telegram Login Widget: вход для менеджеров и админов с привязанным Telegram
    @PostMapping("telegram")
    public ResponseEntity<LoginResponse> loginTelegram(@Valid @RequestBody TelegramAuthDto req) {
        telegramVerifier.verify(req);
        AuthService.Session<LoginResponse> session = authService.loginTelegram(req.getId());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(session.refreshToken()).toString())
                .cacheControl(CacheControl.noStore())
                .body(session.body());
    }

    @PostMapping("telegram/link")
    public ResponseEntity<UserResponse> linkTelegram(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) UUID userId,
            @Valid @RequestBody TelegramAuthDto req) {
        telegramVerifier.verify(req);
        return ResponseEntity.ok(authService.linkTelegram(userId, req.getId(), req.getUsername()));
    }

    @DeleteMapping("telegram/link")
    public ResponseEntity<UserResponse> unlinkTelegram(@RequestAttribute(AuthInterceptor.USER_ID_ATTR) UUID userId) {
        return ResponseEntity.ok(authService.unlinkTelegram(userId));
    }

    @PostMapping("refresh")
    public ResponseEntity<?> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        try {
            AuthService.Session<TokenResponse> session = authService.refresh(refreshToken);
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshCookie(session.refreshToken()).toString())
                    .cacheControl(CacheControl.noStore())
                    .body(session.body());
        } catch (ApiException e) {
            return ResponseEntity.status(e.getStatus())
                    .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                    .body(ErrorResponse.of(e.getCode(), e.getMessage(), e.getDetails()));
        }
    }

    @PostMapping("logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                .build();
    }

    @GetMapping("me")
    public ResponseEntity<UserResponse> me(@RequestAttribute(AuthInterceptor.USER_ID_ATTR) UUID userId) {
        return ResponseEntity.ok(authService.getMe(userId));
    }

    private ResponseCookie refreshCookie(String value) {
        return buildCookie(value, authProperties.refreshTtl());
    }

    private ResponseCookie clearRefreshCookie() {
        return buildCookie("", Duration.ZERO);
    }

    private ResponseCookie buildCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(authProperties.cookieSecure())
                .sameSite(authProperties.cookieSameSite())
                .path(REFRESH_COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }
}
