package com.example.UmirHack_Flagman.common;

import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;


@Service
public class IdempotencyService {
    private static final int MAX_KEY_LENGTH = 200;

    private final IdempotencyKeyRepoDB repo;

    public IdempotencyService(IdempotencyKeyRepoDB repo) {
        this.repo = repo;
    }

    public Optional<UUID> findPrevious(String scope, String key, String requestHash) {
        if (key == null) {
            return Optional.empty();
        }
        if (key.isBlank() || key.length() > MAX_KEY_LENGTH) {
            throw Paging.validation("Idempotency-Key должен быть непустым и не длиннее " + MAX_KEY_LENGTH + " символов");
        }
        return repo.findById(scope + ":" + key).map(saved -> {
            if (!saved.getRequestHash().equals(requestHash)) {
                throw new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED",
                        "Ключ идемпотентности уже использован с другим телом запроса");
            }
            return saved.getResourceId();
        });
    }

    public void remember(String scope, String key, String requestHash, UUID resourceId) {
        if (key != null) {
            repo.save(new IdempotencyKeyEntity(scope + ":" + key, requestHash, resourceId));
        }
    }
}
