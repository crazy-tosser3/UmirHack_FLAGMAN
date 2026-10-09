package com.example.UmirHack_Flagman.Config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
        String jwtIssuer,
        String jwtAudience,
        String jwtSigningKey,
        Duration accessTtl,
        Duration refreshTtl,
        boolean cookieSecure,
        String cookieSameSite) {
}
