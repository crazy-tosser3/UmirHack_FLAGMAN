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
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.UmirHack_Flagman.Config.AuthInterceptor;
import com.example.UmirHack_Flagman.Config.Roles;
import com.example.UmirHack_Flagman.Services.CatalogService;
import com.example.UmirHack_Flagman.Services.DeliveryService;
import com.example.UmirHack_Flagman.Services.OffersService;
import com.example.UmirHack_Flagman.Services.OrderService;
import com.example.UmirHack_Flagman.Services.SseBroadcaster;
import com.example.UmirHack_Flagman.Services.StockService;
import com.example.UmirHack_Flagman.Services.StopListService;
import com.example.UmirHack_Flagman.models.ConsuptionData;
import com.example.UmirHack_Flagman.models.CreateOrderData;
import com.example.UmirHack_Flagman.models.DeliveryData;
import com.example.UmirHack_Flagman.models.DeliveryResponse;
import com.example.UmirHack_Flagman.models.CreateDish;
import com.example.UmirHack_Flagman.models.DishList;
import com.example.UmirHack_Flagman.models.IngredientList;
import com.example.UmirHack_Flagman.models.OffersResponse;
import com.example.UmirHack_Flagman.models.OrderData;
import com.example.UmirHack_Flagman.models.StockBatchesResponse;
import com.example.UmirHack_Flagman.models.StockResponse;
import com.example.UmirHack_Flagman.models.StopListResponse;
import com.example.UmirHack_Flagman.models.PostIngredientsList;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/v1/")
public class InventoryHandler {
    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final CatalogService catalogService;
    private final DeliveryService deliveryService;
    private final StockService stockService;
    private final OrderService orderService;
    private final StopListService stopListService;
    private final OffersService offersService;
    private final SseBroadcaster broadcaster;

    public InventoryHandler(CatalogService catalogService, DeliveryService deliveryService, StockService stockService,
            OrderService orderService, StopListService stopListService, OffersService offersService,
            SseBroadcaster broadcaster) {
        this.catalogService = catalogService;
        this.deliveryService = deliveryService;
        this.stockService = stockService;
        this.orderService = orderService;
        this.stopListService = stopListService;
        this.offersService = offersService;
        this.broadcaster = broadcaster;
    }

    // Ингредиенты

    @GetMapping("ingredients")
    public ResponseEntity<IngredientList> getIngredients(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(catalogService.getIngredients(page, limit, search));
    }

    @PostMapping("ingredients")
    public ResponseEntity<IngredientList> createIngredients(
            @RequestAttribute(AuthInterceptor.ROLE_ATTR) String role,
            @Valid @RequestBody PostIngredientsList req) {
        Roles.require(role, Roles.ADMIN);
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogService.createIngredients(req));
    }

    // Блюда

    @GetMapping("dishes")
    public ResponseEntity<DishList> getDishes(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(catalogService.getDishes(page, limit, search, active));
    }

    @PostMapping("dishes")
    public ResponseEntity<DishList> createDishes(
            @RequestAttribute(AuthInterceptor.ROLE_ATTR) String role,
            @Valid @RequestBody CreateDish req) {
        Roles.require(role, Roles.ADMIN);
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogService.createDishes(req));
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

    // Заказы

    @PostMapping("orders")
    public ResponseEntity<OrderData> createOrder(
            @Valid @RequestBody CreateOrderData req,
            @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(req, idempotencyKey));
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

    // Стоп-лист

    @GetMapping("stop-list")
    public ResponseEntity<StopListResponse> getStopList(
            @RequestParam(required = false) String reason,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(stopListService.getStopList(reason, page, limit));
    }

    // Горячие предложения

    @GetMapping("offers")
    public ResponseEntity<OffersResponse> getOffers(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Double minPriority) {
        return ResponseEntity.ok(offersService.getOffers(limit, minPriority));
    }

    // SSE: stock.updated, stop-list.updated, offers.updated, delivery.created, order.confirmed, batch.expiring, batch.expired

    @GetMapping(value = "events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events() {
        return broadcaster.subscribe();
    }
}
