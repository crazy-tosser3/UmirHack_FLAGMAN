package com.example.UmirHack_Flagman.Handlers;

import java.time.Instant;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.UmirHack_Flagman.models.ConsuptionData;
import com.example.UmirHack_Flagman.models.CreateOrderData;
import com.example.UmirHack_Flagman.models.DeliveryData;
import com.example.UmirHack_Flagman.models.DeliveryResponse;
import com.example.UmirHack_Flagman.models.DishList;
import com.example.UmirHack_Flagman.models.IngredientList;
import com.example.UmirHack_Flagman.models.OffersResponse;
import com.example.UmirHack_Flagman.models.OrderData;
import com.example.UmirHack_Flagman.models.StockBatchesResponse;
import com.example.UmirHack_Flagman.models.StockResponse;
import com.example.UmirHack_Flagman.models.StopListResponse;

@RestController
@RequestMapping("/api/v1/")
public class InventoryHandler {

    // Ингредиенты

    @GetMapping("ingredients")
    public ResponseEntity<IngredientList> getIngredients(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String search) {
        // TODO каталог ингредиентов
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    // Блюда

    @GetMapping("dishes")
    public ResponseEntity<DishList> getDishes(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active) {
        // TODO блюда и состав
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    // Поставки

    @PostMapping("deliveries")
    public ResponseEntity<DeliveryResponse> createDelivery(
            @RequestBody DeliveryData req,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        // TODO регистрация поставки
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @GetMapping("deliveries/{deliveryId}")
    public ResponseEntity<DeliveryResponse> getDeliveryById(@PathVariable UUID deliveryId) {
        // TODO детали поставки
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    // Склад

    @GetMapping("stock")
    public ResponseEntity<StockResponse> getStock(
            @RequestParam(required = false) UUID ingredientId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        // TODO сводные остатки
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    // expiryStatus: valid | expiring | expired | all
    @GetMapping("stock/batches")
    public ResponseEntity<StockBatchesResponse> getStockBatches(
            @RequestParam(required = false) UUID ingredientId,
            @RequestParam(required = false) String expiryStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant expiresBefore,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        // TODO остатки и сроки по партиям
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    // Заказы

    @PostMapping("orders")
    public ResponseEntity<OrderData> createOrder(
            @RequestBody CreateOrderData req,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        // TODO создание заказа и FEFO-списание
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @GetMapping("orders/{orderId}")
    public ResponseEntity<OrderData> getOrderById(@PathVariable UUID orderId) {
        // TODO детали заказа
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @GetMapping("orders/{orderId}/consumption")
    public ResponseEntity<ConsuptionData> getOrderConsumption(@PathVariable UUID orderId) {
        // TODO списание по партиям
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    // Стоп-лист

    @GetMapping("stop-list")
    public ResponseEntity<StopListResponse> getStopList(
            @RequestParam(required = false) String reason,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    // Горячие предложения

    @GetMapping("offers")
    public ResponseEntity<OffersResponse> getOffers(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Double minPriority) {
        // TODO горячие предложения
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    // SSE

    @GetMapping(value = "events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> events() {
        // TODO подписка на события
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
