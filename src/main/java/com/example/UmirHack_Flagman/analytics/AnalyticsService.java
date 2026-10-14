package com.example.UmirHack_Flagman.analytics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.UmirHack_Flagman.analytics.dto.DailyDishesResponse;
import com.example.UmirHack_Flagman.analytics.dto.DailyIngredientsResponse;
import com.example.UmirHack_Flagman.analytics.dto.DeficitsResponse;
import com.example.UmirHack_Flagman.analytics.dto.StockForecastResponse;
import com.example.UmirHack_Flagman.analytics.dto.SupplySuggestionsResponse;
import com.example.UmirHack_Flagman.analytics.dto.WriteOffsResponse;
import com.example.UmirHack_Flagman.catalog.DishEntity;
import com.example.UmirHack_Flagman.catalog.DishIngredientEntity;
import com.example.UmirHack_Flagman.catalog.DishRepoDB;
import com.example.UmirHack_Flagman.common.Paging;
import com.example.UmirHack_Flagman.inventory.ConsumptionStats;
import com.example.UmirHack_Flagman.inventory.MovementType;
import com.example.UmirHack_Flagman.inventory.StockBatchEntity;
import com.example.UmirHack_Flagman.inventory.StockBatchRepoDB;
import com.example.UmirHack_Flagman.inventory.StockMovementRepoDB;
import com.example.UmirHack_Flagman.inventory.WriteOffRow;
import com.example.UmirHack_Flagman.orders.CustomerOrderRepoDB;
import com.example.UmirHack_Flagman.orders.DeficitRow;
import com.example.UmirHack_Flagman.orders.DishDayRow;
import com.example.UmirHack_Flagman.orders.OrderItemRepoDB;
import com.example.UmirHack_Flagman.orders.OrderStatus;
import com.example.UmirHack_Flagman.orders.StockDeficitRepoDB;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {
    private static final Duration DEFAULT_PERIOD = Duration.ofDays(30);
    private static final Duration FORECAST_HORIZON = Duration.ofDays(30);

    private final StockBatchRepoDB batchRepo;
    private final CustomerOrderRepoDB orderRepo;
    private final OrderItemRepoDB orderItemRepo;
    private final StockMovementRepoDB movementRepo;
    private final StockDeficitRepoDB deficitRepo;
    private final DishRepoDB dishRepo;
    private final ConsumptionStats consumptionStats;
    private final ZoneId zone;

    public AnalyticsService(StockBatchRepoDB batchRepo, CustomerOrderRepoDB orderRepo,
            OrderItemRepoDB orderItemRepo, StockMovementRepoDB movementRepo, StockDeficitRepoDB deficitRepo,
            DishRepoDB dishRepo, ConsumptionStats consumptionStats,
            @Value("${app.analytics.zone:Europe/Moscow}") ZoneId zone) {
        this.zone = zone;
        this.batchRepo = batchRepo;
        this.orderRepo = orderRepo;
        this.orderItemRepo = orderItemRepo;
        this.movementRepo = movementRepo;
        this.deficitRepo = deficitRepo;
        this.dishRepo = dishRepo;
        this.consumptionStats = consumptionStats;
    }

    // Дневная статистика. День считается в часовом поясе ресторана (app.analytics.zone)

    public DailyDishesResponse getDailyDishes(LocalDate date) {
        DayRange day = dayRange(date);
        List<DishDayRow> rows = orderItemRepo.salesByDish(day.from(), day.to(), OrderStatus.CONFIRMED);

        DailyDishesResponse response = new DailyDishesResponse();
        response.setDate(day.date());
        response.setFrom(day.from());
        response.setTo(day.to());
        response.setItems(rows.stream().map(r -> {
            DailyDishesResponse.Item item = new DailyDishesResponse.Item();
            item.setDishId(r.getDishId());
            item.setDishName(r.getDishName());
            item.setPortions(r.getPortions());
            item.setOrders(r.getOrders());
            return item;
        }).toList());
        response.setTotalPortions(rows.stream().mapToLong(DishDayRow::getPortions).sum());
        response.setTotalOrders(orderRepo.countByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                OrderStatus.CONFIRMED, day.from(), day.to()));
        return response;
    }

    public DailyIngredientsResponse getDailyIngredients(LocalDate date) {
        DayRange day = dayRange(date);

        DailyIngredientsResponse response = new DailyIngredientsResponse();
        response.setDate(day.date());
        response.setFrom(day.from());
        response.setTo(day.to());
        response.setItems(movementRepo.usageByIngredient(day.from(), day.to(),
                MovementType.ORDER_CONSUMPTION, MovementType.EXPIRED_WRITE_OFF).stream().map(r -> {
            DailyIngredientsResponse.Item item = new DailyIngredientsResponse.Item();
            item.setIngredientId(r.getIngredientId());
            item.setIngredientName(r.getIngredientName());
            item.setUnit(r.getUnit());
            item.setConsumedQuantity(r.getConsumed());
            item.setWrittenOffQuantity(r.getWrittenOff());
            item.setTotalQuantity(r.getConsumed().add(r.getWrittenOff()));
            return item;
        }).toList());
        return response;
    }

    private record DayRange(LocalDate date, Instant from, Instant to) {
    }

    private DayRange dayRange(LocalDate date) {
        LocalDate day = date != null ? date : LocalDate.now(zone);
        return new DayRange(day, day.atStartOfDay(zone).toInstant(), day.plusDays(1).atStartOfDay(zone).toInstant());
    }

    public WriteOffsResponse getWriteOffs(Instant from, Instant to, UUID ingredientId, int page, int limit) {
        Instant end = to != null ? to : Instant.now();
        Instant start = from != null ? from : end.minus(DEFAULT_PERIOD);
        Paging.checkRange(start, end);

        Page<WriteOffRow> rows = movementRepo.sumByIngredient(
                MovementType.EXPIRED_WRITE_OFF, start, end, ingredientId, Paging.pageable(page, limit, Sort.unsorted()));

        WriteOffsResponse response = new WriteOffsResponse();
        response.setFrom(start);
        response.setTo(end);
        response.setItems(rows.map(r -> {
            WriteOffsResponse.Item item = new WriteOffsResponse.Item();
            item.setIngredientId(r.getIngredientId());
            item.setIngredientName(r.getIngredientName());
            item.setUnit(r.getUnit());
            item.setQuantity(r.getQuantity());
            item.setBatchesCount(r.getBatchesCount());
            return item;
        }).getContent());
        response.setPage(page);
        response.setLimit(limit);
        response.setTotal(rows.getTotalElements());
        return response;
    }

    public DeficitsResponse getDeficits(Instant from, Instant to, UUID ingredientId, int page, int limit) {
        Instant end = to != null ? to : Instant.now();
        Instant start = from != null ? from : end.minus(DEFAULT_PERIOD);
        Paging.checkRange(start, end);

        Page<DeficitRow> rows = deficitRepo.aggregateByIngredient(
                start, end, ingredientId, Paging.pageable(page, limit, Sort.unsorted()));

        DeficitsResponse response = new DeficitsResponse();
        response.setFrom(start);
        response.setTo(end);
        response.setItems(rows.map(r -> {
            DeficitsResponse.Item item = new DeficitsResponse.Item();
            item.setIngredientId(r.getIngredientId());
            item.setIngredientName(r.getIngredientName());
            item.setUnit(r.getUnit());
            item.setDeficitCount(r.getDeficitCount());
            item.setMissingQuantity(r.getMissingQuantity());
            item.setLastDeficitAt(r.getLastDeficitAt());
            return item;
        }).getContent());
        response.setPage(page);
        response.setLimit(limit);
        response.setTotal(rows.getTotalElements());
        return response;
    }

    public StockForecastResponse getStockForecast(UUID ingredientId, int page, int limit) {
        Pageable pageable = Paging.pageable(page, limit, Sort.unsorted());
        Instant now = Instant.now();

        Map<UUID, List<StockBatchEntity>> batchesByIngredient = batchRepo.findUsable(now).stream()
                .collect(Collectors.groupingBy(b -> b.getIngredient().getId()));
        Map<UUID, Double> ratePerSecond = consumptionStats.ratesPerSecond(now);

        List<StockForecastResponse.Item> items = new ArrayList<>();
        for (DishEntity dish : dishRepo.findAllWithIngredients()) {
            if (!dish.isActive() || dish.isManualStop() || dish.getIngredients().isEmpty()) {
                continue;
            }
            if (ingredientId != null && dish.getIngredients().stream()
                    .noneMatch(di -> di.getIngredient().getId().equals(ingredientId))) {
                continue;
            }
            items.add(forecastDish(dish, batchesByIngredient, ratePerSecond, now));
        }

        items.sort(Comparator.comparing(StockForecastResponse.Item::getStopAt,
                Comparator.nullsLast(Comparator.naturalOrder())));

        int fromIndex = (int) Math.min(pageable.getOffset(), items.size());
        int toIndex = Math.min(fromIndex + limit, items.size());

        StockForecastResponse response = new StockForecastResponse();
        response.setGeneratedAt(now);
        response.setHorizonDays((int) FORECAST_HORIZON.toDays());
        response.setConsumptionWindowDays((int) ConsumptionStats.WINDOW.toDays());
        response.setItems(items.subList(fromIndex, toIndex));
        response.setPage(page);
        response.setLimit(limit);
        response.setTotal(items.size());
        return response;
    }

    private StockForecastResponse.Item forecastDish(DishEntity dish, Map<UUID, List<StockBatchEntity>> batchesByIngredient,
            Map<UUID, Double> ratePerSecond, Instant now) {
        Depletion earliest = null;
        DishIngredientEntity limiting = null;
        int portions = Integer.MAX_VALUE;
        DishIngredientEntity scarcest = null;

        for (DishIngredientEntity di : dish.getIngredients()) {
            UUID id = di.getIngredient().getId();
            List<StockBatchEntity> batches = batchesByIngredient.getOrDefault(id, List.of());

            int ingredientPortions = available(batches).divide(di.getQuantity(), 0, RoundingMode.FLOOR).intValue();
            if (ingredientPortions < portions) {
                portions = ingredientPortions;
                scarcest = di;
            }

            Depletion d = depletion(batches, di.getQuantity().doubleValue(), ratePerSecond.getOrDefault(id, 0.0), now);
            if (d != null && (earliest == null || d.at().isBefore(earliest.at()))) {
                earliest = d;
                limiting = di;
            }
        }

        DishIngredientEntity shown = limiting != null ? limiting : scarcest;
        UUID shownId = shown.getIngredient().getId();

        StockForecastResponse.LimitingIngredient li = new StockForecastResponse.LimitingIngredient();
        li.setIngredientId(shownId);
        li.setIngredientName(shown.getIngredient().getName());
        li.setUnit(shown.getIngredient().getUnit());
        li.setAvailableQuantity(available(batchesByIngredient.getOrDefault(shownId, List.of())));
        li.setRequiredPerPortion(shown.getQuantity());
        li.setDailyConsumption(BigDecimal.valueOf(ratePerSecond.getOrDefault(shownId, 0.0) * 86_400)
                .setScale(3, RoundingMode.HALF_UP));

        StockForecastResponse.Item item = new StockForecastResponse.Item();
        item.setDishId(dish.getId());
        item.setDishName(dish.getName());
        item.setAvailablePortions(portions);
        item.setStopAt(earliest != null ? earliest.at() : null);
        item.setCause(earliest != null ? earliest.cause() : null);
        item.setLimitingIngredient(li);
        return item;
    }

    private record Depletion(Instant at, String cause) {
    }

    private static Depletion depletion(List<StockBatchEntity> batches, double threshold, double rate, Instant now) {
        int n = batches.size();
        double[] qty = new double[n];
        double[] expiresIn = new double[n];
        double total = 0;
        for (int i = 0; i < n; i++) {
            qty[i] = batches.get(i).getRemainingQuantity().doubleValue();
            expiresIn[i] = Duration.between(now, batches.get(i).getExpiresAt()).toMillis() / 1000.0;
            total += qty[i];
        }
        if (total < threshold) {
            return new Depletion(now, "already_stopped");
        }

        double horizon = FORECAST_HORIZON.toSeconds();
        double time = 0;
        int head = 0;
        while (time <= horizon) {
            double nextExpiry = head < n ? expiresIn[head] : Double.POSITIVE_INFINITY;
            double cross = rate > 0 ? time + (total - threshold) / rate : Double.POSITIVE_INFINITY;

            if (cross <= nextExpiry) {
                return cross <= horizon ? new Depletion(at(now, cross), "consumption") : null;
            }

            double toConsume = rate * (nextExpiry - time);
            total -= toConsume;
            for (int j = head; j < n && toConsume > 0; j++) {
                double take = Math.min(qty[j], toConsume);
                qty[j] -= take;
                toConsume -= take;
            }
            total -= qty[head];
            qty[head] = 0;
            head++;
            time = nextExpiry;

            if (total < threshold) {
                return time <= horizon ? new Depletion(at(now, time), "expiry") : null;
            }
        }
        return null;
    }

    /*
     * Предложение поставок: сколько докупить, чтобы следующие coverDays дней покрыть средний расход
     * и не опуститься ниже нормы на порцию самого «тяжелого» блюда с этим ингредиентом.
     * Учитывается, что часть текущих партий сгорит по сроку до того, как будет израсходована.
     */
    public SupplySuggestionsResponse getSupplySuggestions(int coverDays) {
        if (coverDays < 1 || coverDays > 30) {
            throw Paging.validation("coverDays — от 1 до 30");
        }
        Instant now = Instant.now();
        double coverSeconds = Duration.ofDays(coverDays).toSeconds();

        Map<UUID, List<StockBatchEntity>> batchesByIngredient = batchRepo.findUsable(now).stream()
                .collect(Collectors.groupingBy(b -> b.getIngredient().getId()));
        Map<UUID, Double> ratePerSecond = consumptionStats.ratesPerSecond(now);

        Map<UUID, DishIngredientEntity> heaviest = new LinkedHashMap<>();
        for (DishEntity dish : dishRepo.findAllWithIngredients()) {
            if (!dish.isActive()) {
                continue;
            }
            for (DishIngredientEntity di : dish.getIngredients()) {
                heaviest.merge(di.getIngredient().getId(), di,
                        (a, b) -> a.getQuantity().compareTo(b.getQuantity()) >= 0 ? a : b);
            }
        }

        List<SupplySuggestionsResponse.Item> items = new ArrayList<>();
        for (DishIngredientEntity di : heaviest.values()) {
            UUID id = di.getIngredient().getId();
            List<StockBatchEntity> batches = batchesByIngredient.getOrDefault(id, List.of());
            double rate = ratePerSecond.getOrDefault(id, 0.0);
            double threshold = di.getQuantity().doubleValue();

            Simulation sim = simulate(batches, rate, coverSeconds, now);
            double shortfall = (rate * coverSeconds - sim.consumed()) + Math.max(0, threshold - sim.remaining());
            if (shortfall <= 0) {
                continue;
            }

            Depletion depletion = depletion(batches, threshold, rate, now);
            SupplySuggestionsResponse.Item item = new SupplySuggestionsResponse.Item();
            item.setIngredientId(id);
            item.setIngredientName(di.getIngredient().getName());
            item.setUnit(di.getIngredient().getUnit());
            item.setAvailableQuantity(available(batches));
            item.setDailyConsumption(BigDecimal.valueOf(rate * 86_400).setScale(3, RoundingMode.HALF_UP));
            item.setDepletionAt(depletion != null ? depletion.at() : null);
            item.setSuggestedQuantity(BigDecimal.valueOf(shortfall).setScale(0, RoundingMode.CEILING));
            items.add(item);
        }

        items.sort(Comparator.comparing(SupplySuggestionsResponse.Item::getDepletionAt,
                Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(SupplySuggestionsResponse.Item::getIngredientName));

        SupplySuggestionsResponse response = new SupplySuggestionsResponse();
        response.setGeneratedAt(now);
        response.setCoverDays(coverDays);
        response.setItems(items);
        return response;
    }

    private record Simulation(double consumed, double remaining) {
    }

    // FEFO-расход с постоянной скоростью на отрезке [now, now + seconds]; остаток истекших партий сгорает
    private static Simulation simulate(List<StockBatchEntity> batches, double rate, double seconds, Instant now) {
        int n = batches.size();
        double[] qty = new double[n];
        double[] expiresIn = new double[n];
        for (int i = 0; i < n; i++) {
            qty[i] = batches.get(i).getRemainingQuantity().doubleValue();
            expiresIn[i] = Duration.between(now, batches.get(i).getExpiresAt()).toMillis() / 1000.0;
        }

        double time = 0;
        double consumed = 0;
        int head = 0;
        while (time < seconds) {
            double next = Math.min(head < n ? expiresIn[head] : Double.POSITIVE_INFINITY, seconds);
            double want = rate * (next - time);
            for (int j = head; j < n && want > 0; j++) {
                double take = Math.min(qty[j], want);
                qty[j] -= take;
                want -= take;
                consumed += take;
            }
            time = next;
            while (head < n && expiresIn[head] <= time && time < seconds) {
                qty[head++] = 0;
            }
        }

        double remaining = 0;
        for (int i = head; i < n; i++) {
            if (expiresIn[i] > seconds) {
                remaining += qty[i];
            }
        }
        return new Simulation(consumed, remaining);
    }

    // Общие помощники

    private static BigDecimal available(List<StockBatchEntity> batches) {
        return batches.stream().map(StockBatchEntity::getRemainingQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static Instant at(Instant now, double seconds) {
        return now.plusMillis((long) (seconds * 1000));
    }
}
