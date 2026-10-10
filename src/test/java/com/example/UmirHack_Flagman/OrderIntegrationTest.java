package com.example.UmirHack_Flagman;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.UmirHack_Flagman.Entity.DishEntity;
import com.example.UmirHack_Flagman.Entity.IngredientEntity;
import com.example.UmirHack_Flagman.models.Role;
import com.example.UmirHack_Flagman.support.IntegrationTest;

import tools.jackson.databind.JsonNode;

class OrderIntegrationTest extends IntegrationTest {
    private String manager;
    private String employee;
    private IngredientEntity cheese;
    private IngredientEntity sauce;
    private DishEntity pizza;

    @BeforeEach
    void setUp() {
        manager = tokenFor(Role.MANAGER);
        employee = tokenFor(Role.EMPLOYEE);
        cheese = ingredient("Моцарелла", "g");
        sauce = ingredient("Соус", "g");
        pizza = dish("Маргарита", cheese, 120, sauce, 80);
    }

    @Test
    void consumesBatchesWithNearestExpiryFirst() {
        List<UUID> batches = stock(manager, cheese, 300, Duration.ofDays(5), 200, Duration.ofDays(1), 500, Duration.ofDays(10));
        stock(manager, sauce, 1000, Duration.ofDays(5));
        UUID later = batches.get(0), soonest = batches.get(1), latest = batches.get(2);

        Response order = post("/orders", employee, orderOf("POS-1", pizza, 4));

        assertThat(order.status()).isEqualTo(201);
        assertThat(order.body().get("status").asString()).isEqualTo("confirmed");
        JsonNode allocations = consumptionOf(order.body(), cheese).get("allocations");
        assertThat(allocations.size()).isEqualTo(2);
        assertThat(allocations.get(0).get("batchId").asString()).isEqualTo(soonest.toString());
        assertThat(allocations.get(0).get("quantity").decimalValue()).isEqualByComparingTo("200");
        assertThat(allocations.get(1).get("batchId").asString()).isEqualTo(later.toString());
        assertThat(allocations.get(1).get("quantity").decimalValue()).isEqualByComparingTo("280");

        assertThat(remaining(soonest)).isEqualByComparingTo("0");
        assertThat(remaining(later)).isEqualByComparingTo("20");
        assertThat(remaining(latest)).isEqualByComparingTo("500");
    }

    @Test
    void expiredBatchesAreNeverUsed() {
        stock(manager, cheese, 100, Duration.ofDays(2));
        UUID expired = expiredBatch(cheese, 1000);
        stock(manager, sauce, 1000, Duration.ofDays(5));

        Response order = post("/orders", employee, orderOf(null, pizza, 1));

        assertThat(order.status()).isEqualTo(409);
        assertThat(order.code()).isEqualTo("INSUFFICIENT_STOCK");
        assertThat(order.body().get("error").get("details").get(0).get("availableQuantity").decimalValue())
                .isEqualByComparingTo("100");
        assertThat(remaining(expired)).isEqualByComparingTo("1000");

        // Ингредиента хватило бы с учетом просрочки — причина стопа именно в ней
        JsonNode stopped = get("/stop-list", employee).body().get("items").get(0);
        assertThat(stopped.get("reasons").get(0).get("code").asString()).isEqualTo("expired_ingredient");
    }

    @Test
    void insufficientIngredientRejectsWholeOrder() {
        List<UUID> cheeseBatch = stock(manager, cheese, 1000, Duration.ofDays(5));
        stock(manager, sauce, 100, Duration.ofDays(5));

        Response order = post("/orders", employee, orderOf("POS-1", pizza, 2));

        assertThat(order.status()).isEqualTo(409);
        JsonNode details = order.body().get("error").get("details");
        assertThat(details.size()).isEqualTo(1);
        assertThat(details.get(0).get("ingredientId").asString()).isEqualTo(sauce.getId().toString());
        assertThat(details.get(0).get("missingQuantity").decimalValue()).isEqualByComparingTo("60");

        assertThat(remaining(cheeseBatch.getFirst())).isEqualByComparingTo("1000");
        assertThat(jdbc.queryForObject("select count(*) from orders", Long.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from stock_deficits", Long.class)).isEqualTo(1);
    }

    @Test
    void needIsSummedAcrossDishesSharingIngredient() {
        DishEntity cheeseBread = dish("Хлеб с сыром", cheese, 100);
        stock(manager, cheese, 200, Duration.ofDays(5));
        stock(manager, sauce, 1000, Duration.ofDays(5));

        // По отдельности оба блюда помещаются (120 и 100), вместе — нет (220 > 200)
        Response order = post("/orders", employee, orderOf(null, pizza, 1, cheeseBread, 1));

        assertThat(order.status()).isEqualTo(409);
        assertThat(order.body().get("error").get("details").get(0).get("requiredQuantity").decimalValue())
                .isEqualByComparingTo("220");
    }

    @Test
    void idempotencyKeyAndExternalIdPreventDoubleProcessing() {
        List<UUID> cheeseBatch = stock(manager, cheese, 1000, Duration.ofDays(5));
        stock(manager, sauce, 1000, Duration.ofDays(5));
        Map<String, Object> body = orderOf("POS-1", pizza, 1);

        Response first = post("/orders", employee, body, "Idempotency-Key", "order-1");
        Response retry = post("/orders", employee, body, "Idempotency-Key", "order-1");

        assertThat(retry.status()).isEqualTo(201);
        assertThat(retry.body().get("id")).isEqualTo(first.body().get("id"));
        assertThat(remaining(cheeseBatch.getFirst())).isEqualByComparingTo("880");

        Response otherBody = post("/orders", employee, orderOf("POS-2", pizza, 2), "Idempotency-Key", "order-1");
        assertThat(otherBody.code()).isEqualTo("IDEMPOTENCY_KEY_REUSED");

        Response sameExternalId = post("/orders", employee, body);
        assertThat(sameExternalId.status()).isEqualTo(409);
        assertThat(sameExternalId.code()).isEqualTo("ORDER_ALREADY_EXISTS");
    }

    @Test
    void unavailableUnknownAndInvalidOrders() {
        DishEntity inactive = dish("Старое блюдо", cheese, 10);
        inactive.setActive(false);
        dishRepo.save(inactive);

        assertThat(post("/orders", employee, orderOf(null, inactive, 1)).code()).isEqualTo("DISH_UNAVAILABLE");

        Response unknown = post("/orders", employee, Map.of("items", List.of(Map.of("dishId", UUID.randomUUID(), "quantity", 1))));
        assertThat(unknown.status()).isEqualTo(404);
        assertThat(unknown.code()).isEqualTo("DISH_NOT_FOUND");

        assertThat(post("/orders", employee, orderOf(null, pizza, 0)).status()).isEqualTo(400);
        assertThat(post("/orders", employee, Map.of("items", List.of())).status()).isEqualTo(400);
    }

    @Test
    void orderDetailsAndConsumptionAreAvailable() {
        stock(manager, cheese, 1000, Duration.ofDays(5));
        stock(manager, sauce, 1000, Duration.ofDays(5));
        String id = post("/orders", employee, orderOf("POS-1", pizza, 2)).body().get("id").asString();

        JsonNode order = get("/orders/" + id, employee).body();
        assertThat(order.get("items").get(0).get("dishName").asString()).isEqualTo("Маргарита");
        assertThat(order.get("consumption").size()).isEqualTo(2);

        Response consumption = get("/orders/" + id + "/consumption", manager);
        assertThat(consumption.body().get("items").size()).isEqualTo(2);
        assertThat(get("/orders/" + id + "/consumption", employee).status()).isEqualTo(403);
        assertThat(get("/orders/" + UUID.randomUUID(), employee).code()).isEqualTo("ORDER_NOT_FOUND");
    }

    @Test
    void dishGoesToStopListWhenStockRunsOut() {
        stock(manager, cheese, 240, Duration.ofDays(5));
        stock(manager, sauce, 1000, Duration.ofDays(5));
        assertThat(get("/stop-list", employee).body().get("total").asInt()).isZero();

        post("/orders", employee, orderOf(null, pizza, 2));

        JsonNode stopList = get("/stop-list", employee).body();
        assertThat(stopList.get("total").asInt()).isEqualTo(1);
        JsonNode reason = stopList.get("items").get(0).get("reasons").get(0);
        assertThat(reason.get("code").asString()).isEqualTo("insufficient_stock");
        assertThat(reason.get("ingredientName").asString()).isEqualTo("Моцарелла");
        assertThat(get("/stop-list?reason=manual", employee).body().get("total").asInt()).isZero();
    }

    private JsonNode consumptionOf(JsonNode order, IngredientEntity ingredient) {
        for (JsonNode c : order.get("consumption")) {
            if (c.get("ingredientId").asString().equals(ingredient.getId().toString())) {
                return c;
            }
        }
        throw new AssertionError("Нет списания для " + ingredient.getName());
    }
}
