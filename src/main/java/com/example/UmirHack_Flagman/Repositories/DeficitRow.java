package com.example.UmirHack_Flagman.Repositories;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface DeficitRow {
    UUID getIngredientId();

    String getIngredientName();

    String getUnit();

    long getDeficitCount();

    BigDecimal getMissingQuantity();

    Instant getLastDeficitAt();
}
