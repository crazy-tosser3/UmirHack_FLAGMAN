package com.example.UmirHack_Flagman.Handlers;

import java.time.Instant;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.UmirHack_Flagman.models.AllDeliveryResponse;
import com.example.UmirHack_Flagman.models.OrderList;

@RestController
@RequestMapping("/api/v1/")
public class ManagerHandler {

    // Поставки

    @GetMapping("deliveries")
    public ResponseEntity<AllDeliveryResponse> getDeliveries(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        // TODO список поставок
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    // Заказы

    @GetMapping("orders")
    public ResponseEntity<OrderList> getOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String externalOrderId) {
        // TODO список заказов
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    // Аналитика

    @GetMapping("analytics/write-offs")
    public ResponseEntity<Object> getWriteOffs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) UUID ingredientId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        // TODO аналитика списаний
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @GetMapping("analytics/deficits")
    public ResponseEntity<Object> getDeficits(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) UUID ingredientId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        // TODO аналитика дефицита
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @GetMapping("analytics/stock-forecast")
    public ResponseEntity<Object> getStockForecast(
            @RequestParam(required = false) UUID ingredientId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        // TODO прогноз стоп-листа
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
