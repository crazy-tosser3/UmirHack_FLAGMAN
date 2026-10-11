package com.example.UmirHack_Flagman.demo;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.UmirHack_Flagman.catalog.DishEntity;
import com.example.UmirHack_Flagman.catalog.DishRepoDB;
import com.example.UmirHack_Flagman.catalog.IngredientEntity;
import com.example.UmirHack_Flagman.catalog.IngredientRepoDB;
import com.example.UmirHack_Flagman.common.ApiException;
import com.example.UmirHack_Flagman.inventory.DeliveryService;
import com.example.UmirHack_Flagman.inventory.dto.DeliveryData;
import com.example.UmirHack_Flagman.orders.OrderService;
import com.example.UmirHack_Flagman.orders.dto.CreateOrderData;

// Генератор поставок и заказов для демонстрации. Включается app.simulator.enabled=true
@Component
public class EventSimulator {
    private static final Logger log = LoggerFactory.getLogger(EventSimulator.class);
    private static final int DELIVERY_EVERY_TICKS = 8;

    private final IngredientRepoDB ingredientRepo;
    private final DishRepoDB dishRepo;
    private final DeliveryService deliveryService;
    private final OrderService orderService;
    private final boolean enabled;
    private final Random random = new Random();
    private long tick;

    public EventSimulator(IngredientRepoDB ingredientRepo, DishRepoDB dishRepo, DeliveryService deliveryService,
            OrderService orderService, @Value("${app.simulator.enabled:false}") boolean enabled) {
        this.ingredientRepo = ingredientRepo;
        this.dishRepo = dishRepo;
        this.deliveryService = deliveryService;
        this.orderService = orderService;
        this.enabled = enabled;
    }

    @Scheduled(fixedDelayString = "${app.simulator.interval-ms:5000}", initialDelay = 10_000)
    public void tick() {
        if (!enabled) {
            return;
        }
        try {
            if (tick++ % DELIVERY_EVERY_TICKS == 0) {
                delivery();
            } else {
                order();
            }
        } catch (ApiException e) {
            log.info("Симулятор: {} — {}", e.getCode(), e.getMessage());
        } catch (RuntimeException e) {
            log.warn("Симулятор: ошибка", e);
        }
    }

    private void delivery() {
        List<IngredientEntity> ingredients = new ArrayList<>(ingredientRepo.findAll());
        if (ingredients.isEmpty()) {
            return;
        }
        Collections.shuffle(ingredients, random);
        Instant now = Instant.now();

        List<DeliveryData.Item> items = new ArrayList<>();
        for (IngredientEntity ingredient : ingredients.subList(0, Math.min(ingredients.size(), 3 + random.nextInt(4)))) {
            DeliveryData.Item item = new DeliveryData.Item();
            item.setIngredientId(ingredient.getId());
            item.setQuantity(BigDecimal.valueOf(switch (ingredient.getUnit()) {
                case "pcs" -> 20 + random.nextInt(41);
                case "ml" -> 1000 + random.nextInt(3001);
                default -> 2000 + random.nextInt(6001);
            }));
            item.setExpiresAt(now.plus(Duration.ofHours(12 + random.nextInt(156))));
            items.add(item);
        }

        DeliveryData req = new DeliveryData();
        req.setDeliveryNumber("SIM-" + now.toEpochMilli());
        req.setReceivedAt(now);
        req.setItems(items);
        deliveryService.create(req, null);
        log.info("Симулятор: поставка {} ({} позиций)", req.getDeliveryNumber(), items.size());
    }

    private void order() {
        List<DishEntity> dishes = new ArrayList<>(dishRepo.findAll().stream()
                .filter(d -> d.isActive() && !d.isManualStop()).toList());
        if (dishes.isEmpty()) {
            return;
        }
        Collections.shuffle(dishes, random);

        List<CreateOrderData.Item> items = new ArrayList<>();
        for (DishEntity dish : dishes.subList(0, Math.min(dishes.size(), 1 + random.nextInt(3)))) {
            CreateOrderData.Item item = new CreateOrderData.Item();
            item.setDishId(dish.getId());
            item.setQuantity(1 + random.nextInt(3));
            items.add(item);
        }

        CreateOrderData req = new CreateOrderData();
        req.setExternalOrderId("SIM-" + UUID.randomUUID().toString().substring(0, 8));
        req.setItems(items);
        orderService.create(req, null);
        log.info("Симулятор: заказ {}", req.getExternalOrderId());
    }
}
