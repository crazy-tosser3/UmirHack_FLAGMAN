package com.example.UmirHack_Flagman.menu;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("offers")
    public ResponseEntity<OffersResponse> getOffers(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Double minPriority) {
        return ResponseEntity.ok(offersService.getOffers(limit, minPriority));
    }
}
