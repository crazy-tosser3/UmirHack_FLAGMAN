package com.example.UmirHack_Flagman.Services;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.example.UmirHack_Flagman.Config.AuthProperties;
import com.example.UmirHack_Flagman.Entity.UserEntity;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
    private static final String ALGORITHM = "HS256";

    private final AuthProperties props;
    private final SecretKey key;

    public JwtService(AuthProperties props) {
        this.props = props;
        // Бросит WeakKeyException, если ключ короче 256 бит
        this.key = Keys.hmacShaKeyFor(props.jwtSigningKey().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UserEntity user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(props.jwtIssuer())
                .audience().add(props.jwtAudience()).and()
                .subject(user.getId().toString())
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(props.accessTtl())))
                .claim("role", user.getRole().name().toLowerCase())
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    // Проверяет подпись, exp, iss, aud и алгоритм; при ошибке бросает JwtException
    public Claims parse(String token) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(props.jwtIssuer())
                .requireAudience(props.jwtAudience())
                .build()
                .parseSignedClaims(token);

        if (!ALGORITHM.equals(jws.getHeader().getAlgorithm())) {
            throw new UnsupportedJwtException("Unexpected JWT algorithm");
        }
        return jws.getPayload();
    }

    public long accessTtlSeconds() {
        return props.accessTtl().toSeconds();
    }
}
