package com.example.UmirHack_Flagman.events;

import java.util.Collection;
import java.util.UUID;

import com.example.UmirHack_Flagman.events.dto.BatchExpiryEvent;

// Внутренние события приложения. Публикуются только после коммита транзакции
public final class InventoryEvents {
    private InventoryEvents() {
    }

    public record StockChanged(Collection<UUID> ingredientIds, Long stockVersion) {
    }

    public record DeliveryCreated(UUID deliveryId, String deliveryNumber, Collection<UUID> ingredientIds) {
    }

    public record OrderConfirmed(UUID orderId, String externalOrderId) {
    }

    public record ManualStopChanged(UUID dishId) {
    }

    public record BatchExpiring(BatchExpiryEvent payload) {
    }

    public record BatchExpired(BatchExpiryEvent payload) {
    }
}
