package com.example.UmirHack_Flagman.menu;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.UmirHack_Flagman.auth.AuthInterceptor;
import com.example.UmirHack_Flagman.auth.Roles;
import com.example.UmirHack_Flagman.menu.dto.ManualStopResponse;
import com.example.UmirHack_Flagman.menu.dto.OffersResponse;
import com.example.UmirHack_Flagman.menu.dto.StopListResponse;

@RestController
@RequestMapping("/api/v1/")
public class MenuHandler {
    private final StopListService stopListService;
    private final OffersService offersService;

    public MenuHandler(StopListService stopListService, OffersService offersService) {
        this.stopListService = stopListService;
        this.offersService = offersService;
    }

    @GetMapping("stop-list")
    public ResponseEntity<StopListResponse> getStopList(
            @RequestParam(required = false) String reason,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(stopListService.getStopList(reason, page, limit));
    }

    @PutMapping("stop-list/{dishId}")
    public ResponseEntity<ManualStopResponse> stopDish(
            @RequestAttribute(AuthInterceptor.ROLE_ATTR) String role,
            @PathVariable UUID dishId) {
        Roles.require(role, Roles.STAFF);
        return ResponseEntity.ok(stopListService.setManualStop(dishId, true));
    }

    @DeleteMapping("stop-list/{dishId}")
    public ResponseEntity<ManualStopResponse> resumeDish(
            @RequestAttribute(AuthInterceptor.ROLE_ATTR) String role,
            @PathVariable UUID dishId) {
        Roles.require(role, Roles.STAFF);
        return ResponseEntity.ok(stopListService.setManualStop(dishId, false));
    }

    @GetMapping("offers")
    public ResponseEntity<OffersResponse> getOffers(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Double minPriority) {
        return ResponseEntity.ok(offersService.getOffers(limit, minPriority));
    }
}
