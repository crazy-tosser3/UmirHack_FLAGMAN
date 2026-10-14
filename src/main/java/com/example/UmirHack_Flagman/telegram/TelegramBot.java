package com.example.UmirHack_Flagman.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

// Long polling в отдельном потоке: общий планировщик (сроки, SSE) не блокируется на 25-секундных запросах
@Component
public class TelegramBot implements SmartLifecycle {
    private static final Logger log = LoggerFactory.getLogger(TelegramBot.class);
    private static final long RETRY_DELAY_MS = 5_000;

    private final TelegramProperties props;
    private final TelegramBotClient client;
    private final BotCommands commands;
    private volatile boolean running;
    private Thread thread;

    public TelegramBot(TelegramProperties props, TelegramBotClient client, BotCommands commands) {
        this.props = props;
        this.client = client;
        this.commands = commands;
    }

    @Override
    public void start() {
        running = true;
        if (!props.enabled() || !props.pollingEnabled()) {
            log.info("Telegram-бот выключен: не задан app.telegram.bot-token или отключен polling");
            return;
        }
        thread = Thread.ofPlatform().name("telegram-bot").daemon().start(this::loop);
    }

    @Override
    public void stop() {
        running = false;
        if (thread != null) {
            thread.interrupt();
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void loop() {
        try {
            client.setCommands(BotCommands.MENU);
        } catch (RuntimeException e) {
            log.warn("Не удалось зарегистрировать команды бота: {}", e.getMessage());
        }
        log.info("Telegram-бот запущен");

        long offset = 0;
        while (running) {
            try {
                for (JsonNode update : client.getUpdates(offset)) {
                    offset = update.get("update_id").asLong() + 1;
                    handle(update);
                }
            } catch (RuntimeException e) {
                if (!running) {
                    return;
                }
                log.warn("Ошибка Telegram-бота: {}", e.getMessage());
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    return;
                }
            }
        }
    }

    private void handle(JsonNode update) {
        JsonNode message = update.get("message");
        if (message == null || !message.has("text") || !message.has("from")) {
            return;
        }
        long chatId = message.get("chat").get("id").asLong();
        String reply = "private".equals(message.get("chat").path("type").asString())
                ? commands.handle(message.get("from").get("id").asLong(), message.get("text").asString())
                : "Бот работает только в личных сообщениях.";
        try {
            client.sendMessage(chatId, reply);
        } catch (RuntimeException e) {
            log.warn("Не удалось отправить ответ в чат {}: {}", chatId, e.getMessage());
        }
    }
}
