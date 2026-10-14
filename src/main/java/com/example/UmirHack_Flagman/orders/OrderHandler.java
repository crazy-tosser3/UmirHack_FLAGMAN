package com.example.UmirHack_Flagman.orders;

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
import com.example.UmirHack_Flagman.orders.dto.ConsuptionData;
import com.example.UmirHack_Flagman.orders.dto.CreateOrderData;
import com.example.UmirHack_Flagman.orders.dto.OrderData;
import com.example.UmirHack_Flagman.orders.dto.OrderList;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/")
public class OrderHandler {
    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final OrderService orderService;

    public OrderHandler(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("orders")
    public ResponseEntity<OrderData> createOrder(
            @Valid @RequestBody CreateOrderData req,
            @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(req, idempotencyKey));
    }

    @GetMapping("orders")
    public ResponseEntity<OrderList> getOrders(
            @RequestAttribute(AuthInterceptor.ROLE_ATTR) String role,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String externalOrderId) {
        Roles.require(role, Roles.STAFF);
        return ResponseEntity.ok(orderService.getOrders(page, limit, status, from, to, externalOrderId));
    }

    @GetMapping("orders/{orderId}")
    public ResponseEntity<OrderData> getOrderById(@PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.getById(orderId));
    }

    @GetMapping("orders/{orderId}/consumption")
    public ResponseEntity<ConsuptionData> getOrderConsumption(
            @RequestAttribute(AuthInterceptor.ROLE_ATTR) String role,
            @PathVariable UUID orderId) {
        Roles.require(role, Roles.STAFF);
        return ResponseEntity.ok(orderService.getConsumption(orderId));
    }
}
