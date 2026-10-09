package com.example.UmirHack_Flagman.Services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.UmirHack_Flagman.Entity.DishEntity;
import com.example.UmirHack_Flagman.Entity.IngredientEntity;
import com.example.UmirHack_Flagman.Repositories.DishRepoDB;
import com.example.UmirHack_Flagman.Repositories.IngredientRepoDB;
import com.example.UmirHack_Flagman.models.DishList;
import com.example.UmirHack_Flagman.models.IngredientList;

import jakarta.persistence.criteria.Predicate;

@Service
@Transactional(readOnly = true)
public class CatalogService {
    private final IngredientRepoDB ingredientRepo;
    private final DishRepoDB dishRepo;

    public CatalogService(IngredientRepoDB ingredientRepo, DishRepoDB dishRepo) {
        this.ingredientRepo = ingredientRepo;
        this.dishRepo = dishRepo;
    }

    public IngredientList getIngredients(int page, int limit, String search) {
        Specification<IngredientEntity> spec = (root, query, cb) -> search == null || search.isBlank()
                ? cb.conjunction()
                : cb.like(cb.lower(root.get("name")), Paging.likePattern(search), '\\');

        Page<IngredientEntity> result = ingredientRepo.findAll(spec, Paging.pageable(page, limit, Sort.by("name")));

        IngredientList response = new IngredientList();
        response.setItems(result.map(i -> {
            IngredientList.Item item = new IngredientList.Item();
            item.setId(i.getId());
            item.setName(i.getName());
            item.setUnit(i.getUnit());
            return item;
        }).getContent());
        response.setPage(page);
        response.setLimit(limit);
        response.setTotal(result.getTotalElements());
        return response;
    }

    public DishList getDishes(int page, int limit, String search, Boolean active) {
        Specification<DishEntity> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                p.add(cb.like(cb.lower(root.get("name")), Paging.likePattern(search), '\\'));
            }
            if (active != null) {
                p.add(cb.equal(root.get("active"), active));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };

        Page<DishEntity> result = dishRepo.findAll(spec, Paging.pageable(page, limit, Sort.by("name")));
        Map<UUID, DishEntity> withIngredients = dishRepo
                .findWithIngredients(result.getContent().stream().map(DishEntity::getId).toList())
                .stream().collect(Collectors.toMap(DishEntity::getId, Function.identity()));

        DishList response = new DishList();
        response.setItems(result.getContent().stream()
                .map(d -> toItem(withIngredients.getOrDefault(d.getId(), d)))
                .toList());
        response.setPage(page);
        response.setLimit(limit);
        response.setTotal(result.getTotalElements());
        return response;
    }

    private static DishList.Item toItem(DishEntity dish) {
        DishList.Item item = new DishList.Item();
        item.setId(dish.getId());
        item.setName(dish.getName());
        item.setActive(dish.isActive());
        item.setIngredients(dish.getIngredients().stream().map(di -> {
            DishList.Ingredient ingredient = new DishList.Ingredient();
            ingredient.setIngredientId(di.getIngredient().getId());
            ingredient.setIngredientName(di.getIngredient().getName());
            ingredient.setQuantity(di.getQuantity());
            ingredient.setUnit(di.getIngredient().getUnit());
            return ingredient;
        }).toList());
        return item;
    }
}
