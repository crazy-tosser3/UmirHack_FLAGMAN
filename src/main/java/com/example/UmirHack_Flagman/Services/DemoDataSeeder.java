package com.example.UmirHack_Flagman.Services;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.UmirHack_Flagman.Entity.DishEntity;
import com.example.UmirHack_Flagman.Entity.DishIngredientEntity;
import com.example.UmirHack_Flagman.Entity.IngredientEntity;
import com.example.UmirHack_Flagman.Repositories.DishRepoDB;
import com.example.UmirHack_Flagman.Repositories.IngredientRepoDB;
import com.example.UmirHack_Flagman.models.DeliveryData;

// Демо-каталог и стартовая поставка для пустой БД. Редактирование каталога — функционал admin
@Component
public class DemoDataSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final IngredientRepoDB ingredientRepo;
    private final DishRepoDB dishRepo;
    private final DeliveryService deliveryService;
    private final TransactionTemplate tx;
    private final boolean enabled;

    public DemoDataSeeder(IngredientRepoDB ingredientRepo, DishRepoDB dishRepo, DeliveryService deliveryService,
            PlatformTransactionManager txManager, @Value("${app.seed.enabled:true}") boolean enabled) {
        this.ingredientRepo = ingredientRepo;
        this.dishRepo = dishRepo;
        this.deliveryService = deliveryService;
        this.tx = new TransactionTemplate(txManager);
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled || ingredientRepo.count() > 0) {
            return;
        }
        Map<String, IngredientEntity> ingredients = tx.execute(status -> seedCatalog());
        seedDelivery(ingredients);
        log.info("Загружен демо-каталог: {} ингредиентов, {} блюд", ingredients.size(), dishRepo.count());
    }

    private Map<String, IngredientEntity> seedCatalog() {
        Map<String, IngredientEntity> i = new LinkedHashMap<>();
        for (String[] row : new String[][] {
                {"Моцарелла", "g"}, {"Томатный соус", "g"}, {"Тесто для пиццы", "g"}, {"Пепперони", "g"},
                {"Шампиньоны", "g"}, {"Куриное филе", "g"}, {"Салат романо", "g"}, {"Пармезан", "g"},
                {"Сливки", "ml"}, {"Спагетти", "g"}, {"Бекон", "g"}, {"Яйцо", "pcs"}}) {
            i.put(row[0], ingredientRepo.save(new IngredientEntity(row[0], row[1])));
        }

        dish("Пицца Маргарита", i, "Тесто для пиццы", 250, "Томатный соус", 80, "Моцарелла", 120);
        dish("Пицца Пепперони", i, "Тесто для пиццы", 250, "Томатный соус", 80, "Моцарелла", 100, "Пепперони", 70);
        dish("Пицца с грибами", i, "Тесто для пиццы", 250, "Томатный соус", 60, "Моцарелла", 100, "Шампиньоны", 80);
        dish("Цезарь с курицей", i, "Салат романо", 120, "Куриное филе", 100, "Пармезан", 20);
        dish("Паста Карбонара", i, "Спагетти", 120, "Бекон", 60, "Сливки", 80, "Яйцо", 1, "Пармезан", 15);
        return i;
    }

    private void dish(String name, Map<String, IngredientEntity> ingredients, Object... recipe) {
        DishEntity dish = new DishEntity(name);
        for (int k = 0; k < recipe.length; k += 2) {
            dish.getIngredients().add(new DishIngredientEntity(
                    dish, ingredients.get((String) recipe[k]), BigDecimal.valueOf((Integer) recipe[k + 1])));
        }
        dishRepo.save(dish);
    }

    private void seedDelivery(Map<String, IngredientEntity> ingredients) {
        Instant now = Instant.now();
        Object[][] stock = {
                {"Моцарелла", 3000, 20}, {"Моцарелла", 5000, 120}, {"Томатный соус", 4000, 96},
                {"Тесто для пиццы", 8000, 48}, {"Пепперони", 1500, 30}, {"Шампиньоны", 600, 14},
                {"Куриное филе", 2500, 60}, {"Салат романо", 1500, 36}, {"Пармезан", 800, 240},
                {"Сливки", 2000, 72}, {"Спагетти", 5000, 720}, {"Бекон", 300, 100}, {"Яйцо", 30, 200}};

        List<DeliveryData.Item> items = new ArrayList<>();
        for (Object[] row : stock) {
            DeliveryData.Item item = new DeliveryData.Item();
            item.setIngredientId(ingredients.get((String) row[0]).getId());
            item.setQuantity(BigDecimal.valueOf((Integer) row[1]));
            item.setExpiresAt(now.plus(Duration.ofHours((Integer) row[2])));
            items.add(item);
        }

        DeliveryData delivery = new DeliveryData();
        delivery.setDeliveryNumber("DEMO-0001");
        delivery.setReceivedAt(now);
        delivery.setItems(items);
        deliveryService.create(delivery, null);
    }
}
