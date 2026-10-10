package com.example.UmirHack_Flagman;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

import com.example.UmirHack_Flagman.Entity.DishEntity;
import com.example.UmirHack_Flagman.Entity.IngredientEntity;
import com.example.UmirHack_Flagman.models.Role;
import com.example.UmirHack_Flagman.support.IntegrationTest;

class ConcurrentOrdersTest extends IntegrationTest {
    private static final int REQUESTS = 30;

    @Test
    void parallelOrdersNeverOversellStock() throws Exception {
        String manager = tokenFor(Role.MANAGER);
        String employee = tokenFor(Role.EMPLOYEE);
        IngredientEntity cheese = ingredient("Моцарелла", "g");
        DishEntity pizza = dish("Маргарита", cheese, 100);
        // 10 порций в двух партиях
        stock(manager, cheese, 600, Duration.ofDays(1), 400, Duration.ofDays(2));

        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(REQUESTS)) {
            for (int i = 0; i < REQUESTS; i++) {
                Map<String, Object> body = orderOf("POS-" + i, pizza, 1);
                results.add(pool.submit(() -> {
                    start.await();
                    return post("/orders", employee, body).status();
                }));
            }
            start.countDown();
        }

        long created = 0, rejected = 0;
        for (Future<Integer> result : results) {
            int status = result.get();
            if (status == 201) {
                created++;
            } else if (status == 409) {
                rejected++;
            }
        }

        assertThat(created).isEqualTo(10);
        assertThat(rejected).isEqualTo(REQUESTS - 10);
        assertThat(jdbc.queryForObject("select sum(remaining_quantity) from stock_batches", BigDecimal.class))
                .isEqualByComparingTo("0");
        assertThat(jdbc.queryForObject("select count(*) from stock_batches where remaining_quantity < 0", Long.class)).isZero();
        assertThat(jdbc.queryForObject(
                "select sum(quantity) from stock_movements where type = 'ORDER_CONSUMPTION'", BigDecimal.class))
                .isEqualByComparingTo("1000");
    }
}
