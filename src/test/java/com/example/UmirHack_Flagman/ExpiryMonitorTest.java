package com.example.UmirHack_Flagman;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.UmirHack_Flagman.auth.dto.Role;
import com.example.UmirHack_Flagman.catalog.IngredientEntity;
import com.example.UmirHack_Flagman.inventory.ExpiryMonitor;
import com.example.UmirHack_Flagman.support.IntegrationTest;

import tools.jackson.databind.JsonNode;

class ExpiryMonitorTest extends IntegrationTest {

    @Autowired
    private ExpiryMonitor monitor;

    @Test
    void expiredBatchIsWrittenOffExactlyOnce() {
        String manager = tokenFor(Role.MANAGER);
        IngredientEntity cheese = ingredient("Моцарелла", "g");
        UUID batch = expiredBatch(cheese, 300);

        monitor.run(Instant.now());
        monitor.run(Instant.now());

        assertThat(remaining(batch)).isEqualByComparingTo("0");
        assertThat(jdbc.queryForObject(
                "select count(*) from stock_movements where type = 'EXPIRED_WRITE_OFF' and batch_id = ?", Long.class, batch))
                .isEqualTo(1);

        JsonNode writeOffs = get("/analytics/write-offs", manager).body().get("items").get(0);
        assertThat(writeOffs.get("ingredientName").asString()).isEqualTo("Моцарелла");
        assertThat(writeOffs.get("quantity").decimalValue()).isEqualByComparingTo("300");
    }

    @Test
    void expiringBatchIsMarkedButNotTouched() {
        String manager = tokenFor(Role.MANAGER);
        IngredientEntity cheese = ingredient("Моцарелла", "g");
        UUID soon = stock(manager, cheese, 100, Duration.ofHours(2)).getFirst();
        UUID later = stock(manager, cheese, 100, Duration.ofDays(5)).getFirst();

        monitor.run(Instant.now());

        assertThat(notified(soon)).isTrue();
        assertThat(notified(later)).isFalse();
        assertThat(remaining(soon)).isEqualByComparingTo("100");
    }

    private boolean notified(UUID batchId) {
        return jdbc.queryForObject("select expiring_notified from stock_batches where id = ?", Boolean.class, batchId);
    }
}
