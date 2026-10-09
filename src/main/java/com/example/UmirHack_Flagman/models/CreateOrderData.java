package com.example.UmirHack_Flagman.models;

import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Setter 
@Getter 
public class CreateOrderData {
    private String externalOrderId;
    private List<Item> items;

    @Setter 
    @Getter 
    public static class Item {
        private UUID dishId;
        private int quantity;
    }
}
