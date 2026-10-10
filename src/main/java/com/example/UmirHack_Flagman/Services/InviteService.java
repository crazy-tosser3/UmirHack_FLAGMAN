package com.example.UmirHack_Flagman.Services;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class InviteService {
    private static final String INVITE_KEY = "auth:invite:";
    private static final String USER_KEY = "auth:user-invite:";

    private final StringRedisTemplate redis;
    private final String baseUrl;
    private final Duration ttl;
    private final SecureRandom random = new SecureRandom();

    public InviteService(StringRedisTemplate redis,
            @Value("${app.invite.base-url:http://localhost:5173/accept-invite}") String baseUrl,
            @Value("${app.invite.ttl:48h}") Duration ttl) {
        this.redis = redis;
        this.baseUrl = baseUrl;
        this.ttl = ttl;
    }

    public record Invite(String url, Instant expiresAt) {
    }

    public Invite create(UUID userId) {
        revoke(userId);

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String hash = Hashes.sha256(token);

        redis.opsForValue().set(INVITE_KEY + hash, userId.toString(), ttl);
        redis.opsForValue().set(USER_KEY + userId, hash, ttl);
        return new Invite(baseUrl + (baseUrl.contains("?") ? "&" : "?") + "token=" + token, Instant.now().plus(ttl));
    }

    public Optional<UUID> find(String token) {
        String userId = redis.opsForValue().get(INVITE_KEY + Hashes.sha256(token));
        return Optional.ofNullable(userId).map(UUID::fromString);
    }

    public Optional<Instant> expiresAt(String token) {
        Long seconds = redis.getExpire(INVITE_KEY + Hashes.sha256(token), TimeUnit.SECONDS);
        return seconds == null || seconds < 0 ? Optional.empty() : Optional.of(Instant.now().plusSeconds(seconds));
    }

    public Optional<UUID> consume(String token) {
        String userId = redis.opsForValue().getAndDelete(INVITE_KEY + Hashes.sha256(token));
        if (userId == null) {
            return Optional.empty();
        }
        redis.delete(USER_KEY + userId);
        return Optional.of(UUID.fromString(userId));
    }

    public void revoke(UUID userId) {
        String hash = redis.opsForValue().getAndDelete(USER_KEY + userId);
        if (hash != null) {
            redis.delete(INVITE_KEY + hash);
        }
    }
}
