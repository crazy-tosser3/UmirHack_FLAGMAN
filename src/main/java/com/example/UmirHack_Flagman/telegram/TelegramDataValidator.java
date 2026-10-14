package com.example.UmirHack_Flagman.telegram;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.example.UmirHack_Flagman.telegram.dto.TelegramAuthDto;

// Проверка подписи Telegram Login Widget: https://core.telegram.org/widgets/login#checking-authorization
public final class TelegramDataValidator {

    private TelegramDataValidator() {
    }

    public static boolean isValid(TelegramAuthDto dto, String botToken) {
        if (dto == null || dto.getId() == null || dto.getAuth_date() == null || dto.getHash() == null) {
            return false;
        }
        try {
            String expected = sign(dto, botToken);
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    dto.getHash().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }

    // Отдельно — чтобы тесты могли подписывать данные так же, как Telegram
    public static String sign(TelegramAuthDto dto, String botToken) throws Exception {
        // Только заполненные поля, по алфавиту, key=value через \n
        Map<String, String> data = new TreeMap<>();
        data.put("id", String.valueOf(dto.getId()));
        data.put("auth_date", String.valueOf(dto.getAuth_date()));
        if (dto.getFirst_name() != null) data.put("first_name", dto.getFirst_name());
        if (dto.getLast_name() != null) data.put("last_name", dto.getLast_name());
        if (dto.getUsername() != null) data.put("username", dto.getUsername());
        if (dto.getPhoto_url() != null) data.put("photo_url", dto.getPhoto_url());

        String dataCheckString = data.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("\n"));

        // Ключ HMAC — SHA-256 от токена бота
        byte[] secretKey = MessageDigest.getInstance("SHA-256").digest(botToken.getBytes(StandardCharsets.UTF_8));
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secretKey, "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8)));
    }
}
