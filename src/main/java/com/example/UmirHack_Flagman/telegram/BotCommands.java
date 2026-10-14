package com.example.UmirHack_Flagman.telegram;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.UmirHack_Flagman.analytics.AnalyticsService;
import com.example.UmirHack_Flagman.analytics.dto.DailyDishesResponse;
import com.example.UmirHack_Flagman.analytics.dto.DailyIngredientsResponse;
import com.example.UmirHack_Flagman.analytics.dto.DeficitsResponse;
import com.example.UmirHack_Flagman.analytics.dto.StockForecastResponse;
import com.example.UmirHack_Flagman.analytics.dto.SupplySuggestionsResponse;
import com.example.UmirHack_Flagman.auth.AuthService;
import com.example.UmirHack_Flagman.auth.UserEntity;
import com.example.UmirHack_Flagman.auth.UserRepoDB;
import com.example.UmirHack_Flagman.auth.dto.UserStatus;
import com.example.UmirHack_Flagman.catalog.IngredientEntity;
import com.example.UmirHack_Flagman.catalog.IngredientRepoDB;
import com.example.UmirHack_Flagman.common.ApiException;
import com.example.UmirHack_Flagman.inventory.StockService;
import com.example.UmirHack_Flagman.inventory.dto.StockBatchesResponse;
import com.example.UmirHack_Flagman.inventory.dto.StockResponse;
import com.example.UmirHack_Flagman.menu.OffersService;
import com.example.UmirHack_Flagman.menu.StopListService;
import com.example.UmirHack_Flagman.menu.dto.OffersResponse;
import com.example.UmirHack_Flagman.menu.dto.StopListResponse;

// Команды бота. Отвечает только менеджерам и админам с привязанным на сайте Telegram
@Service
public class BotCommands {
    private static final Logger log = LoggerFactory.getLogger(BotCommands.class);
    private static final int LIST_LIMIT = 15;

    public static final Map<String, String> MENU = menu();

    private final UserRepoDB userRepo;
    private final IngredientRepoDB ingredientRepo;
    private final AnalyticsService analytics;
    private final StockService stockService;
    private final StopListService stopListService;
    private final OffersService offersService;
    private final DateTimeFormatter dateTime;
    private final DateTimeFormatter date;

    public BotCommands(UserRepoDB userRepo, IngredientRepoDB ingredientRepo, AnalyticsService analytics,
            StockService stockService, StopListService stopListService, OffersService offersService,
            @Value("${app.analytics.zone:Europe/Moscow}") ZoneId zone) {
        this.userRepo = userRepo;
        this.ingredientRepo = ingredientRepo;
        this.analytics = analytics;
        this.stockService = stockService;
        this.stopListService = stopListService;
        this.offersService = offersService;
        this.dateTime = DateTimeFormatter.ofPattern("dd.MM HH:mm").withZone(zone);
        this.date = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(zone);
    }

    private static Map<String, String> menu() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("today", "Продажи и расход продуктов за сегодня");
        m.put("expiring", "Партии, у которых скоро истекает срок");
        m.put("stoplist", "Блюда в стоп-листе и почему");
        m.put("offers", "Горячие предложения");
        m.put("forecast", "Когда блюда уйдут в стоп");
        m.put("supply", "Что докупить на 3 дня");
        m.put("deficits", "Чего не хватало для заказов за 30 дней");
        m.put("stock", "Остаток продукта: /stock моцарелла");
        m.put("help", "Список команд");
        return m;
    }

    public String handle(long telegramId, String text) {
        String trimmed = text == null ? "" : text.trim();
        String[] parts = trimmed.split("\\s+", 2);
        // /cmd@BotName → cmd
        String command = parts[0].toLowerCase(Locale.ROOT).replaceFirst("^/", "").replaceFirst("@.*$", "");
        String argument = parts.length > 1 ? parts[1].trim() : "";

        Optional<UserEntity> user = allowedUser(telegramId);
        if (command.equals("start")) {
            return user.map(u -> "Здравствуйте, " + esc(u.getName()) + "!\n\n" + help())
                    .orElseGet(() -> notLinked(telegramId));
        }
        if (user.isEmpty()) {
            return notLinked(telegramId);
        }
        try {
            return switch (command) {
                case "help" -> help();
                case "today" -> today();
                case "expiring" -> expiring();
                case "stoplist" -> stopList();
                case "offers" -> offers();
                case "forecast" -> forecast();
                case "supply" -> supply();
                case "deficits" -> deficits();
                case "stock" -> stock(argument);
                default -> "Неизвестная команда. " + help();
            };
        } catch (ApiException e) {
            return esc(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Ошибка команды бота /{}", command, e);
            return "Не удалось выполнить команду, попробуйте позже.";
        }
    }

    private Optional<UserEntity> allowedUser(long telegramId) {
        return userRepo.findByTelegramId(telegramId)
                .filter(u -> u.getStatus() == UserStatus.ACTIVE && AuthService.canUseTelegram(u.getRole()));
    }

    private static String notLinked(long telegramId) {
        return "Этот Telegram не привязан к аккаунту менеджера или администратора.\n\n"
                + "Войдите на сайт и привяжите Telegram в профиле. Ваш Telegram ID: <code>" + telegramId + "</code>";
    }

    private static String help() {
        return "<b>Команды</b>\n" + MENU.entrySet().stream()
                .map(e -> "/" + e.getKey() + " — " + esc(e.getValue()))
                .collect(Collectors.joining("\n"));
    }

    // Команды

    private String today() {
        DailyDishesResponse dishes = analytics.getDailyDishes(null);
        DailyIngredientsResponse ingredients = analytics.getDailyIngredients(null);

        StringBuilder sb = new StringBuilder("<b>Сегодня, " + date.format(dishes.getFrom()) + "</b>\n")
                .append("Заказов: ").append(dishes.getTotalOrders())
                .append(", порций: ").append(dishes.getTotalPortions()).append("\n");
        if (!dishes.getItems().isEmpty()) {
            sb.append("\n<b>Топ блюд</b>\n");
            int i = 1;
            for (DailyDishesResponse.Item item : dishes.getItems().subList(0, Math.min(5, dishes.getItems().size()))) {
                sb.append(i++).append(". ").append(esc(item.getDishName())).append(" — ").append(item.getPortions()).append("\n");
            }
        }
        if (!ingredients.getItems().isEmpty()) {
            sb.append("\n<b>Расход продуктов</b>\n");
            for (DailyIngredientsResponse.Item item : ingredients.getItems().subList(0, Math.min(8, ingredients.getItems().size()))) {
                sb.append("• ").append(esc(item.getIngredientName())).append(" — ")
                        .append(qty(item.getConsumedQuantity(), item.getUnit()));
                if (item.getWrittenOffQuantity().signum() > 0) {
                    sb.append(", списано ").append(qty(item.getWrittenOffQuantity(), item.getUnit()));
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private String expiring() {
        StockBatchesResponse expiring = stockService.getBatches(null, "expiring", null, 1, LIST_LIMIT);
        long expired = stockService.getBatches(null, "expired", null, 1, 1).getTotal();
        if (expiring.getItems().isEmpty()) {
            return "Партий с истекающим сроком нет." + (expired > 0 ? "\nПросроченных партий на складе: " + expired : "");
        }
        Map<UUID, String> units = units(expiring.getItems().stream().map(StockBatchesResponse.Item::getIngredientId).toList());
        StringBuilder sb = new StringBuilder("<b>Скоро истекает срок</b> (" + expiring.getTotal() + ")\n");
        for (StockBatchesResponse.Item b : expiring.getItems()) {
            sb.append("• ").append(esc(b.getIngredientName())).append(" — ")
                    .append(qty(b.getRemainingQuantity(), units.get(b.getIngredientId())))
                    .append(", до ").append(dateTime.format(b.getExpiresAt()))
                    .append(" (").append(left(b.getRemainingShelfLifeSeconds())).append(")\n");
        }
        more(sb, expiring.getTotal(), expiring.getItems().size());
        if (expired > 0) {
            sb.append("\nПросроченных партий на складе: ").append(expired);
        }
        return sb.toString();
    }

    private String stopList() {
        StopListResponse stop = stopListService.getStopList(null, 1, LIST_LIMIT);
        if (stop.getItems().isEmpty()) {
            return "Стоп-лист пуст — все блюда доступны.";
        }
        StringBuilder sb = new StringBuilder("<b>Стоп-лист</b> (" + stop.getTotal() + ")\n");
        for (StopListResponse.Item item : stop.getItems()) {
            sb.append("• ").append(esc(item.getDishName())).append(" — ").append(item.getReasons().stream()
                    .map(r -> switch (r.getCode()) {
                        case MANUAL -> "снято вручную";
                        case EXPIRED_INGREDIENT -> esc(r.getIngredientName()) + " просрочен";
                        case INSUFFICIENT_STOCK -> "мало " + esc(r.getIngredientName()) + " ("
                                + qty(r.getAvailableQuantity(), r.getUnit()) + " из " + qty(r.getRequiredQuantity(), r.getUnit()) + ")";
                    })
                    .collect(Collectors.joining("; "))).append("\n");
        }
        more(sb, stop.getTotal(), stop.getItems().size());
        return sb.toString();
    }

    private String offers() {
        OffersResponse offers = offersService.getOffers(5, null);
        if (offers.getItems().isEmpty()) {
            return "Горячих предложений нет — продуктов с близким сроком не найдено.";
        }
        StringBuilder sb = new StringBuilder("<b>Горячие предложения</b>\n");
        int i = 1;
        for (OffersResponse.Item item : offers.getItems()) {
            sb.append(i++).append(". <b>").append(esc(item.getDishName())).append("</b> — приоритет ")
                    .append(Math.round(item.getPriority())).append(", порций: ").append(item.getAvailablePortions()).append("\n");
            if (!item.getReasons().isEmpty()) {
                sb.append("   ").append(esc(item.getReasons().getFirst().getIngredientName())).append(": ")
                        .append(esc(item.getReasons().getFirst().getExplanation())).append("\n");
            }
        }
        return sb.toString();
    }

    private String forecast() {
        List<StockForecastResponse.Item> soon = analytics.getStockForecast(null, 1, 100).getItems().stream()
                .filter(i -> i.getStopAt() != null)
                .limit(10)
                .toList();
        if (soon.isEmpty()) {
            return "В ближайшие 30 дней блюда в стоп-лист не уйдут (при текущем расходе).";
        }
        StringBuilder sb = new StringBuilder("<b>Прогноз стоп-листа</b>\n");
        for (StockForecastResponse.Item item : soon) {
            sb.append("• ").append(esc(item.getDishName())).append(" — ");
            if ("already_stopped".equals(item.getCause())) {
                sb.append("уже в стопе");
            } else {
                sb.append("~").append(dateTime.format(item.getStopAt()))
                        .append("expiry".equals(item.getCause()) ? ", испортится " : ", закончится ")
                        .append(esc(item.getLimitingIngredient().getIngredientName()))
                        .append("; сейчас порций: ").append(item.getAvailablePortions());
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String supply() {
        SupplySuggestionsResponse supply = analytics.getSupplySuggestions(3);
        if (supply.getItems().isEmpty()) {
            return "Запасов хватит на ближайшие 3 дня.";
        }
        StringBuilder sb = new StringBuilder("<b>Что докупить на 3 дня</b>\n");
        for (SupplySuggestionsResponse.Item item : supply.getItems().subList(0, Math.min(20, supply.getItems().size()))) {
            sb.append("• ").append(esc(item.getIngredientName())).append(" — ")
                    .append(qty(item.getSuggestedQuantity(), item.getUnit()))
                    .append(" (есть ").append(qty(item.getAvailableQuantity(), item.getUnit()))
                    .append(", расход ").append(qty(item.getDailyConsumption(), item.getUnit())).append("/день)\n");
        }
        more(sb, supply.getItems().size(), Math.min(20, supply.getItems().size()));
        return sb.toString();
    }

    private String deficits() {
        DeficitsResponse deficits = analytics.getDeficits(null, null, null, 1, 10);
        if (deficits.getItems().isEmpty()) {
            return "За 30 дней отказов из-за нехватки продуктов не было.";
        }
        StringBuilder sb = new StringBuilder("<b>Нехватка продуктов за 30 дней</b>\n");
        for (DeficitsResponse.Item item : deficits.getItems()) {
            sb.append("• ").append(esc(item.getIngredientName())).append(" — ").append(item.getDeficitCount())
                    .append(" отказ(ов), не хватило ").append(qty(item.getMissingQuantity(), item.getUnit()))
                    .append(", последний ").append(dateTime.format(item.getLastDeficitAt())).append("\n");
        }
        return sb.toString();
    }

    private String stock(String search) {
        if (search.isBlank()) {
            return "Укажите продукт: <code>/stock моцарелла</code>";
        }
        StockResponse stock = stockService.getStock(null, search, 1, 10);
        if (stock.getItems().isEmpty()) {
            return "Продукт «" + esc(search) + "» не найден.";
        }
        StringBuilder sb = new StringBuilder("<b>Остатки</b>\n");
        for (StockResponse.Item item : stock.getItems()) {
            sb.append("• ").append(esc(item.getName())).append(" — доступно ").append(qty(item.getAvailableQuantity(), item.getUnit()));
            if (item.getExpiredQuantity().signum() > 0) {
                sb.append(", просрочено ").append(qty(item.getExpiredQuantity(), item.getUnit()));
            }
            if (item.getNearestExpiryAt() != null) {
                sb.append(", ближайший срок ").append(dateTime.format(item.getNearestExpiryAt()));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    // Форматирование

    private Map<UUID, String> units(List<UUID> ids) {
        return ingredientRepo.findAllById(ids).stream()
                .collect(Collectors.toMap(IngredientEntity::getId, IngredientEntity::getUnit, (a, b) -> a));
    }

    private static void more(StringBuilder sb, long total, int shown) {
        if (total > shown) {
            sb.append("…и ещё ").append(total - shown).append("\n");
        }
    }

    private static String qty(BigDecimal value, String unit) {
        String number = value == null ? "0" : value.stripTrailingZeros().toPlainString();
        return unit == null ? number : number + " " + unit;
    }

    private static String left(long seconds) {
        Duration d = Duration.ofSeconds(seconds);
        return d.toHours() >= 1 ? "через " + d.toHours() + " ч" : "через " + Math.max(1, d.toMinutes()) + " мин";
    }

    static String esc(String text) {
        return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
