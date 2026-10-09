package com.example.UmirHack_Flagman.Repositories;

import java.math.BigDecimal;
import java.util.UUID;

public interface WriteOffRow {
    UUID getIngredientId();

    String getIngredientName();

    String getUnit();

    BigDecimal getQuantity();

    long getBatchesCount();
}
