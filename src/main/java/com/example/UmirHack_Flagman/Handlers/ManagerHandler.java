package com.example.UmirHack_Flagman.Handlers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.UmirHack_Flagman.Config.AuthInterceptor;
import com.example.UmirHack_Flagman.Exceptions.ApiException;
import com.example.UmirHack_Flagman.Services.ManageService;
import com.example.UmirHack_Flagman.models.AllDeliveryResponse;
import com.example.UmirHack_Flagman.models.DailyDishesResponse;
import com.example.UmirHack_Flagman.models.DailyIngredientsResponse;
import com.example.UmirHack_Flagman.models.DeficitsResponse;
import com.example.UmirHack_Flagman.models.OrderList;
import com.example.UmirHack_Flagman.models.StockForecastResponse;
import com.example.UmirHack_Flagman.models.SupplySuggestionsResponse;
import com.example.UmirHack_Flagman.models.WriteOffsResponse;

@RestController
@RequestMapping("/api/v1/")
public class ManagerHandler {
    private static final Set<String> ALLOWED_ROLES = Set.of("manager", "admin");

    private final ManageService manageService;

    public ManagerHandler(ManageService manageService) {
        this.manageService = manageService;
    }

    // Поставки

    @GetMapping("deliveries")
    public ResponseEntity<AllDeliveryResponse> getDeliveries(
            @RequestAttribute(AuthInterceptor.ROLE_ATTR) String role,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        requireManager(role);
        return ResponseEntity.ok(manageService.getDeliveries(page, limit, search, from, to));
    }

    // Заказы

    @GetMapping("orders")
    public ResponseEntity<OrderList> getOrders(
            @RequestAttribute(AuthInterceptor.ROLE_ATTR) String role,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String externalOrderId) {
        requireManager(role);
        return ResponseEntity.ok(manageService.getOrders(page, limit, status, from, to, externalOrderId));
    }

    // Аналитика

    @GetMapping("analytics/write-offs")
    public ResponseEntity<WriteOffsResponse> getWriteOffs(
            @RequestAttribute(AuthInterceptor.ROLE_ATTR) String role,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) UUID ingredientId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        requireManager(role);
        return ResponseEntity.ok(manageService.getWriteOffs(from, to, ingredientId, page, limit));
    }

    @GetMapping("analytics/deficits")
    public ResponseEntity<DeficitsResponse> getDeficits(
            @RequestAttribute(AuthInterceptor.ROLE_ATTR) String role,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) UUID ingredientId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        requireManager(role);
        return ResponseEntity.ok(manageService.getDeficits(from, to, ingredientId, page, limit));
    }

    @GetMapping("analytics/stock-forecast")
    public ResponseEntity<StockForecastResponse> getStockForecast(
            @RequestAttribute(AuthInterceptor.ROLE_ATTR) String role,
            @RequestParam(required = false) UUID ingredientId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        requireManager(role);
        return ResponseEntity.ok(manageService.getStockForecast(ingredientId, page, limit));
    }

    @GetMapping("analytics/supply-suggestions")
    public ResponseEntity<SupplySuggestionsResponse> getSupplySuggestions(
            @RequestAttribute(AuthInterceptor.ROLE_ATTR) String role,
            @RequestParam(defaultValue = "3") int coverDays) {
        requireManager(role);
        return ResponseEntity.ok(manageService.getSupplySuggestions(coverDays));
    }

    // Дневная статистика; date в формате 2026-10-11, по умолчанию — сегодня

    @GetMapping("analytics/daily/dishes")
    public ResponseEntity<DailyDishesResponse> getDailyDishes(
            @RequestAttribute(AuthInterceptor.ROLE_ATTR) String role,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        requireManager(role);
        return ResponseEntity.ok(manageService.getDailyDishes(date));
    }

    @GetMapping("analytics/daily/ingredients")
    public ResponseEntity<DailyIngredientsResponse> getDailyIngredients(
            @RequestAttribute(AuthInterceptor.ROLE_ATTR) String role,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        requireManager(role);
        return ResponseEntity.ok(manageService.getDailyIngredients(date));
    }

    private static void requireManager(String role) {
        if (!ALLOWED_ROLES.contains(role)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Недостаточно прав для выполнения операции");
        }
    }
}
