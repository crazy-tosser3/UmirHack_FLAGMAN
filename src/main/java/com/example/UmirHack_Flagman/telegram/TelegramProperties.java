package com.example.UmirHack_Flagman.telegram;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

// Пустой botToken — вход через Telegram и бот выключены
@ConfigurationProperties(prefix = "app.telegram")
public record TelegramProperties(
        @DefaultValue("") String botToken,
        @DefaultValue("") String botUsername,
        @DefaultValue("true") boolean pollingEnabled,
        @DefaultValue("1h") Duration authMaxAge) {

    public boolean enabled() {
        return botToken != null && !botToken.isBlank();
    }
}
