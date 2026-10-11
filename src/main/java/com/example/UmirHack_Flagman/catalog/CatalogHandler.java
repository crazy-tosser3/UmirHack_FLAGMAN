package com.example.UmirHack_Flagman.catalog;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.UmirHack_Flagman.auth.AuthInterceptor;
import com.example.UmirHack_Flagman.auth.Roles;
import com.example.UmirHack_Flagman.catalog.dto.CreateDish;
import com.example.UmirHack_Flagman.catalog.dto.DishList;
import com.example.UmirHack_Flagman.catalog.dto.IngredientList;
import com.example.UmirHack_Flagman.catalog.dto.PostIngredientsList;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/")
public class CatalogHandler {
    private final CatalogService catalogService;

    public CatalogHandler(CatalogService catalogService) {
        this.catalogService = catalogService;
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
}
