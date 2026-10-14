package com.example.UmirHack_Flagman.inventory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class StockVersionService {
    private static final Logger log = LoggerFactory.getLogger(StockVersionService.class);
    private static final String KEY = "inventory:stock-version";

    private final StringRedisTemplate redis;

    public StockVersionService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public Long next() {
        try {
            return redis.opsForValue().increment(KEY);
        } catch (RuntimeException e) {
            log.warn("Не удалось увеличить версию склада: {}", e.getMessage());
            return null;
        }
    }
}
