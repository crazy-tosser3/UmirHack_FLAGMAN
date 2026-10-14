package com.example.UmirHack_Flagman.menu;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.UmirHack_Flagman.catalog.DishEntity;
import com.example.UmirHack_Flagman.catalog.DishIngredientEntity;
import com.example.UmirHack_Flagman.catalog.DishRepoDB;
import com.example.UmirHack_Flagman.common.ApiException;
import com.example.UmirHack_Flagman.common.Paging;
import com.example.UmirHack_Flagman.events.InventoryEvents;
import com.example.UmirHack_Flagman.inventory.IngredientStockRow;
import com.example.UmirHack_Flagman.inventory.StockBatchRepoDB;
import com.example.UmirHack_Flagman.menu.dto.ManualStopResponse;
import com.example.UmirHack_Flagman.menu.dto.StopListResponse;
import com.example.UmirHack_Flagman.menu.dto.StopReasonCode;

@Service
public class StopListService {
    private final DishRepoDB dishRepo;
    private final StockBatchRepoDB batchRepo;
    private final TransactionTemplate tx;
    private final TransactionTemplate readTx;
    private final ApplicationEventPublisher events;

    public StopListService(DishRepoDB dishRepo, StockBatchRepoDB batchRepo, PlatformTransactionManager txManager,
            ApplicationEventPublisher events) {
        this.dishRepo = dishRepo;
        this.batchRepo = batchRepo;
        this.events = events;
        this.tx = new TransactionTemplate(txManager);
        this.readTx = new TransactionTemplate(txManager);
        this.readTx.setReadOnly(true);
    }

    public record Reason(StopReasonCode code, UUID ingredientId, String ingredientName,
            BigDecimal requiredQuantity, BigDecimal availableQuantity, String unit) {
    }

    public record DishState(DishEntity dish, int availablePortions, List<Reason> reasons) {
        public boolean available() {
            return reasons.isEmpty();
        }
    }

    public List<DishState> computeStates() {
        Instant now = Instant.now();
        Map<UUID, IngredientStockRow> stock = batchRepo.aggregateAll(now).stream()
                .collect(Collectors.toMap(IngredientStockRow::getId, r -> r));

        List<DishState> states = new ArrayList<>();
        for (DishEntity dish : dishRepo.findAllWithIngredients()) {
            if (dish.isActive()) {
                states.add(evaluate(dish, stock));
            }
        }
        return states;
    }

    public StopListResponse getStopList(String reason, int page, int limit) {
        StopReasonCode filter = parseReason(reason);

        return readTx.execute(status -> {
            Instant now = Instant.now();
            List<StopListResponse.Item> items = computeStates().stream()
                    .filter(s -> !s.available())
                    .filter(s -> filter == null || s.reasons().stream().anyMatch(r -> r.code() == filter))
                    .sorted(Comparator.comparing(s -> s.dish().getName()))
                    .map(s -> toItem(s, now))
                    .toList();

            StopListResponse response = new StopListResponse();
            response.setItems(Paging.slice(items, page, limit));
            response.setPage(page);
            response.setLimit(limit);
            response.setTotal(items.size());
            return response;
        });
    }

    // Ручное добавление блюда в стоп-лист (stop = true) или снятие с него (stop = false)
    public ManualStopResponse setManualStop(UUID dishId, boolean stop) {
        boolean changed = Boolean.TRUE.equals(tx.execute(status -> {
            DishEntity dish = dishRepo.findById(dishId).orElseThrow(() ->
                    new ApiException(HttpStatus.NOT_FOUND, "DISH_NOT_FOUND", "Блюдо не найдено"));
            if (dish.isManualStop() == stop) {
                return false;
            }
            dish.setManualStop(stop);
            return true;
        }));
        if (changed) {
            events.publishEvent(new InventoryEvents.ManualStopChanged(dishId));
        }

        return readTx.execute(status -> {
            Map<UUID, IngredientStockRow> stock = batchRepo.aggregateAll(Instant.now()).stream()
                    .collect(Collectors.toMap(IngredientStockRow::getId, r -> r));
            DishEntity dish = dishRepo.findWithIngredients(List.of(dishId)).getFirst();
            DishState state = evaluate(dish, stock);

            ManualStopResponse response = new ManualStopResponse();
            response.setDishId(dish.getId());
            response.setDishName(dish.getName());
            response.setManualStop(dish.isManualStop());
            response.setAvailable(dish.isActive() && state.available());
            response.setReasons(state.reasons().stream().map(StopListService::toReason).toList());
            return response;
        });
    }

    // Возвращает блюда, у которых изменилась доступность
    public synchronized Set<UUID> recompute() {
        return tx.execute(status -> {
            Instant now = Instant.now();
            Set<UUID> changed = new HashSet<>();
            Set<UUID> activeIds = new HashSet<>();
            for (DishState state : computeStates()) {
                DishEntity dish = state.dish();
                activeIds.add(dish.getId());
                if (!state.available() && dish.getStoppedSince() == null) {
                    dish.setStoppedSince(now);
                    changed.add(dish.getId());
                } else if (state.available() && dish.getStoppedSince() != null) {
                    dish.setStoppedSince(null);
                    changed.add(dish.getId());
                }
            }
            for (DishEntity dish : dishRepo.findAll()) {
                if (!activeIds.contains(dish.getId()) && dish.getStoppedSince() != null) {
                    dish.setStoppedSince(null);
                    changed.add(dish.getId());
                }
            }
            return changed;
        });
    }

    private static DishState evaluate(DishEntity dish, Map<UUID, IngredientStockRow> stock) {
        List<Reason> reasons = new ArrayList<>();
        if (dish.isManualStop()) {
            reasons.add(new Reason(StopReasonCode.MANUAL, null, null, null, null, null));
        }

        int portions = Integer.MAX_VALUE;
        for (DishIngredientEntity di : dish.getIngredients()) {
            IngredientStockRow row = stock.get(di.getIngredient().getId());
            BigDecimal usable = row != null ? row.getUsable() : BigDecimal.ZERO;
            BigDecimal expired = row != null ? row.getExpired() : BigDecimal.ZERO;

            portions = Math.min(portions, usable.divide(di.getQuantity(), 0, RoundingMode.FLOOR).intValue());
            if (usable.compareTo(di.getQuantity()) < 0) {
                // Хватило бы, если бы не просрочка
                StopReasonCode code = usable.add(expired).compareTo(di.getQuantity()) >= 0
                        ? StopReasonCode.EXPIRED_INGREDIENT : StopReasonCode.INSUFFICIENT_STOCK;
                reasons.add(new Reason(code, di.getIngredient().getId(), di.getIngredient().getName(),
                        di.getQuantity(), usable, di.getIngredient().getUnit()));
            }
        }
        return new DishState(dish, portions, reasons);
    }

    private static StopListResponse.Item toItem(DishState state, Instant now) {
        StopListResponse.Item item = new StopListResponse.Item();
        item.setDishId(state.dish().getId());
        item.setDishName(state.dish().getName());
        item.setAvailable(false);
        item.setUpdatedAt(state.dish().getStoppedSince() != null ? state.dish().getStoppedSince() : now);
        item.setReasons(state.reasons().stream().map(StopListService::toReason).toList());
        return item;
    }

    private static StopListResponse.Reason toReason(Reason r) {
        StopListResponse.Reason reason = new StopListResponse.Reason();
        reason.setCode(r.code());
        reason.setIngredientId(r.ingredientId());
        reason.setIngredientName(r.ingredientName());
        reason.setRequiredQuantity(r.requiredQuantity());
        reason.setAvailableQuantity(r.availableQuantity());
        reason.setUnit(r.unit());
        return reason;
    }

    private static StopReasonCode parseReason(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return StopReasonCode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw Paging.validation("reason: допустимы insufficient_stock, expired_ingredient, manual");
        }
    }
}
