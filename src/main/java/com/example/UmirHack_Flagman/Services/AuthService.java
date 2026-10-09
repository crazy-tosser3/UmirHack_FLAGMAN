package com.example.UmirHack_Flagman.Services;

import java.util.Locale;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.UmirHack_Flagman.Entity.UserEntity;
import com.example.UmirHack_Flagman.Exceptions.ApiException;
import com.example.UmirHack_Flagman.Repositories.UserRepoDB;
import com.example.UmirHack_Flagman.models.LoginData;
import com.example.UmirHack_Flagman.models.LoginResponse;
import com.example.UmirHack_Flagman.models.RegisterData;
import com.example.UmirHack_Flagman.models.TokenResponse;
import com.example.UmirHack_Flagman.models.UserResponse;
import com.example.UmirHack_Flagman.models.UserStatus;

@Service
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepoDB userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final String dummyHash;

    public AuthService(PasswordEncoder passwordEncoder, UserRepoDB userRepository,
            JwtService jwtService, RefreshTokenService refreshTokenService) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public record Session<T>(T body, String refreshToken) {
    }

    public UserResponse registerUser(RegisterData data) {
        String email = normalizeEmail(data.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw emailExists();
        }

        String hashPassword = passwordEncoder.encode(data.getPassword());
        UserEntity newUser = new UserEntity(data.getName().trim(), email, hashPassword);

        try {
            return toResponse(userRepository.saveAndFlush(newUser));
        } catch (DataIntegrityViolationException e) {
            throw emailExists();
        }
    }

    public Session<LoginResponse> loginUser(LoginData data) {
        UserEntity user = userRepository.findByEmail(normalizeEmail(data.getEmail())).orElse(null);

        String hash = user != null ? user.getPasswordHash() : dummyHash;
        boolean passwordOk = passwordEncoder.matches(data.getPassword(), hash);
        if (user == null || !passwordOk) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Неверный email или пароль");
        }
        ensureActive(user);

        LoginResponse.User userDto = new LoginResponse.User();
        userDto.setId(user.getId());
        userDto.setEmail(user.getEmail());
        userDto.setName(user.getName());
        userDto.setRole(user.getRole());

        LoginResponse response = new LoginResponse();
        fillToken(response, user);
        response.setUser(userDto);

        return new Session<>(response, refreshTokenService.issue(user.getId()));
    }

    public Session<TokenResponse> refresh(String refreshToken) {
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(refreshToken);

        UserEntity user = userRepository.findById(rotation.userId()).orElse(null);
        if (user == null || user.getStatus() == UserStatus.DISABLED) {
            refreshTokenService.revoke(rotation.refreshToken());
        }
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN",
                    "Refresh token отсутствует или недействителен");
        }
        ensureActive(user);

        TokenResponse response = new TokenResponse();
        fillToken(response, user);
        return new Session<>(response, rotation.refreshToken());
    }

    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    public UserResponse getMe(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                        "Требуется действительный access token"));
        ensureActive(user);
        return toResponse(user);
    }

    private void fillToken(TokenResponse response, UserEntity user) {
        response.setAccessToken(jwtService.generateAccessToken(user));
        response.setExpiresIn(jwtService.accessTtlSeconds());
    }

    private static void ensureActive(UserEntity user) {
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED", "Аккаунт отключен");
        }
    }

    private static UserResponse toResponse(UserEntity user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setName(user.getName());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static ApiException emailExists() {
        return new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "Аккаунт с таким email уже существует");
    }
}
