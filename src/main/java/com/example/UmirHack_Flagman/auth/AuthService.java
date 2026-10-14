package com.example.UmirHack_Flagman.auth;

import java.util.Locale;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.UmirHack_Flagman.auth.dto.InviteInfo;
import com.example.UmirHack_Flagman.auth.dto.LoginData;
import com.example.UmirHack_Flagman.auth.dto.LoginResponse;
import com.example.UmirHack_Flagman.auth.dto.RegisterData;
import com.example.UmirHack_Flagman.auth.dto.Role;
import com.example.UmirHack_Flagman.auth.dto.TokenResponse;
import com.example.UmirHack_Flagman.auth.dto.UserResponse;
import com.example.UmirHack_Flagman.auth.dto.UserStatus;
import com.example.UmirHack_Flagman.common.ApiException;

@Service
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepoDB userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final InviteService inviteService;
    private final String dummyHash;

    public AuthService(PasswordEncoder passwordEncoder, UserRepoDB userRepository,
            JwtService jwtService, RefreshTokenService refreshTokenService, InviteService inviteService) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.inviteService = inviteService;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public record Session<T>(T body, String refreshToken) {
    }

    public UserResponse registerUser(RegisterData data) {
        String email = normalizeEmail(data.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw emailExists();
        }

        // Код расходуется только после проверки email, чтобы неудачная регистрация не съедала использование ссылки
        Role role = inviteService.consume(data.getCode().trim()).map(AuthService::parseRole)
                .orElseThrow(AuthService::inviteNotFound);

        String hashPassword = passwordEncoder.encode(data.getPassword());
        UserEntity newUser = new UserEntity(data.getName().trim(), email, hashPassword);
        newUser.setRole(role);

        try {
            return toResponse(userRepository.saveAndFlush(newUser));
        } catch (DataIntegrityViolationException e) {
            throw emailExists();
        }
    }

    public Session<LoginResponse> loginUser(LoginData data) {
        UserEntity user = userRepository.findByEmail(normalizeEmail(data.getEmail())).orElse(null);

        boolean hasPassword = user != null && user.getPasswordHash() != null;
        boolean passwordOk = passwordEncoder.matches(data.getPassword(), hasPassword ? user.getPasswordHash() : dummyHash);
        if (!hasPassword || !passwordOk) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Неверный email или пароль");
        }
        ensureActive(user);
        return startSession(user);
    }

    public InviteInfo inviteInfo(String code) {
        String trimmed = code.trim();
        Role role = inviteService.find(trimmed).map(AuthService::parseRole).orElseThrow(AuthService::inviteNotFound);

        InviteInfo info = new InviteInfo();
        info.setRole(role);
        info.setExpiresAt(inviteService.expiresAt(trimmed).orElse(null));
        info.setRemainingClicks(inviteService.remainingClicks(trimmed).filter(c -> c > 0).orElse(null));
        return info;
    }

    private static Role parseRole(String role) {
        try {
            return Role.valueOf(role.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw inviteNotFound();
        }
    }

    private static ApiException inviteNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "INVITE_NOT_FOUND",
                "Приглашение недействительно, уже использовано или истекло");
    }

    private Session<LoginResponse> startSession(UserEntity user) {
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

    // Telegram: вход через виджет и привязка аккаунта. Подпись данных виджета проверяется до вызова

    public static boolean canUseTelegram(Role role) {
        return role == Role.MANAGER || role == Role.ADMIN;
    }

    public Session<LoginResponse> loginTelegram(long telegramId) {
        UserEntity user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "TELEGRAM_NOT_LINKED",
                        "Этот Telegram не привязан к аккаунту. Войдите по паролю и привяжите Telegram в профиле"));
        ensureActive(user);
        ensureTelegramAllowed(user);
        return startSession(user);
    }

    @Transactional
    public UserResponse linkTelegram(UUID userId, long telegramId, String telegramUsername) {
        UserEntity user = activeUser(userId);
        ensureTelegramAllowed(user);
        userRepository.findByTelegramId(telegramId)
                .filter(other -> !other.getId().equals(userId))
                .ifPresent(other -> {
                    throw telegramTaken();
                });
        user.setTelegramId(telegramId);
        user.setTelegramUsername(telegramUsername);
        try {
            return toResponse(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException e) {
            throw telegramTaken();
        }
    }

    @Transactional
    public UserResponse unlinkTelegram(UUID userId) {
        UserEntity user = activeUser(userId);
        user.setTelegramId(null);
        user.setTelegramUsername(null);
        return toResponse(userRepository.saveAndFlush(user));
    }

    private UserEntity activeUser(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                        "Требуется действительный access token"));
        ensureActive(user);
        return user;
    }

    private static void ensureTelegramAllowed(UserEntity user) {
        if (!canUseTelegram(user.getRole())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN",
                    "Вход через Telegram доступен только менеджерам и администраторам");
        }
    }

    private static ApiException telegramTaken() {
        return new ApiException(HttpStatus.CONFLICT, "TELEGRAM_ALREADY_LINKED",
                "Этот Telegram уже привязан к другому аккаунту");
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
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_NOT_ACTIVATED", "Аккаунт еще не активирован по приглашению");
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
        response.setUpdatedAt(user.getUpdatedAt());
        response.setTelegramLinked(user.getTelegramId() != null);
        response.setTelegramUsername(user.getTelegramUsername());
        return response;
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static ApiException emailExists() {
        return new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "Аккаунт с таким email уже существует");
    }
}
