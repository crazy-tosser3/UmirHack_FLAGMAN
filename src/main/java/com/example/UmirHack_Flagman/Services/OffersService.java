package com.example.UmirHack_Flagman.Services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.UmirHack_Flagman.Config.InventoryProperties;
import com.example.UmirHack_Flagman.Entity.DishIngredientEntity;
import com.example.UmirHack_Flagman.Entity.StockBatchEntity;
import com.example.UmirHack_Flagman.Repositories.StockBatchRepoDB;
import com.example.UmirHack_Flagman.models.OffersResponse;

@Service
@Transactional(readOnly = true)
public class OffersService {
    private final StopListService stopListService;
    private final StockBatchRepoDB batchRepo;
    private final ConsumptionStats consumptionStats;
    private final InventoryProperties props;

    public OffersService(StopListService stopListService, StockBatchRepoDB batchRepo,
            ConsumptionStats consumptionStats, InventoryProperties props) {
        this.stopListService = stopListService;
        this.batchRepo = batchRepo;
        this.consumptionStats = consumptionStats;
        this.props = props;
    }

    private record Scored(double score, OffersResponse.Reason reason) {
    }

    public OffersResponse getOffers(Integer limit, Double minPriority) {
        int max = limit != null ? limit : props.offersDefaultLimit();
        double threshold = minPriority != null ? minPriority : 0;
        if (max < 1 || max > Paging.MAX_LIMIT) {
            throw Paging.validation("limit — от 1 до " + Paging.MAX_LIMIT);
        }
        if (threshold < 0 || threshold > 100) {
            throw Paging.validation("minPriority — от 0 до 100");
        }

        Instant now = Instant.now();
        double horizonHours = props.offersHorizonHours();
        Instant horizonEnd = now.plus(Duration.ofHours(props.offersHorizonHours()));

        List<StopListService.DishState> states = stopListService.computeStates();
        Map<UUID, Double> rates = consumptionStats.ratesPerSecond(now);
        Map<UUID, List<StockBatchEntity>> atRisk = batchRepo.findUsable(now).stream()
                .filter(b -> !b.getExpiresAt().isAfter(horizonEnd))
                .collect(Collectors.groupingBy(b -> b.getIngredient().getId()));

        Map<UUID, Integer> dishesPerIngredient = new HashMap<>();
        for (StopListService.DishState s : states) {
            s.dish().getIngredients().forEach(di -> dishesPerIngredient.merge(di.getIngredient().getId(), 1, Integer::sum));
        }

        List<OffersResponse.Item> items = new ArrayList<>();
        for (StopListService.DishState state : states) {
            if (!state.available() || state.dish().getIngredients().isEmpty()) {
                continue;
            }

            List<Scored> scored = new ArrayList<>();
            for (DishIngredientEntity di : state.dish().getIngredients()) {
                UUID id = di.getIngredient().getId();
                List<StockBatchEntity> batches = atRisk.get(id);
                if (batches == null) {
                    continue;
                }

                StockBatchEntity earliest = batches.getFirst();
                double hoursLeft = Math.max(0, Duration.between(now, earliest.getExpiresAt()).toSeconds() / 3600.0);
                double quantity = batches.stream().mapToDouble(b -> b.getRemainingQuantity().doubleValue()).sum();
                double ratePerHour = rates.getOrDefault(id, 0.0) * 3600;

                double urgency = clamp(1 - hoursLeft / horizonHours);
                double leftover = Math.max(0, quantity - ratePerHour * hoursLeft);
                double wasteShare = quantity > 0 ? leftover / quantity : 0;
                int dishCount = dishesPerIngredient.getOrDefault(id, 1);
                double exclusivity = 1.0 / dishCount;

                double score = urgency * (0.4 + 0.6 * wasteShare) * (0.75 + 0.25 * exclusivity);
                if (score <= 0) {
                    continue;
                }

                OffersResponse.Reason reason = new OffersResponse.Reason();
                reason.setIngredientId(id);
                reason.setIngredientName(di.getIngredient().getName());
                reason.setExpiresAt(earliest.getExpiresAt());
                reason.setAvailableQuantity(BigDecimal.valueOf(quantity).setScale(3, RoundingMode.HALF_UP));
                reason.setUnit(di.getIngredient().getUnit());
                reason.setExplanation(explain(hoursLeft, ratePerHour, leftover, di.getIngredient().getUnit(), dishCount));
                scored.add(new Scored(score, reason));
            }
            if (scored.isEmpty()) {
                continue;
            }

            scored.sort(Comparator.comparingDouble(Scored::score).reversed());
            double priority = Math.round(scored.getFirst().score() * 1000) / 10.0;
            if (priority <= 0 || priority < threshold) {
                continue;
            }

            OffersResponse.Item item = new OffersResponse.Item();
            item.setDishId(state.dish().getId());
            item.setDishName(state.dish().getName());
            item.setPriority(priority);
            item.setAvailablePortions(state.availablePortions());
            item.setReasons(scored.stream().map(Scored::reason).toList());
            item.setCalculatedAt(now);
            items.add(item);
        }

        items.sort(Comparator.comparingDouble(OffersResponse.Item::getPriority).reversed()
                .thenComparing(OffersResponse.Item::getDishName));

        OffersResponse response = new OffersResponse();
        response.setItems(items.subList(0, Math.min(max, items.size())));
        response.setGeneratedAt(now);
        return response;
    }

    private static String explain(double hoursLeft, double ratePerHour, double leftover, String unit, int dishCount) {
        StringBuilder sb = new StringBuilder(hoursLeft < 1
                ? "Срок годности партии истекает менее чем через час"
                : "Срок годности партии истекает через " + Math.round(hoursLeft) + " ч");
        if (ratePerHour <= 0) {
            sb.append("; ингредиент сейчас не расходуется");
        } else if (leftover > 0) {
            sb.append("; при текущем расходе к этому сроку останется около ")
                    .append(Math.round(leftover)).append(' ').append(unit);
        } else {
            sb.append("; при текущем расходе партия будет израсходована вовремя");
        }
        if (dishCount == 1) {
            sb.append("; используется только в этом блюде");
        }
        return sb.toString();
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }
}
