package com.example.UmirHack_Flagman.Services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.example.UmirHack_Flagman.Config.AuthProperties;
import com.example.UmirHack_Flagman.Exceptions.ApiException;

@Service
public class RefreshTokenService {
    private static final String TOKEN_KEY = "auth:refresh:";
    private static final String FAMILY_KEY = "auth:family:";
    private static final String F_USER = "userId";
    private static final String F_FAMILY = "familyId";
    private static final String F_USED = "usedAt";

    private final StringRedisTemplate redis;
    private final AuthProperties props;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(StringRedisTemplate redis, AuthProperties props) {
        this.redis = redis;
        this.props = props;
    }

    public record Rotation(UUID userId, String refreshToken) {
    }

    public String issue(UUID userId) {
        String familyId = UUID.randomUUID().toString();
        redis.opsForValue().set(FAMILY_KEY + familyId, userId.toString(), props.refreshTtl());
        return store(userId.toString(), familyId);
    }

    public Rotation rotate(String token) {
        if (token == null || token.isBlank()) {
            throw invalid();
        }

        String key = TOKEN_KEY + hash(token);
        Map<Object, Object> data = redis.opsForHash().entries(key);
        if (data.isEmpty()) {
            throw invalid();
        }

        String userId = (String) data.get(F_USER);
        String familyId = (String) data.get(F_FAMILY);
        if (!Boolean.TRUE.equals(redis.hasKey(FAMILY_KEY + familyId))) {
            throw invalid();
        }

        boolean firstUse = redis.opsForHash().putIfAbsent(key, F_USED, Instant.now().toString());
        if (!firstUse) {
            redis.delete(FAMILY_KEY + familyId);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_REUSED",
                    "Refresh token уже был использован, сессия отозвана");
        }

        redis.expire(FAMILY_KEY + familyId, props.refreshTtl());
        return new Rotation(UUID.fromString(userId), store(userId, familyId));
    }

    public void revoke(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        Object familyId = redis.opsForHash().get(TOKEN_KEY + hash(token), F_FAMILY);
        if (familyId != null) {
            redis.delete(FAMILY_KEY + familyId);
        }
    }

    private String store(String userId, String familyId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        String key = TOKEN_KEY + hash(token);
        redis.opsForHash().putAll(key, Map.of(F_USER, userId, F_FAMILY, familyId));
        redis.expire(key, props.refreshTtl());
        return token;
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ApiException invalid() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN",
                "Refresh token отсутствует или недействителен");
    }
}
