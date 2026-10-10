package com.example.UmirHack_Flagman.Services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.UmirHack_Flagman.Entity.DishEntity;
import com.example.UmirHack_Flagman.Entity.DishIngredientEntity;
import com.example.UmirHack_Flagman.Entity.IngredientEntity;
import com.example.UmirHack_Flagman.Exceptions.ApiException;
import com.example.UmirHack_Flagman.Repositories.DishRepoDB;
import com.example.UmirHack_Flagman.Repositories.IngredientRepoDB;
import com.example.UmirHack_Flagman.models.CreateDish;
import com.example.UmirHack_Flagman.models.DishList;
import com.example.UmirHack_Flagman.models.IngredientList;
import com.example.UmirHack_Flagman.models.PostIngredientsList;

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

    @Transactional
    public IngredientList createIngredients(PostIngredientsList req) {
        Set<String> seen = new HashSet<>();
        List<String> duplicates = req.getItems().stream()
                .map(i -> i.getName().trim())
                .filter(name -> !seen.add(name.toLowerCase(Locale.ROOT)))
                .toList();
        if (!duplicates.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Повторяющиеся названия в запросе", duplicates);
        }

        List<String> existing = req.getItems().stream()
                .map(i -> i.getName().trim())
                .filter(ingredientRepo::existsByNameIgnoreCase)
                .toList();
        if (!existing.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "INGREDIENT_ALREADY_EXISTS", "Ингредиент уже существует", existing);
        }

        List<IngredientEntity> saved = ingredientRepo.saveAll(req.getItems().stream()
                .map(i -> new IngredientEntity(i.getName().trim(), i.getUnit()))
                .toList());

        IngredientList response = new IngredientList();
        response.setItems(saved.stream().map(i -> {
            IngredientList.Item item = new IngredientList.Item();
            item.setId(i.getId());
            item.setName(i.getName());
            item.setUnit(i.getUnit());
            return item;
        }).toList());
        response.setPage(1);
        response.setLimit(saved.size());
        response.setTotal(saved.size());
        return response;
    }

    @Transactional
    public DishList createDishes(CreateDish req) {
        Set<String> seen = new HashSet<>();
        List<String> duplicates = req.getItems().stream()
                .map(d -> d.getName().trim())
                .filter(name -> !seen.add(name.toLowerCase(Locale.ROOT)))
                .toList();
        if (!duplicates.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Повторяющиеся названия в запросе", duplicates);
        }

        List<String> existing = req.getItems().stream()
                .map(d -> d.getName().trim())
                .filter(dishRepo::existsByNameIgnoreCase)
                .toList();
        if (!existing.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "DISH_ALREADY_EXISTS", "Блюдо уже существует", existing);
        }

        List<String> repeatedIngredients = req.getItems().stream()
                .filter(d -> d.getIngredients().stream().map(CreateDish.Ingredient::getIngredientId).distinct().count()
                        < d.getIngredients().size())
                .map(d -> d.getName().trim())
                .toList();
        if (!repeatedIngredients.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Ингредиент повторяется в составе блюда", repeatedIngredients);
        }

        Set<UUID> ingredientIds = req.getItems().stream()
                .flatMap(d -> d.getIngredients().stream())
                .map(CreateDish.Ingredient::getIngredientId)
                .collect(Collectors.toSet());
        Map<UUID, IngredientEntity> ingredients = ingredientRepo.findAllById(ingredientIds).stream()
                .collect(Collectors.toMap(IngredientEntity::getId, Function.identity()));
        List<UUID> missing = ingredientIds.stream().filter(id -> !ingredients.containsKey(id)).toList();
        if (!missing.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "INGREDIENT_NOT_FOUND", "Ингредиент не найден", missing);
        }

        List<DishEntity> dishes = req.getItems().stream().map(d -> {
            DishEntity dish = new DishEntity(d.getName().trim());
            dish.setActive(d.isActive());
            for (CreateDish.Ingredient i : d.getIngredients()) {
                dish.getIngredients().add(new DishIngredientEntity(dish, ingredients.get(i.getIngredientId()), i.getQuantity()));
            }
            return dish;
        }).toList();
        List<DishEntity> saved = dishRepo.saveAll(dishes);

        DishList response = new DishList();
        response.setItems(saved.stream().map(CatalogService::toItem).toList());
        response.setPage(1);
        response.setLimit(saved.size());
        response.setTotal(saved.size());
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
