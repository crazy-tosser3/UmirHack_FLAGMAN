package com.example.UmirHack_Flagman.auth;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.example.UmirHack_Flagman.auth.dto.Role;

import jakarta.persistence.LockModeType;

public interface UserRepoDB
        extends JpaRepository<UserEntity, UUID>, JpaSpecificationExecutor<UserEntity> {

    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);

    // Блокировка не дает двум админам одновременно разжаловать друг друга
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserEntity u where u.role = com.example.UmirHack_Flagman.auth.dto.Role.ADMIN and u.status = com.example.UmirHack_Flagman.auth.dto.UserStatus.ACTIVE order by u.id")
    List<UserEntity> lockActiveAdmins();
}
