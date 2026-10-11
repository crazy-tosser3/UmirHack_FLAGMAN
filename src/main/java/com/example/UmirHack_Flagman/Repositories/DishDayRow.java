package com.example.UmirHack_Flagman.Repositories;

import java.util.UUID;

public interface DishDayRow {
    UUID getDishId();

    String getDishName();

    long getPortions();

    long getOrders();
}
