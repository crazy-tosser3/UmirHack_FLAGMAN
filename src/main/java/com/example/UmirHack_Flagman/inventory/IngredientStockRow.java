package com.example.UmirHack_Flagman.inventory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface IngredientStockRow {
    UUID getId();

    BigDecimal getUsable();

    BigDecimal getExpired();

    Instant getNearestExpiryAt();
}
