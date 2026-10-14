package com.example.UmirHack_Flagman.orders;

import java.util.UUID;

public interface DishDayRow {
    UUID getDishId();

    String getDishName();

    long getPortions();

    long getOrders();
}
