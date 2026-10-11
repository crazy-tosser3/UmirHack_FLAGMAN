package com.example.UmirHack_Flagman.support;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.UmirHack_Flagman.Entity.DeliveryEntity;
import com.example.UmirHack_Flagman.Entity.DishEntity;
import com.example.UmirHack_Flagman.Entity.DishIngredientEntity;
import com.example.UmirHack_Flagman.Entity.IngredientEntity;
import com.example.UmirHack_Flagman.Entity.StockBatchEntity;
import com.example.UmirHack_Flagman.Entity.UserEntity;
import com.example.UmirHack_Flagman.Repositories.DeliveryRepoDB;
import com.example.UmirHack_Flagman.Repositories.DishRepoDB;
import com.example.UmirHack_Flagman.Repositories.IngredientRepoDB;
import com.example.UmirHack_Flagman.Repositories.StockBatchRepoDB;
import com.example.UmirHack_Flagman.Repositories.UserRepoDB;
import com.example.UmirHack_Flagman.Services.InviteService;
import com.example.UmirHack_Flagman.models.Role;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.seed.enabled=false",
        "app.simulator.enabled=false",
        "app.auth.cookie-secure=false",
        "app.inventory.expiry-check-interval-ms=3600000",
        "app.inventory.expiry-warning-hours=24",
        "app.inventory.offers-horizon-hours=72",
        "app.admin.email=admin@flagman.local",
        "app.admin.password=" + IntegrationTest.PASSWORD,
        "spring.jpa.show-sql=false"
})
@Import(TestcontainersConfig.class)
public abstract class IntegrationTest {
    protected static final String PASSWORD = "TestPassword-123";

    @LocalServerPort
    private int port;

    @Autowired
    protected JdbcTemplate jdbc;
    @Autowired
    protected JsonMapper json;
    @Autowired
    protected IngredientRepoDB ingredientRepo;
    @Autowired
    protected DishRepoDB dishRepo;
    @Autowired
    protected DeliveryRepoDB deliveryRepo;
    @Autowired
    protected StockBatchRepoDB batchRepo;
    @Autowired
    protected UserRepoDB userRepo;
    @Autowired
    private PlatformTransactionManager txManager;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private InviteService inviteService;

    private final HttpClient http = HttpClient.newHttpClient();

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("""
                TRUNCATE stock_movements, stock_deficits, order_items, orders, stock_batches, deliveries,
                         dish_ingredients, dishes, ingredients, idempotency_keys, users CASCADE""");
    }

    // HTTP

    public record Response(int status, JsonNode body, HttpHeaders headers) {
        public String code() {
            return body.get("error").get("code").asString();
        }

        public String cookie(String name) {
            return headers.allValues("set-cookie").stream()
                    .filter(c -> c.startsWith(name + "="))
                    .findFirst().orElse(null);
        }

        public String cookieValue(String name) {
            String cookie = cookie(name);
            return cookie == null ? null : cookie.substring(name.length() + 1, cookie.indexOf(';'));
        }
    }

    protected Response get(String path, String token) {
        return send(request(path, token).GET());
    }

    protected Response post(String path, String token, Object body, String... headers) {
        HttpRequest.Builder builder = request(path, token)
                .header("Content-Type", "application/json")
                .POST(body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        for (int i = 0; i < headers.length; i += 2) {
            builder.header(headers[i], headers[i + 1]);
        }
        return send(builder);
    }

    protected Response send(String method, String path, String token, Object body) {
        return send(request(path, token)
                .header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))));
    }

    private HttpRequest.Builder request(String path, String token) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1" + path))
                .timeout(Duration.ofSeconds(30));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder;
    }

    private Response send(HttpRequest.Builder builder) {
        try {
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            JsonNode body = response.body().isEmpty() ? null : json.readTree(response.body());
            return new Response(response.statusCode(), body, response.headers());
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    // Пользователи

    protected Map<String, Object> registration(String email) {
        return Map.of("email", email, "password", PASSWORD, "name", "Тест");
    }

    protected Response login(String email, String password) {
        return post("/auth/login", null, Map.of("email", email, "password", password));
    }

    protected Map<String, Object> registration(String email, String code) {
        Map<String, Object> body = new LinkedHashMap<>(registration(email));
        body.put("code", code);
        return body;
    }

    // Без кода регистрация закрыта, поэтому тестовые пользователи создаются напрямую в БД с паролем PASSWORD
    protected UserEntity existingUser(String email, Role role) {
        UserEntity user = new UserEntity("Тест", email, passwordEncoder.encode(PASSWORD));
        user.setRole(role);
        return userRepo.save(user);
    }

    // Код приглашения в обход админского API — для тестов самой регистрации
    protected String inviteCode(Role role) {
        String code = UUID.randomUUID().toString();
        inviteService.create(code, role.name().toLowerCase(), Instant.now().plus(Duration.ofHours(1)).toString(), 0);
        return code;
    }

    protected String tokenFor(Role role) {
        String email = role.name().toLowerCase() + "-" + UUID.randomUUID() + "@test.local";
        existingUser(email, role);
        return login(email, PASSWORD).body().get("accessToken").asString();
    }

    // Каталог и склад

    protected IngredientEntity ingredient(String name, String unit) {
        return ingredientRepo.save(new IngredientEntity(name, unit));
    }

    // recipe: ингредиент, количество на порцию, ...
    protected DishEntity dish(String name, Object... recipe) {
        return tx().execute(status -> {
            DishEntity dish = new DishEntity(name);
            for (int i = 0; i < recipe.length; i += 2) {
                dish.getIngredients().add(new DishIngredientEntity(
                        dish, (IngredientEntity) recipe[i], new BigDecimal(recipe[i + 1].toString())));
            }
            return dishRepo.save(dish);
        });
    }

    protected Map<String, Object> deliveryItem(IngredientEntity ingredient, int quantity, Duration expiresIn) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("ingredientId", ingredient.getId());
        item.put("quantity", quantity);
        item.put("expiresAt", Instant.now().plus(expiresIn).toString());
        return item;
    }

    protected Map<String, Object> delivery(String number, List<Map<String, Object>> items) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("deliveryNumber", number);
        body.put("receivedAt", Instant.now().toString());
        body.put("items", items);
        return body;
    }

    // Партии через API, каждая отдельной поставкой, чтобы порядок создания не совпадал с порядком сроков
    protected List<UUID> stock(String managerToken, IngredientEntity ingredient, Object... quantityAndExpiry) {
        List<UUID> batchIds = new ArrayList<>();
        for (int i = 0; i < quantityAndExpiry.length; i += 2) {
            Response r = post("/deliveries", managerToken, delivery("D-" + UUID.randomUUID(),
                    List.of(deliveryItem(ingredient, (Integer) quantityAndExpiry[i], (Duration) quantityAndExpiry[i + 1]))));
            if (r.status() != 201) {
                throw new IllegalStateException("Поставка не создана: " + r.body());
            }
            batchIds.add(UUID.fromString(r.body().get("items").get(0).get("batchId").asString()));
        }
        return batchIds;
    }

    // API не принимает просроченный товар, поэтому такая партия создается напрямую
    protected UUID expiredBatch(IngredientEntity ingredient, int quantity) {
        return tx().execute(status -> {
            DeliveryEntity delivery = deliveryRepo.save(new DeliveryEntity("OLD-" + UUID.randomUUID(), Instant.now().minus(Duration.ofDays(5))));
            return batchRepo.save(new StockBatchEntity(delivery, ingredient, BigDecimal.valueOf(quantity),
                    Instant.now().minus(Duration.ofHours(1)))).getId();
        });
    }

    protected BigDecimal remaining(UUID batchId) {
        return jdbc.queryForObject("select remaining_quantity from stock_batches where id = ?", BigDecimal.class, batchId);
    }

    protected Map<String, Object> orderOf(String externalId, Object... dishAndQuantity) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = 0; i < dishAndQuantity.length; i += 2) {
            items.add(Map.of("dishId", ((DishEntity) dishAndQuantity[i]).getId(), "quantity", dishAndQuantity[i + 1]));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        if (externalId != null) {
            body.put("externalOrderId", externalId);
        }
        body.put("items", items);
        return body;
    }

    protected TransactionTemplate tx() {
        return new TransactionTemplate(txManager);
    }
}
