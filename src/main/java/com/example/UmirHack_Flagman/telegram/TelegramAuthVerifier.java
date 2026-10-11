package com.example.UmirHack_Flagman.telegram;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.example.UmirHack_Flagman.common.ApiException;
import com.example.UmirHack_Flagman.telegram.dto.TelegramAuthDto;

@Service
public class TelegramAuthVerifier {
    private static final Logger log = LoggerFactory.getLogger(TelegramAuthVerifier.class);
    private static final Duration CLOCK_SKEW = Duration.ofMinutes(1);

    private final TelegramProperties props;

    public TelegramAuthVerifier(TelegramProperties props) {
        this.props = props;
    }

    public void verify(TelegramAuthDto dto) {
        if (!props.enabled()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "TELEGRAM_DISABLED", "Вход через Telegram не настроен");
        }
        if (!TelegramDataValidator.isValid(dto, props.botToken())) {
            log.warn("Неверная подпись данных Telegram для id {}", dto.getId());
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_TELEGRAM_AUTH", "Подпись данных Telegram не совпадает");
        }
        // Защита от повторного использования старых данных виджета
        Instant authDate = Instant.ofEpochSecond(dto.getAuth_date());
        Instant now = Instant.now();
        if (authDate.isBefore(now.minus(props.authMaxAge())) || authDate.isAfter(now.plus(CLOCK_SKEW))) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "TELEGRAM_AUTH_EXPIRED", "Данные авторизации Telegram устарели, войдите заново");
        }
    }
}
