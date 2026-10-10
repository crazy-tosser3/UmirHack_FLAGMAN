package com.example.UmirHack_Flagman.Services;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.example.UmirHack_Flagman.models.DeliveryCreatedEvent;
import com.example.UmirHack_Flagman.models.OffersUpdatedEvent;
import com.example.UmirHack_Flagman.models.OrderConfirmedEvent;
import com.example.UmirHack_Flagman.models.StockUpdatedEvent;
import com.example.UmirHack_Flagman.models.StopListUpdatedEvent;


@Component
public class InventoryEventListener {
    private static final Logger log = LoggerFactory.getLogger(InventoryEventListener.class);

    private final SseBroadcaster broadcaster;
    private final StopListService stopListService;

    public InventoryEventListener(SseBroadcaster broadcaster, StopListService stopListService) {
        this.broadcaster = broadcaster;
        this.stopListService = stopListService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        stopListService.recompute();
    }

    @Async
    @EventListener
    public void onStockChanged(InventoryEvents.StockChanged event) {
        StockUpdatedEvent stock = new StockUpdatedEvent();
        stock.setStockVersion(event.stockVersion() != null ? event.stockVersion() : 0);
        stock.setIngredientIds(List.copyOf(event.ingredientIds()));
        broadcaster.publish("stock.updated", stock);

        try {
            Set<UUID> changed = stopListService.recompute();
            if (!changed.isEmpty()) {
                StopListUpdatedEvent stopList = new StopListUpdatedEvent();
                stopList.setUpdatedAt(Instant.now());
                stopList.setAffectedDishIds(List.copyOf(changed));
                broadcaster.publish("stop-list.updated", stopList);
            }
        } catch (RuntimeException e) {
            log.error("Ошибка пересчета стоп-листа", e);
        }

        OffersUpdatedEvent offers = new OffersUpdatedEvent();
        offers.setGeneratedAt(Instant.now());
        broadcaster.publish("offers.updated", offers);
    }

    @Async
    @EventListener
    public void onDeliveryCreated(InventoryEvents.DeliveryCreated event) {
        DeliveryCreatedEvent payload = new DeliveryCreatedEvent();
        payload.setDeliveryId(event.deliveryId());
        payload.setDeliveryNumber(event.deliveryNumber());
        payload.setIngredientIds(List.copyOf(event.ingredientIds()));
        broadcaster.publish("delivery.created", payload);
    }

    @Async
    @EventListener
    public void onOrderConfirmed(InventoryEvents.OrderConfirmed event) {
        OrderConfirmedEvent payload = new OrderConfirmedEvent();
        payload.setOrderId(event.orderId());
        payload.setExternalOrderId(event.externalOrderId());
        broadcaster.publish("order.confirmed", payload);
    }

    @Async
    @EventListener
    public void onBatchExpiring(InventoryEvents.BatchExpiring event) {
        broadcaster.publish("batch.expiring", event.payload());
    }

    @Async
    @EventListener
    public void onBatchExpired(InventoryEvents.BatchExpired event) {
        broadcaster.publish("batch.expired", event.payload());
    }
}
