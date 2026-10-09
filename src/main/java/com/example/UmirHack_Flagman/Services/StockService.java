package com.example.UmirHack_Flagman.Services;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.UmirHack_Flagman.Config.InventoryProperties;
import com.example.UmirHack_Flagman.Entity.IngredientEntity;
import com.example.UmirHack_Flagman.Entity.StockBatchEntity;
import com.example.UmirHack_Flagman.Repositories.IngredientRepoDB;
import com.example.UmirHack_Flagman.Repositories.IngredientStockRow;
import com.example.UmirHack_Flagman.Repositories.StockBatchRepoDB;
import com.example.UmirHack_Flagman.models.BatchStatus;
import com.example.UmirHack_Flagman.models.StockBatchesResponse;
import com.example.UmirHack_Flagman.models.StockResponse;

import jakarta.persistence.criteria.Predicate;

@Service
@Transactional(readOnly = true)
public class StockService {
    private final IngredientRepoDB ingredientRepo;
    private final StockBatchRepoDB batchRepo;
    private final InventoryProperties props;

    public StockService(IngredientRepoDB ingredientRepo, StockBatchRepoDB batchRepo, InventoryProperties props) {
        this.ingredientRepo = ingredientRepo;
        this.batchRepo = batchRepo;
        this.props = props;
    }

    public StockResponse getStock(UUID ingredientId, String search, int page, int limit) {
        Instant now = Instant.now();
        Specification<IngredientEntity> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (ingredientId != null) {
                p.add(cb.equal(root.get("id"), ingredientId));
            }
            if (search != null && !search.isBlank()) {
                p.add(cb.like(cb.lower(root.get("name")), Paging.likePattern(search), '\\'));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };

        Page<IngredientEntity> result = ingredientRepo.findAll(spec, Paging.pageable(page, limit, Sort.by("name")));
        Map<UUID, IngredientStockRow> stock = batchRepo
                .aggregateFor(now, result.getContent().stream().map(IngredientEntity::getId).toList())
                .stream().collect(Collectors.toMap(IngredientStockRow::getId, Function.identity()));

        StockResponse response = new StockResponse();
        response.setItems(result.map(i -> {
            IngredientStockRow row = stock.get(i.getId());
            BigDecimal usable = row != null ? row.getUsable() : BigDecimal.ZERO;
            BigDecimal expired = row != null ? row.getExpired() : BigDecimal.ZERO;

            StockResponse.Item item = new StockResponse.Item();
            item.setIngredientId(i.getId());
            item.setName(i.getName());
            item.setUnit(i.getUnit());
            item.setTotalQuantity(usable.add(expired));
            item.setAvailableQuantity(usable);
            item.setReservedQuantity(BigDecimal.ZERO);
            item.setExpiredQuantity(expired);
            item.setNearestExpiryAt(row != null ? row.getNearestExpiryAt() : null);
            return item;
        }).getContent());
        response.setPage(page);
        response.setLimit(limit);
        response.setTotal(result.getTotalElements());
        return response;
    }

    public StockBatchesResponse getBatches(UUID ingredientId, String expiryStatus, Instant expiresBefore, int page, int limit) {
        Instant now = Instant.now();
        Instant warningUntil = now.plus(Duration.ofHours(props.expiryWarningHours()));
        BatchStatus status = parseStatus(expiryStatus);

        Specification<StockBatchEntity> spec = (root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("ingredient");
            }
            List<Predicate> p = new ArrayList<>();
            p.add(cb.greaterThan(root.get("remainingQuantity"), BigDecimal.ZERO));
            if (ingredientId != null) {
                p.add(cb.equal(root.get("ingredient").get("id"), ingredientId));
            }
            if (expiresBefore != null) {
                p.add(cb.lessThan(root.get("expiresAt"), expiresBefore));
            }
            if (status == BatchStatus.EXPIRED) {
                p.add(cb.lessThanOrEqualTo(root.get("expiresAt"), now));
            } else if (status == BatchStatus.EXPIRING) {
                p.add(cb.greaterThan(root.get("expiresAt"), now));
                p.add(cb.lessThanOrEqualTo(root.get("expiresAt"), warningUntil));
            } else if (status == BatchStatus.VALID) {
                p.add(cb.greaterThan(root.get("expiresAt"), warningUntil));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };

        Page<StockBatchEntity> result = batchRepo.findAll(spec,
                Paging.pageable(page, limit, Sort.by("expiresAt").and(Sort.by("id"))));

        StockBatchesResponse response = new StockBatchesResponse();
        response.setItems(result.map(b -> {
            boolean expired = !b.getExpiresAt().isAfter(now);

            StockBatchesResponse.Item item = new StockBatchesResponse.Item();
            item.setBatchId(b.getId());
            item.setDeliveryId(b.getDelivery().getId());
            item.setIngredientId(b.getIngredient().getId());
            item.setIngredientName(b.getIngredient().getName());
            item.setReceivedQuantity(b.getReceivedQuantity());
            item.setRemainingQuantity(b.getRemainingQuantity());
            item.setReservedQuantity(BigDecimal.ZERO);
            item.setAvailableQuantity(expired ? BigDecimal.ZERO : b.getRemainingQuantity());
            item.setExpiresAt(b.getExpiresAt());
            item.setExpiryStatus(expired ? BatchStatus.EXPIRED
                    : b.getExpiresAt().isAfter(warningUntil) ? BatchStatus.VALID : BatchStatus.EXPIRING);
            item.setRemainingShelfLifeSeconds(Math.max(0, Duration.between(now, b.getExpiresAt()).toSeconds()));
            return item;
        }).getContent());
        response.setPage(page);
        response.setLimit(limit);
        response.setTotal(result.getTotalElements());
        return response;
    }

    private static BatchStatus parseStatus(String value) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("all")) {
            return null;
        }
        try {
            return BatchStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw Paging.validation("expiryStatus: допустимы valid, expiring, expired, all");
        }
    }
}
