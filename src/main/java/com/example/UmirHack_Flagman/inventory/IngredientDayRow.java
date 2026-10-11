package com.example.UmirHack_Flagman.inventory;

import java.math.BigDecimal;
import java.util.UUID;

public interface IngredientDayRow {
    UUID getIngredientId();

    String getIngredientName();

    String getUnit();

    BigDecimal getConsumed();

    BigDecimal getWrittenOff();
}
