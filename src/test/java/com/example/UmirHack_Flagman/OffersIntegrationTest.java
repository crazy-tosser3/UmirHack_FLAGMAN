package com.example.UmirHack_Flagman;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.UmirHack_Flagman.auth.dto.Role;
import com.example.UmirHack_Flagman.catalog.IngredientEntity;
import com.example.UmirHack_Flagman.support.IntegrationTest;

import tools.jackson.databind.JsonNode;

class OffersIntegrationTest extends IntegrationTest {
    private String manager;
    private String employee;

    @BeforeEach
    void setUp() {
        manager = tokenFor(Role.MANAGER);
        employee = tokenFor(Role.EMPLOYEE);
    }

    @Test
    void soonerExpiryGivesHigherPriority() {
        IngredientEntity fish = ingredient("Лосось", "g");
        IngredientEntity cream = ingredient("Сливки", "ml");
        IngredientEntity rice = ingredient("Рис", "g");
        dish("Суши", fish, 50);
        dish("Суп", cream, 100);
        dish("Плов", rice, 150);
        stock(manager, fish, 1000, Duration.ofHours(6));
        stock(manager, cream, 1000, Duration.ofHours(48));
        stock(manager, rice, 1000, Duration.ofDays(30));

        JsonNode items = get("/offers", employee).body().get("items");

        // Рис истекает за горизонтом (72 ч) — блюдо не рекомендуется
        assertThat(names(items)).containsExactly("Суши", "Суп");
        assertThat(items.get(0).get("priority").asDouble()).isGreaterThan(items.get(1).get("priority").asDouble());
        assertThat(items.get(0).get("priority").asDouble()).isBetween(0.0, 100.0);
        assertThat(items.get(0).get("availablePortions").asInt()).isEqualTo(20);
        assertThat(items.get(0).get("reasons").get(0).get("explanation").asString()).contains("истекает");
    }

    @Test
    void unavailableDishIsNotRecommended() {
        IngredientEntity fish = ingredient("Лосось", "g");
        IngredientEntity nori = ingredient("Нори", "pcs");
        dish("Ролл", fish, 50, nori, 1);
        stock(manager, fish, 1000, Duration.ofHours(6));

        assertThat(get("/offers", employee).body().get("items").size()).isZero();
        assertThat(get("/stop-list", employee).body().get("total").asInt()).isEqualTo(1);
    }

    @Test
    void filtersAndValidation() {
        IngredientEntity fish = ingredient("Лосось", "g");
        IngredientEntity cream = ingredient("Сливки", "ml");
        dish("Суши", fish, 50);
        dish("Суп", cream, 100);
        stock(manager, fish, 1000, Duration.ofHours(6));
        stock(manager, cream, 1000, Duration.ofHours(60));

        assertThat(get("/offers?limit=1", employee).body().get("items").size()).isEqualTo(1);
        double top = get("/offers", employee).body().get("items").get(0).get("priority").asDouble();
        assertThat(names(get("/offers?minPriority=" + (top - 0.1), employee).body().get("items"))).containsExactly("Суши");

        assertThat(get("/offers?limit=0", employee).status()).isEqualTo(400);
        assertThat(get("/offers?minPriority=101", employee).status()).isEqualTo(400);
    }

    private static List<String> names(JsonNode items) {
        List<String> names = new ArrayList<>();
        items.forEach(i -> names.add(i.get("dishName").asString()));
        return names;
    }
}
