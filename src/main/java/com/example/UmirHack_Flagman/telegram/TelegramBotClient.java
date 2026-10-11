package com.example.UmirHack_Flagman.telegram;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

// Минимальный клиент Telegram Bot API: https://core.telegram.org/bots/api
@Component
public class TelegramBotClient {
    private static final int MAX_MESSAGE_LENGTH = 4000;
    static final int POLL_TIMEOUT_SECONDS = 25;

    private final TelegramProperties props;
    private final JsonMapper json;
    private RestClient rest;

    public TelegramBotClient(TelegramProperties props, JsonMapper json) {
        this.props = props;
        this.json = json;
    }

    private RestClient rest() {
        if (rest == null) {
            JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                    HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
            factory.setReadTimeout(Duration.ofSeconds(POLL_TIMEOUT_SECONDS + 15));
            rest = RestClient.builder()
                    .baseUrl("https://api.telegram.org/bot" + props.botToken())
                    .requestFactory(factory)
                    .build();
        }
        return rest;
    }

    public JsonNode getUpdates(long offset) {
        return call("getUpdates", Map.of("offset", offset, "timeout", POLL_TIMEOUT_SECONDS,
                "allowed_updates", List.of("message")));
    }

    public void sendMessage(long chatId, String html) {
        String text = html.length() > MAX_MESSAGE_LENGTH ? html.substring(0, MAX_MESSAGE_LENGTH) + "\n…" : html;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("chat_id", chatId);
        body.put("text", text);
        body.put("parse_mode", "HTML");
        body.put("link_preview_options", Map.of("is_disabled", true));
        call("sendMessage", body);
    }

    // Меню команд в клиенте Telegram
    public void setCommands(Map<String, String> commands) {
        List<Map<String, String>> list = commands.entrySet().stream()
                .map(e -> Map.of("command", e.getKey(), "description", e.getValue()))
                .toList();
        call("setMyCommands", Map.of("commands", list));
    }

    private JsonNode call(String method, Map<String, ?> body) {
        String response = rest().post()
                .uri("/" + method)
                .contentType(MediaType.APPLICATION_JSON)
                .body(json.writeValueAsString(body))
                .retrieve()
                .body(String.class);
        JsonNode node = json.readTree(response);
        if (!node.path("ok").asBoolean()) {
            throw new IllegalStateException("Telegram " + method + ": " + node.path("description").asString());
        }
        return node.get("result");
    }
}
