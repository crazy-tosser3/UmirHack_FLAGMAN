package com.example.UmirHack_Flagman.auth;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import com.example.UmirHack_Flagman.common.Hashes;

@Service
public class InviteService {

    private static final String INVITE_KEY = "auth:invite:";

    private static final String CONSUME_SCRIPT = """
        local role = redis.call('HGET', KEYS[1], 'role')
        if not role then
            return nil
        end

        local expiresAt = tonumber(
            redis.call('HGET', KEYS[1], 'expiresAt')
        )

        if not expiresAt or expiresAt <= tonumber(ARGV[1]) then
            redis.call('DEL', KEYS[1])
            return nil
        end

        local clicks = tonumber(
            redis.call('HGET', KEYS[1], 'clicks')
        )

        if not clicks then
            return nil
        end

        if clicks > 0 then
            if clicks == 1 then
                redis.call('DEL', KEYS[1])
            else
                redis.call('HINCRBY', KEYS[1], 'clicks', -1)
            end
        end

        return role
        """;

    private static final DefaultRedisScript<String> CONSUME_REDIS_SCRIPT =
            new DefaultRedisScript<>(CONSUME_SCRIPT, String.class);

    private final StringRedisTemplate redis;
    private final String baseUrl;

    public InviteService(
            StringRedisTemplate redis,
            @Value("${app.invite.base-url:http://localhost:5173/register?code={code}}")
            String baseUrl) {
        this.redis = redis;
        this.baseUrl = baseUrl;
    }

    public record Invite(String url, Instant expiresAt) {
    }

    public Invite create(
            String code,
            String role,
            String expireAt,
            int clicks
    ) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException(
                    "Invite code cannot be empty"
            );
        }

        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException(
                    "Role cannot be empty"
            );
        }

        if (clicks < 0) {
            throw new IllegalArgumentException(
                    "Clicks cannot be negative"
            );
        }

        if (expireAt == null || expireAt.isBlank()) {
            throw new IllegalArgumentException(
                    "expireAt cannot be empty"
            );
        }

        Instant expiration = Instant.parse(expireAt);
        Instant now = Instant.now();

        if (!expiration.isAfter(now)) {
            throw new IllegalArgumentException(
                    "expireAt must be in the future"
            );
        }

        String key = INVITE_KEY + Hashes.sha256(code);

        Map<String, String> data = Map.of(
                "role", role,
                "expiresAt", String.valueOf(
                        expiration.toEpochMilli()
                ),
                "clicks", String.valueOf(clicks)
        );

        redis.opsForHash().putAll(key, data);

        redis.expire(
                key,
                Duration.between(now, expiration)
        );

        String encodedCode = URLEncoder.encode(
                code,
                StandardCharsets.UTF_8
        );

        String url;

        if (baseUrl.contains("{code}")) {
            url = baseUrl.replace("{code}", encodedCode);
        } else {
            url = baseUrl
                    + (baseUrl.contains("?") ? "&" : "?")
                    + "code=" + encodedCode;
        }

        return new Invite(url, expiration);
    }

    public Optional<String> find(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }

        String key = INVITE_KEY + Hashes.sha256(code);

        Object role = redis.opsForHash().get(key, "role");
        Object expiresAt = redis.opsForHash().get(key, "expiresAt");

        if (role == null || expiresAt == null) {
            return Optional.empty();
        }

        try {
            Instant expiration = Instant.ofEpochMilli(
                    Long.parseLong(expiresAt.toString())
            );

            if (!expiration.isAfter(Instant.now())) {
                redis.delete(key);
                return Optional.empty();
            }

            return Optional.of(role.toString());
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    public Optional<Instant> expiresAt(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }

        String key = INVITE_KEY + Hashes.sha256(code);

        Object value = redis.opsForHash().get(key, "expiresAt");

        if (value == null) {
            return Optional.empty();
        }

        try {
            Instant expiration = Instant.ofEpochMilli(
                    Long.parseLong(value.toString())
            );

            if (!expiration.isAfter(Instant.now())) {
                redis.delete(key);
                return Optional.empty();
            }

            return Optional.of(expiration);
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    public Optional<String> consume(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }

        String key = INVITE_KEY + Hashes.sha256(code);

        String role = redis.execute(
                CONSUME_REDIS_SCRIPT,
                List.of(key),
                String.valueOf(Instant.now().toEpochMilli())
        );

        return Optional.ofNullable(role);
    }

    public void revoke(String code) {
        if (code == null || code.isBlank()) {
            return;
        }

        redis.delete(INVITE_KEY + Hashes.sha256(code));
    }

    public Optional<Integer> remainingClicks(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }

        String key = INVITE_KEY + Hashes.sha256(code);

        Object clicks = redis.opsForHash().get(key, "clicks");

        if (clicks == null || find(code).isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(
                    Integer.parseInt(clicks.toString())
            );
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}