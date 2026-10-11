package com.example.UmirHack_Flagman.common;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface IdempotencyKeyRepoDB extends JpaRepository<IdempotencyKeyEntity, String> {

    @Modifying
    @Query("delete from IdempotencyKeyEntity k where k.createdAt < :before")
    int deleteOlderThan(Instant before);
}
