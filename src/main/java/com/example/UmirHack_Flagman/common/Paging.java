package com.example.UmirHack_Flagman.common;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

public final class Paging {
    public static final int MAX_LIMIT = 100;

    private Paging() {
    }

    public static Pageable pageable(int page, int limit, Sort sort) {
        if (page < 1 || limit < 1 || limit > MAX_LIMIT) {
            throw validation("page должен быть >= 1, limit — от 1 до " + MAX_LIMIT);
        }
        return PageRequest.of(page - 1, limit, sort);
    }

    public static <T> List<T> slice(List<T> all, int page, int limit) {
        Pageable p = pageable(page, limit, Sort.unsorted());
        int from = (int) Math.min(p.getOffset(), all.size());
        return all.subList(from, Math.min(from + limit, all.size()));
    }

    public static void checkRange(Instant from, Instant to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw validation("from не может быть позже to");
        }
    }

    public static String likePattern(String search) {
        String escaped = search.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }

    public static ApiException validation(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }
}
