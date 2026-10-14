package com.example.UmirHack_Flagman.inventory;

import java.time.Instant;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.UmirHack_Flagman.auth.AuthInterceptor;
import com.example.UmirHack_Flagman.auth.Roles;
import com.example.UmirHack_Flagman.inventory.dto.AllDeliveryResponse;
import com.example.UmirHack_Flagman.inventory.dto.DeliveryData;
import com.example.UmirHack_Flagman.inventory.dto.DeliveryResponse;
import com.example.UmirHack_Flagman.inventory.dto.StockBatchesResponse;
import com.example.UmirHack_Flagman.inventory.dto.StockResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/")
public class InventoryHandler {
    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final DeliveryService deliveryService;
    private final StockService stockService;

    public InventoryHandler(DeliveryService deliveryService, StockService stockService) {
        this.deliveryService = deliveryService;
        this.stockService = stockService;
    }

    // Поставки

    @PostMapping("deliveries")
    public ResponseEntity<DeliveryResponse> createDelivery(
            @RequestAttribute(AuthInterceptor.ROLE_ATTR) String role,
            @Valid @RequestBody DeliveryData req,
            @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey) {
        Roles.require(role, Roles.STAFF);
        return ResponseEntity.status(HttpStatus.CREATED).body(deliveryService.create(req, idempotencyKey));
    }

    @GetMapping("deliveries")
    public ResponseEntity<AllDeliveryResponse> getDeliveries(
            @RequestAttribute(AuthInterceptor.ROLE_ATTR) String role,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        Roles.require(role, Roles.STAFF);
        return ResponseEntity.ok(deliveryService.getDeliveries(page, limit, search, from, to));
    }

    @GetMapping("deliveries/{deliveryId}")
    public ResponseEntity<DeliveryResponse> getDeliveryById(
            @RequestAttribute(AuthInterceptor.ROLE_ATTR) String role,
            @PathVariable UUID deliveryId) {
        Roles.require(role, Roles.STAFF);
        return ResponseEntity.ok(deliveryService.getById(deliveryId));
    }

    // Склад

    @GetMapping("stock")
    public ResponseEntity<StockResponse> getStock(
            @RequestAttribute(AuthInterceptor.ROLE_ATTR) String role,
            @RequestParam(required = false) UUID ingredientId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        Roles.require(role, Roles.STAFF);
        return ResponseEntity.ok(stockService.getStock(ingredientId, search, page, limit));
    }

    @GetMapping("stock/batches")
    public ResponseEntity<StockBatchesResponse> getStockBatches(
            @RequestAttribute(AuthInterceptor.ROLE_ATTR) String role,
            @RequestParam(required = false) UUID ingredientId,
            @RequestParam(required = false) String expiryStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant expiresBefore,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        Roles.require(role, Roles.STAFF);
        return ResponseEntity.ok(stockService.getBatches(ingredientId, expiryStatus, expiresBefore, page, limit));
    }
}
