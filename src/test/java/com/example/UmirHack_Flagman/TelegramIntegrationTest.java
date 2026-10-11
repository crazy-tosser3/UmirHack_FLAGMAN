package com.example.UmirHack_Flagman;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.UmirHack_Flagman.auth.UserEntity;
import com.example.UmirHack_Flagman.auth.dto.Role;
import com.example.UmirHack_Flagman.auth.dto.UserStatus;
import com.example.UmirHack_Flagman.catalog.IngredientEntity;
import com.example.UmirHack_Flagman.support.IntegrationTest;
import com.example.UmirHack_Flagman.telegram.BotCommands;
import com.example.UmirHack_Flagman.telegram.TelegramDataValidator;
import com.example.UmirHack_Flagman.telegram.dto.TelegramAuthDto;

class TelegramIntegrationTest extends IntegrationTest {
    private static final long TG_ID = 777000111L;

    @Autowired
    private BotCommands bot;

    @Test
    void managerLinksTelegramAndLogsInWithIt() {
        existingUser("manager@test.local", Role.MANAGER);
        String token = login("manager@test.local", PASSWORD).body().get("accessToken").asString();

        Response linked = post("/auth/telegram/link", token, widget(TG_ID, Instant.now()));
        assertThat(linked.status()).isEqualTo(200);
        assertThat(linked.body().get("telegramLinked").asBoolean()).isTrue();
        assertThat(linked.body().get("telegramUsername").asString()).isEqualTo("ivan_tg");
        assertThat(get("/auth/me", token).body().get("telegramLinked").asBoolean()).isTrue();

        Response login = post("/auth/telegram", null, widget(TG_ID, Instant.now()));
        assertThat(login.status()).isEqualTo(200);
        assertThat(login.body().get("user").get("role").asString()).isEqualTo("manager");
        assertThat(login.cookie("refresh_token")).contains("HttpOnly");
        assertThat(get("/stock", login.body().get("accessToken").asString()).status()).isEqualTo(200);
    }

    @Test
    void telegramLoginChecksSignatureAgeAndLink() {
        assertThat(post("/auth/telegram", null, widget(TG_ID, Instant.now())).code()).isEqualTo("TELEGRAM_NOT_LINKED");

        Map<String, Object> tampered = widget(TG_ID, Instant.now());
        tampered.put("id", TG_ID + 1);
        assertThat(post("/auth/telegram", null, tampered).code()).isEqualTo("INVALID_TELEGRAM_AUTH");

        Response expired = post("/auth/telegram", null, widget(TG_ID, Instant.now().minus(Duration.ofHours(2))));
        assertThat(expired.status()).isEqualTo(401);
        assertThat(expired.code()).isEqualTo("TELEGRAM_AUTH_EXPIRED");

        Map<String, Object> noHash = widget(TG_ID, Instant.now());
        noHash.remove("hash");
        assertThat(post("/auth/telegram", null, noHash).status()).isEqualTo(400);
    }

    @Test
    void onlyManagersAndAdminsCanUseTelegram() {
        existingUser("cook@test.local", Role.EMPLOYEE);
        String employee = login("cook@test.local", PASSWORD).body().get("accessToken").asString();
        assertThat(post("/auth/telegram/link", employee, widget(TG_ID, Instant.now())).status()).isEqualTo(403);

        // Менеджера, привязавшего Telegram, понизили — вход через Telegram закрывается
        UserEntity manager = existingUser("manager@test.local", Role.MANAGER);
        String token = login("manager@test.local", PASSWORD).body().get("accessToken").asString();
        post("/auth/telegram/link", token, widget(TG_ID, Instant.now()));
        manager = userRepo.findById(manager.getId()).orElseThrow();
        manager.setRole(Role.EMPLOYEE);
        userRepo.save(manager);

        assertThat(post("/auth/telegram", null, widget(TG_ID, Instant.now())).status()).isEqualTo(403);
        assertThat(bot.handle(TG_ID, "/today")).contains("не привязан");
    }

    @Test
    void telegramAccountCanBeLinkedToOneUserOnly() {
        existingUser("first@test.local", Role.MANAGER);
        existingUser("second@test.local", Role.ADMIN);
        String first = login("first@test.local", PASSWORD).body().get("accessToken").asString();
        String second = login("second@test.local", PASSWORD).body().get("accessToken").asString();

        post("/auth/telegram/link", first, widget(TG_ID, Instant.now()));
        assertThat(post("/auth/telegram/link", second, widget(TG_ID, Instant.now())).code()).isEqualTo("TELEGRAM_ALREADY_LINKED");

        Response unlinked = send("DELETE", "/auth/telegram/link", first, null);
        assertThat(unlinked.body().get("telegramLinked").asBoolean()).isFalse();
        assertThat(post("/auth/telegram/link", second, widget(TG_ID, Instant.now())).status()).isEqualTo(200);
        assertThat(post("/auth/telegram", null, widget(TG_ID, Instant.now())).body().get("user").get("email").asString())
                .isEqualTo("second@test.local");
    }

    @Test
    void botAnswersOnlyLinkedManagers() {
        assertThat(bot.handle(TG_ID, "/start")).contains("не привязан").contains(String.valueOf(TG_ID));
        assertThat(bot.handle(TG_ID, "/today")).contains("не привязан");

        UserEntity manager = existingUser("manager@test.local", Role.MANAGER);
        manager.setTelegramId(TG_ID);
        userRepo.save(manager);

        String managerToken = login("manager@test.local", PASSWORD).body().get("accessToken").asString();
        IngredientEntity cheese = ingredient("Моцарелла", "g");
        dish("Маргарита", cheese, 120);
        stock(managerToken, cheese, 1000, Duration.ofHours(10));
        post("/orders", managerToken, orderOf("POS-1", dishRepo.findAll().getFirst(), 2));

        assertThat(bot.handle(TG_ID, "/start")).contains("Здравствуйте").contains("/today");
        assertThat(bot.handle(TG_ID, "/help")).contains("/stoplist").contains("/supply");
        assertThat(bot.handle(TG_ID, "/today")).contains("Заказов: 1").contains("Маргарита — 2").contains("Моцарелла — 240 g");
        assertThat(bot.handle(TG_ID, "/expiring")).contains("Моцарелла — 760 g");
        assertThat(bot.handle(TG_ID, "/stock моцар")).contains("доступно 760 g");
        assertThat(bot.handle(TG_ID, "/stock")).contains("Укажите продукт");
        assertThat(bot.handle(TG_ID, "/stoplist@FlagmanBot")).contains("Стоп-лист пуст");
        assertThat(bot.handle(TG_ID, "/offers")).contains("Маргарита");
        assertThat(bot.handle(TG_ID, "/supply")).isNotBlank();
        assertThat(bot.handle(TG_ID, "/forecast")).isNotBlank();
        assertThat(bot.handle(TG_ID, "/deficits")).contains("не было");
        assertThat(bot.handle(TG_ID, "/whatever")).contains("Неизвестная команда");

        manager.setStatus(UserStatus.DISABLED);
        userRepo.save(manager);
        assertThat(bot.handle(TG_ID, "/today")).contains("не привязан");
    }

    // Данные виджета, подписанные так же, как это делает Telegram
    private static Map<String, Object> widget(long id, Instant authDate) {
        TelegramAuthDto dto = new TelegramAuthDto();
        dto.setId(id);
        dto.setFirst_name("Иван");
        dto.setUsername("ivan_tg");
        dto.setAuth_date(authDate.getEpochSecond());
        try {
            dto.setHash(TelegramDataValidator.sign(dto, TELEGRAM_BOT_TOKEN));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", dto.getId());
        body.put("first_name", dto.getFirst_name());
        body.put("username", dto.getUsername());
        body.put("auth_date", dto.getAuth_date());
        body.put("hash", dto.getHash());
        return body;
    }
}
