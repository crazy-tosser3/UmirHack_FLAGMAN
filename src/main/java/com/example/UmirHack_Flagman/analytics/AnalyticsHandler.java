package com.example.UmirHack_Flagman.analytics;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.UmirHack_Flagman.analytics.dto.DailyDishesResponse;
import com.example.UmirHack_Flagman.analytics.dto.DailyIngredientsResponse;
import com.example.UmirHack_Flagman.analytics.dto.DeficitsResponse;
import com.example.UmirHack_Flagman.analytics.dto.StockForecastResponse;
import com.example.UmirHack_Flagman.analytics.dto.SupplySuggestionsResponse;
import com.example.UmirHack_Flagman.analytics.dto.WriteOffsResponse;
import com.example.UmirHack_Flagman.auth.AuthInterceptor;
import com.example.UmirHack_Flagman.auth.Roles;

@RestController
@RequestMapping("/api/v1/")
public class AnalyticsHandler {
    private final AnalyticsService analyticsService;

    public AnalyticsHandler(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("analytics/write-offs")
    public ResponseEntity<WriteOffsResponse> getWriteOffs(
            @RequestAttribute(AuthInterceptor.ROLE_ATTR) String role,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) UUID ingredientId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        Roles.require(role, Roles.STAFF);
        return ResponseEntity.ok(analyticsService.getWriteOffs(from, to, ingredientId, page, limit));
    }

    @GetMapping("analytics/deficits")
    public ResponseEntity<DeficitsResponse> getDeficits(
            @RequestAttribute(AuthInterceptor.ROLE_ATTR) String role,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) UUID ingredientId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        Roles.require(role, Roles.STAFF);
        return ResponseEntity.ok(analyticsService.getDeficits(from, to, ingredientId, page, limit));
    }

    @GetMapping("analytics/stock-forecast")
    public ResponseEntity<StockForecastResponse> getStockForecast(
            @RequestAttribute(AuthInterceptor.ROLE_ATTR) String role,
            @RequestParam(required = false) UUID ingredientId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        Roles.require(role, Roles.STAFF);
        return ResponseEntity.ok(analyticsService.getStockForecast(ingredientId, page, limit));
    }

    @GetMapping("analytics/supply-suggestions")
    public ResponseEntity<SupplySuggestionsResponse> getSupplySuggestions(
            @RequestAttribute(AuthInterceptor.ROLE_ATTR) String role,
            @RequestParam(defaultValue = "3") int coverDays) {
        Roles.require(role, Roles.STAFF);
        return ResponseEntity.ok(analyticsService.getSupplySuggestions(coverDays));
    }


    @GetMapping("analytics/daily/dishes")
    public ResponseEntity<DailyDishesResponse> getDailyDishes(
            @RequestAttribute(AuthInterceptor.ROLE_ATTR) String role,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Roles.require(role, Roles.STAFF);
        return ResponseEntity.ok(analyticsService.getDailyDishes(date));
    }

    @GetMapping("analytics/daily/ingredients")
    public ResponseEntity<DailyIngredientsResponse> getDailyIngredients(
            @RequestAttribute(AuthInterceptor.ROLE_ATTR) String role,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Roles.require(role, Roles.STAFF);
        return ResponseEntity.ok(analyticsService.getDailyIngredients(date));
    }
}
