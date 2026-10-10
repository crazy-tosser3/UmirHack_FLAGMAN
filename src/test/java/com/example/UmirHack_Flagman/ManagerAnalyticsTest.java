package com.example.UmirHack_Flagman;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.UmirHack_Flagman.Entity.DishEntity;
import com.example.UmirHack_Flagman.Entity.IngredientEntity;
import com.example.UmirHack_Flagman.models.Role;
import com.example.UmirHack_Flagman.support.IntegrationTest;

import tools.jackson.databind.JsonNode;

class ManagerAnalyticsTest extends IntegrationTest {
    private String manager;
    private String employee;
    private IngredientEntity cheese;
    private DishEntity pizza;

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
}
