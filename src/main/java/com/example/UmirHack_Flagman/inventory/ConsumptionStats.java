package com.example.UmirHack_Flagman.inventory;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.example.UmirHack_Flagman.common.IdSum;

// Средний расход ингредиентов под заказы за последние 7 дней (или с первого заказа, если данных меньше)
@Component
public class ConsumptionStats {
    public static final Duration WINDOW = Duration.ofDays(7);
    private static final Duration MIN_WINDOW = Duration.ofHours(1);

    private final StockMovementRepoDB movementRepo;

    public ConsumptionStats(StockMovementRepoDB movementRepo) {
        this.movementRepo = movementRepo;
    }

    public Map<UUID, Double> ratesPerSecond(Instant now) {
        Instant first = movementRepo.firstAt(MovementType.ORDER_CONSUMPTION);
        if (first == null) {
            return Map.of();
        }
        Instant since = now.minus(WINDOW);
        Duration window = Duration.between(first.isAfter(since) ? first : since, now);
        if (window.compareTo(MIN_WINDOW) < 0) {
            window = MIN_WINDOW;
        }

        Map<UUID, Double> rates = new HashMap<>();
        for (IdSum row : movementRepo.totalsSince(MovementType.ORDER_CONSUMPTION, since)) {
            rates.put(row.getId(), row.getTotal().doubleValue() / window.toSeconds());
        }
        return rates;
    }
}
