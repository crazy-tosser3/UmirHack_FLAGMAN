package com.example.UmirHack_Flagman;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.UmirHack_Flagman.auth.dto.Role;
import com.example.UmirHack_Flagman.catalog.DishEntity;
import com.example.UmirHack_Flagman.catalog.IngredientEntity;
import com.example.UmirHack_Flagman.support.IntegrationTest;

import tools.jackson.databind.JsonNode;

class StopListAndExclusionsTest extends IntegrationTest {
    private String manager;
    private String employee;
    private IngredientEntity cheese;
    private IngredientEntity sauce;
    private IngredientEntity basil;
    private DishEntity pizza;

    @BeforeEach
    void setUp() {
        manager = tokenFor(Role.MANAGER);
        employee = tokenFor(Role.EMPLOYEE);
        cheese = ingredient("Моцарелла", "g");
        sauce = ingredient("Соус", "g");
        basil = ingredient("Базилик", "g");
        pizza = dish("Маргарита", cheese, 120, sauce, 80, basil, 5);
    }

    @Test
    void manualStopBlocksOrdersUntilResumed() {
        stock(manager, cheese, 1000, Duration.ofDays(5));
        stock(manager, sauce, 1000, Duration.ofDays(5));
        stock(manager, basil, 1000, Duration.ofDays(5));

        Response stopped = send("PUT", "/stop-list/" + pizza.getId(), manager, null);
        assertThat(stopped.status()).isEqualTo(200);
        assertThat(stopped.body().get("manualStop").asBoolean()).isTrue();
        assertThat(stopped.body().get("isAvailable").asBoolean()).isFalse();
        assertThat(stopped.body().get("reasons").get(0).get("code").asString()).isEqualTo("manual");

        JsonNode list = get("/stop-list?reason=manual", employee).body();
        assertThat(list.get("total").asInt()).isEqualTo(1);

        Response order = post("/orders", employee, orderOf(null, pizza, 1));
        assertThat(order.status()).isEqualTo(409);
        assertThat(order.code()).isEqualTo("DISH_UNAVAILABLE");

        Response resumed = send("DELETE", "/stop-list/" + pizza.getId(), manager, null);
        assertThat(resumed.status()).isEqualTo(200);
        assertThat(resumed.body().get("manualStop").asBoolean()).isFalse();
        assertThat(resumed.body().get("isAvailable").asBoolean()).isTrue();
        assertThat(get("/stop-list", employee).body().get("total").asInt()).isZero();
        assertThat(post("/orders", employee, orderOf(null, pizza, 1)).status()).isEqualTo(201);
    }

    @Test
    void manualStopRequiresStaffAndExistingDish() {
        assertThat(send("PUT", "/stop-list/" + pizza.getId(), employee, null).status()).isEqualTo(403);
        Response missing = send("PUT", "/stop-list/" + java.util.UUID.randomUUID(), manager, null);
        assertThat(missing.status()).isEqualTo(404);
        assertThat(missing.code()).isEqualTo("DISH_NOT_FOUND");
    }

    @Test
    void excludedIngredientIsNotRequiredNorConsumed() {
        stock(manager, cheese, 1000, Duration.ofDays(5));
        stock(manager, sauce, 1000, Duration.ofDays(5));

        // Базилика нет вообще — обычный заказ отклоняется
        assertThat(post("/orders", employee, orderOf(null, pizza, 1)).status()).isEqualTo(409);

        Response order = post("/orders", employee, Map.of("items", List.of(
                Map.of("dishId", pizza.getId(), "quantity", 2, "excludedIngredientIds", List.of(basil.getId())))));

        assertThat(order.status()).isEqualTo(201);
        JsonNode item = order.body().get("items").get(0);
        assertThat(item.get("excludedIngredients").size()).isEqualTo(1);
        assertThat(item.get("excludedIngredients").get(0).get("ingredientName").asString()).isEqualTo("Базилик");
        assertThat(order.body().get("consumption").size()).isEqualTo(2);
        for (JsonNode c : order.body().get("consumption")) {
            assertThat(c.get("ingredientId").asString()).isNotEqualTo(basil.getId().toString());
        }
    }

    @Test
    void sameDishWithDifferentExclusionsKeepsSeparateLines() {
        stock(manager, cheese, 1000, Duration.ofDays(5));
        stock(manager, sauce, 1000, Duration.ofDays(5));
        stock(manager, basil, 1000, Duration.ofDays(5));

        Response order = post("/orders", employee, Map.of("items", List.of(
                Map.of("dishId", pizza.getId(), "quantity", 1),
                Map.of("dishId", pizza.getId(), "quantity", 2, "excludedIngredientIds", List.of(sauce.getId())))));

        assertThat(order.status()).isEqualTo(201);
        assertThat(order.body().get("items").size()).isEqualTo(2);
        for (JsonNode c : order.body().get("consumption")) {
            String id = c.get("ingredientId").asString();
            String expected = id.equals(sauce.getId().toString()) ? "80" : id.equals(cheese.getId().toString()) ? "360" : "15";
            assertThat(c.get("quantity").decimalValue()).isEqualByComparingTo(expected);
        }
    }

    @Test
    void cannotExcludeIngredientOutsideRecipe() {
        IngredientEntity olives = ingredient("Оливки", "g");
        Response order = post("/orders", employee, Map.of("items", List.of(
                Map.of("dishId", pizza.getId(), "quantity", 1, "excludedIngredientIds", List.of(olives.getId())))));

        assertThat(order.status()).isEqualTo(400);
        assertThat(order.code()).isEqualTo("INGREDIENT_NOT_IN_DISH");
    }
}
