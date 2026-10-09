package com.example.UmirHack_Flagman.Entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.Getter;

// Сохраняется в той же транзакции, что и операция: ключ появляется только при успехе
@Entity
@Table(name = "idempotency_keys")
@Getter
public class IdempotencyKeyEntity {

    // scope:key, например delivery:abc
    @Id
    @Column(length = 300)
    private String id;

    @Column(nullable = false, length = 64)
    private String requestHash;

    @Column(nullable = false)
    private UUID resourceId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected IdempotencyKeyEntity() {
    }

    public IdempotencyKeyEntity(String id, String requestHash, UUID resourceId) {
        this.id = id;
        this.requestHash = requestHash;
        this.resourceId = resourceId;
    }
}
