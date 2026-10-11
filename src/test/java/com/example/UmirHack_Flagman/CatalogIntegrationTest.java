package com.example.UmirHack_Flagman;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.UmirHack_Flagman.auth.dto.Role;
import com.example.UmirHack_Flagman.catalog.IngredientEntity;
import com.example.UmirHack_Flagman.support.IntegrationTest;

import tools.jackson.databind.JsonNode;

class CatalogIntegrationTest extends IntegrationTest {
    private String admin;

    @BeforeEach
    void setUp() {
        admin = tokenFor(Role.ADMIN);
    }

    @Test
    void createsIngredients() {
        Response created = post("/ingredients", admin, Map.of("items", List.of(
                Map.of("name", "Базилик", "unit", "g"),
                Map.of("name", "Апельсиновый сок", "unit", "ml"))));

        assertThat(created.status()).isEqualTo(201);
        assertThat(created.body().get("items").size()).isEqualTo(2);
        assertThat(created.body().get("items").get(0).get("id").asString()).isNotBlank();
        assertThat(ingredientRepo.count()).isEqualTo(2);
    }

    @Test
    void ingredientValidation() {
        ingredient("Базилик", "g");

        Response existing = post("/ingredients", admin, Map.of("items", List.of(Map.of("name", "БАЗИЛИК", "unit", "g"))));
        assertThat(existing.status()).isEqualTo(409);
        assertThat(existing.code()).isEqualTo("INGREDIENT_ALREADY_EXISTS");

        assertThat(post("/ingredients", admin, Map.of("items", List.of(
                Map.of("name", "Мята", "unit", "g"), Map.of("name", "мята", "unit", "g")))).status()).isEqualTo(400);
        assertThat(post("/ingredients", admin, Map.of("items", List.of(Map.of("name", "Мята", "unit", "kg")))).status())
                .isEqualTo(400);
        assertThat(post("/ingredients", tokenFor(Role.MANAGER), Map.of("items", List.of(Map.of("name", "Мята", "unit", "g")))).status())
                .isEqualTo(403);
        assertThat(ingredientRepo.count()).isEqualTo(1);
    }

    @Test
    void createsDishWithRecipe() {
        IngredientEntity cheese = ingredient("Моцарелла", "g");
        IngredientEntity sauce = ingredient("Соус", "g");

        Response created = post("/dishes", admin, Map.of("items", List.of(Map.of(
                "name", "Маргарита",
                "ingredients", List.of(
                        Map.of("ingredientId", cheese.getId(), "quantity", 120),
                        Map.of("ingredientId", sauce.getId(), "quantity", 80))))));

        assertThat(created.status()).isEqualTo(201);
        JsonNode dish = created.body().get("items").get(0);
        assertThat(dish.get("name").asString()).isEqualTo("Маргарита");
        assertThat(dish.get("isActive").asBoolean()).isTrue();
        assertThat(dish.get("ingredients").size()).isEqualTo(2);
        assertThat(dish.get("ingredients").get(0).get("unit").asString()).isEqualTo("g");

        JsonNode listed = get("/dishes", admin).body().get("items").get(0);
        assertThat(listed.get("ingredients").size()).isEqualTo(2);
    }

    @Test
    void dishValidationIsAtomic() {
        IngredientEntity cheese = ingredient("Моцарелла", "g");
        dish("Маргарита", cheese, 120);

        Response existing = post("/dishes", admin, dishes("маргарита", cheese.getId()));
        assertThat(existing.status()).isEqualTo(409);
        assertThat(existing.code()).isEqualTo("DISH_ALREADY_EXISTS");

        // Второе блюдо ссылается на несуществующий ингредиент — не сохраняется ни одно
        Response missing = post("/dishes", admin, Map.of("items", List.of(
                dishBody("Сырная тарелка", cheese.getId()),
                dishBody("Пустое", UUID.randomUUID()))));
        assertThat(missing.status()).isEqualTo(404);
        assertThat(missing.code()).isEqualTo("INGREDIENT_NOT_FOUND");
        assertThat(dishRepo.count()).isEqualTo(1);

        Map<String, Object> repeated = Map.of("name", "Двойной сыр", "ingredients", List.of(
                Map.of("ingredientId", cheese.getId(), "quantity", 50),
                Map.of("ingredientId", cheese.getId(), "quantity", 60)));
        assertThat(post("/dishes", admin, Map.of("items", List.of(repeated))).status()).isEqualTo(400);

        assertThat(post("/dishes", admin, Map.of("items", List.of(Map.of("name", "Без состава", "ingredients", List.of()))))
                .status()).isEqualTo(400);
        assertThat(post("/dishes", admin, Map.of("items", List.of(Map.of("name", "Ноль", "ingredients",
                List.of(Map.of("ingredientId", cheese.getId(), "quantity", 0)))))).status()).isEqualTo(400);
        assertThat(post("/dishes", tokenFor(Role.EMPLOYEE), dishes("Сырная тарелка", cheese.getId())).status()).isEqualTo(403);
    }

    private static Map<String, Object> dishBody(String name, UUID ingredientId) {
        return Map.of("name", name, "ingredients", List.of(Map.of("ingredientId", ingredientId, "quantity", 100)));
    }

    private static Map<String, Object> dishes(String name, UUID ingredientId) {
        return Map.of("items", List.of(dishBody(name, ingredientId)));
    }
}
