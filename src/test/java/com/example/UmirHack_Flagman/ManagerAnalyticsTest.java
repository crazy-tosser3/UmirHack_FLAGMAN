package com.example.UmirHack_Flagman;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.UmirHack_Flagman.auth.dto.Role;
import com.example.UmirHack_Flagman.catalog.DishEntity;
import com.example.UmirHack_Flagman.catalog.IngredientEntity;
import com.example.UmirHack_Flagman.inventory.ExpiryMonitor;
import com.example.UmirHack_Flagman.support.IntegrationTest;

import tools.jackson.databind.JsonNode;

class ManagerAnalyticsTest extends IntegrationTest {
    private String manager;
    private String employee;
    private IngredientEntity cheese;
    private DishEntity pizza;

    @Autowired
    private ExpiryMonitor expiryMonitor;

    @BeforeEach
    void setUp() {
        manager = tokenFor(Role.MANAGER);
        employee = tokenFor(Role.EMPLOYEE);
        cheese = ingredient("Моцарелла", "g");
        pizza = dish("Маргарита", cheese, 120);
    }

    @Test
    void listsDeliveriesAndOrders() {
        stock(manager, cheese, 1000, Duration.ofDays(5));
        post("/orders", employee, orderOf("POS-1", pizza, 1));
        post("/orders", employee, orderOf("POS-2", pizza, 2));

        assertThat(get("/deliveries", manager).body().get("total").asInt()).isEqualTo(1);
        assertThat(get("/orders?status=confirmed", manager).body().get("total").asInt()).isEqualTo(2);
        assertThat(get("/orders?externalOrderId=POS-2", manager).body().get("total").asInt()).isEqualTo(1);
        assertThat(get("/orders?status=unknown", manager).status()).isEqualTo(400);
        assertThat(get("/orders?limit=500", manager).status()).isEqualTo(400);
    }

    @Test
    void deficitsAreCountedFromRejectedOrders() {
        stock(manager, cheese, 100, Duration.ofDays(5));
        post("/orders", employee, orderOf(null, pizza, 1));
        post("/orders", employee, orderOf(null, pizza, 2));

        JsonNode deficit = get("/analytics/deficits", manager).body().get("items").get(0);

        assertThat(deficit.get("deficitCount").asInt()).isEqualTo(2);
        assertThat(deficit.get("missingQuantity").decimalValue()).isEqualByComparingTo("160");
    }

    @Test
    void forecastWithoutConsumptionStopsAtExpiry() {
        stock(manager, cheese, 500, Duration.ofDays(2));
        Instant expiresAt = Instant.parse(get("/stock/batches", manager).body().get("items").get(0).get("expiresAt").asString());

        JsonNode item = get("/analytics/stock-forecast", manager).body().get("items").get(0);

        assertThat(item.get("availablePortions").asInt()).isEqualTo(4);
        assertThat(item.get("cause").asString()).isEqualTo("expiry");
        assertThat(Instant.parse(item.get("stopAt").asString())).isCloseTo(expiresAt, within(1, ChronoUnit.SECONDS));
    }

    @Test
    void supplySuggestionCoversStockThatWillExpire() {
        stock(manager, cheese, 500, Duration.ofDays(1));

        JsonNode item = get("/analytics/supply-suggestions?coverDays=3", manager).body().get("items").get(0);

        // Расхода нет, но партия сгорит раньше конца периода — нужен минимум на одну порцию
        assertThat(item.get("ingredientName").asString()).isEqualTo("Моцарелла");
        assertThat(item.get("suggestedQuantity").decimalValue()).isEqualByComparingTo("120");
        assertThat(get("/analytics/supply-suggestions?coverDays=0", manager).status()).isEqualTo(400);
    }

    @Test
    void analyticsIsForStaffOnly() {
        assertThat(get("/analytics/write-offs", employee).status()).isEqualTo(403);
        assertThat(get("/analytics/stock-forecast", employee).status()).isEqualTo(403);
        assertThat(get("/deliveries", employee).status()).isEqualTo(403);
    }

    @Test
    void dailyDishesAndIngredients() {
        DishEntity bread = dish("Сырный хлеб", cheese, 50);
        stock(manager, cheese, 1000, Duration.ofDays(5));
        expiredBatch(cheese, 200);
        post("/orders", employee, orderOf("POS-1", pizza, 2));
        post("/orders", employee, orderOf("POS-2", pizza, 1, bread, 3));
        // Отказ из-за нехватки в статистику не попадает
        assertThat(post("/orders", employee, orderOf("POS-3", pizza, 50)).status()).isEqualTo(409);
        expiryMonitor.run(Instant.now());

        String today = LocalDate.now(ZoneId.of("Europe/Moscow")).toString();

        JsonNode dishes = get("/analytics/daily/dishes?date=" + today, manager).body();
        assertThat(dishes.get("date").asString()).isEqualTo(today);
        assertThat(dishes.get("totalOrders").asInt()).isEqualTo(2);
        assertThat(dishes.get("totalPortions").asInt()).isEqualTo(6);
        JsonNode margherita = dishes.get("items").get(0);
        assertThat(margherita.get("dishName").asString()).isEqualTo("Маргарита");
        assertThat(margherita.get("portions").asInt()).isEqualTo(3);
        assertThat(margherita.get("orders").asInt()).isEqualTo(2);
        assertThat(dishes.get("items").get(1).get("portions").asInt()).isEqualTo(3);

        JsonNode usage = get("/analytics/daily/ingredients", manager).body().get("items").get(0);
        assertThat(usage.get("ingredientName").asString()).isEqualTo("Моцарелла");
        assertThat(usage.get("consumedQuantity").decimalValue()).isEqualByComparingTo("510");
        assertThat(usage.get("writtenOffQuantity").decimalValue()).isEqualByComparingTo("200");
        assertThat(usage.get("totalQuantity").decimalValue()).isEqualByComparingTo("710");

        String yesterday = LocalDate.parse(today).minusDays(1).toString();
        assertThat(get("/analytics/daily/dishes?date=" + yesterday, manager).body().get("items").size()).isZero();
        assertThat(get("/analytics/daily/ingredients?date=" + yesterday, manager).body().get("items").size()).isZero();

        assertThat(get("/analytics/daily/dishes", employee).status()).isEqualTo(403);
        assertThat(get("/analytics/daily/ingredients?date=11.10.2026", manager).status()).isEqualTo(400);
    }

    @Test
    void burstOfOrdersIsNotExtrapolatedToWholeDay() {
        stock(manager, cheese, 10000, Duration.ofDays(5));
        for (int i = 0; i < 10; i++) {
            post("/orders", employee, orderOf(null, pizza, 1));
        }

        // 10 порций по 120 г за минуту — это 1200 г за день, а не 1200 × 24 × 60
        JsonNode limiting = get("/analytics/stock-forecast", manager).body().get("items").get(0).get("limitingIngredient");
        assertThat(limiting.get("dailyConsumption").decimalValue()).isEqualByComparingTo("1200");
        // Остатка 8800 г хватает на 3 дня по 1200 г — докупать нечего
        assertThat(get("/analytics/supply-suggestions?coverDays=3", manager).body().get("items").size()).isZero();
    }
}
