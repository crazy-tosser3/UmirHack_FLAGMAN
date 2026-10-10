package com.example.UmirHack_Flagman;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.UmirHack_Flagman.Entity.IngredientEntity;
import com.example.UmirHack_Flagman.models.Role;
import com.example.UmirHack_Flagman.support.IntegrationTest;

import tools.jackson.databind.JsonNode;

class DeliveryAndStockIntegrationTest extends IntegrationTest {
    private String manager;
    private IngredientEntity cheese;

    @BeforeEach
    void setUp() {
        manager = tokenFor(Role.MANAGER);
        cheese = ingredient("Моцарелла", "g");
    }

    @Test
    void deliveryCreatesBatchesAndIncreasesStock() {
        Response created = post("/deliveries", manager, delivery("DEL-1", List.of(
                deliveryItem(cheese, 1000, Duration.ofDays(3)),
                deliveryItem(cheese, 500, Duration.ofDays(1)))));

        assertThat(created.status()).isEqualTo(201);
        assertThat(created.body().get("status").asString()).isEqualTo("accepted");
        assertThat(created.body().get("items").size()).isEqualTo(2);

        JsonNode stock = get("/stock?ingredientId=" + cheese.getId(), manager).body().get("items").get(0);
        assertThat(stock.get("totalQuantity").decimalValue()).isEqualByComparingTo("1500");
        assertThat(stock.get("availableQuantity").decimalValue()).isEqualByComparingTo("1500");

        Response fetched = get("/deliveries/" + created.body().get("id").asString(), manager);
        assertThat(fetched.status()).isEqualTo(200);
        assertThat(fetched.body().get("deliveryNumber").asString()).isEqualTo("DEL-1");
    }

    @Test
    void duplicateDeliveryNumberIsRejected() {
        post("/deliveries", manager, delivery("DEL-1", List.of(deliveryItem(cheese, 100, Duration.ofDays(3)))));

        Response duplicate = post("/deliveries", manager, delivery("DEL-1", List.of(deliveryItem(cheese, 100, Duration.ofDays(3)))));

        assertThat(duplicate.status()).isEqualTo(409);
        assertThat(duplicate.code()).isEqualTo("DELIVERY_ALREADY_EXISTS");
    }

    @Test
    void idempotentRetryDoesNotIncreaseStockTwice() {
        Map<String, Object> body = delivery("DEL-1", List.of(deliveryItem(cheese, 700, Duration.ofDays(3))));

        Response first = post("/deliveries", manager, body, "Idempotency-Key", "import-1");
        Response retry = post("/deliveries", manager, body, "Idempotency-Key", "import-1");

        assertThat(retry.status()).isEqualTo(201);
        assertThat(retry.body().get("id")).isEqualTo(first.body().get("id"));
        assertThat(get("/stock", manager).body().get("items").get(0).get("totalQuantity").decimalValue())
                .isEqualByComparingTo("700");

        Response otherBody = post("/deliveries", manager,
                delivery("DEL-2", List.of(deliveryItem(cheese, 1, Duration.ofDays(3)))), "Idempotency-Key", "import-1");
        assertThat(otherBody.status()).isEqualTo(409);
        assertThat(otherBody.code()).isEqualTo("IDEMPOTENCY_KEY_REUSED");
    }

    @Test
    void invalidDeliveriesAreRejected() {
        Response expired = post("/deliveries", manager, delivery("DEL-1", List.of(deliveryItem(cheese, 100, Duration.ofHours(-1)))));
        assertThat(expired.status()).isEqualTo(400);
        assertThat(expired.body().get("error").get("details").get(0).get("reason").asString()).isEqualTo("already_expired");

        Map<String, Object> unknown = deliveryItem(cheese, 100, Duration.ofDays(1));
        unknown.put("ingredientId", UUID.randomUUID());
        Response notFound = post("/deliveries", manager, delivery("DEL-2", List.of(unknown)));
        assertThat(notFound.status()).isEqualTo(404);
        assertThat(notFound.code()).isEqualTo("INGREDIENT_NOT_FOUND");

        assertThat(post("/deliveries", manager, delivery("DEL-3", new ArrayList<>())).status()).isEqualTo(400);
        assertThat(post("/deliveries", manager, delivery("DEL-4", List.of(deliveryItem(cheese, 0, Duration.ofDays(1))))).status())
                .isEqualTo(400);
        assertThat(deliveryRepo.count()).isZero();
    }

    @Test
    void batchesHaveExpiryStatusAndRemainingShelfLife() {
        stock(manager, cheese, 100, Duration.ofHours(10), 200, Duration.ofDays(3));
        expiredBatch(cheese, 300);

        JsonNode all = get("/stock/batches", manager).body();
        assertThat(all.get("total").asInt()).isEqualTo(3);
        assertThat(statuses(all)).containsExactly("expired", "expiring", "valid");

        JsonNode expiring = get("/stock/batches?expiryStatus=expiring", manager).body().get("items");
        assertThat(expiring.size()).isEqualTo(1);
        assertThat(expiring.get(0).get("remainingShelfLifeSeconds").asLong()).isBetween(9 * 3600L, 10 * 3600L);

        JsonNode expired = get("/stock/batches?expiryStatus=expired", manager).body().get("items").get(0);
        assertThat(expired.get("availableQuantity").decimalValue()).isEqualByComparingTo("0");
        assertThat(expired.get("remainingShelfLifeSeconds").asLong()).isZero();

        JsonNode stock = get("/stock", manager).body().get("items").get(0);
        assertThat(stock.get("totalQuantity").decimalValue()).isEqualByComparingTo("600");
        assertThat(stock.get("availableQuantity").decimalValue()).isEqualByComparingTo("300");
        assertThat(stock.get("expiredQuantity").decimalValue()).isEqualByComparingTo("300");

        assertThat(get("/stock/batches?expiryStatus=foo", manager).status()).isEqualTo(400);
    }

    @Test
    void stockAndDeliveriesAreForStaffOnly() {
        String employee = tokenFor(Role.EMPLOYEE);

        assertThat(get("/stock", employee).status()).isEqualTo(403);
        assertThat(get("/stock/batches", employee).status()).isEqualTo(403);
        assertThat(post("/deliveries", employee, delivery("DEL-1", List.of(deliveryItem(cheese, 1, Duration.ofDays(1))))).status())
                .isEqualTo(403);
        assertThat(get("/ingredients", employee).status()).isEqualTo(200);
    }

    private static List<String> statuses(JsonNode page) {
        List<String> result = new ArrayList<>();
        page.get("items").forEach(i -> result.add(i.get("expiryStatus").asString()));
        return result;
    }
}
