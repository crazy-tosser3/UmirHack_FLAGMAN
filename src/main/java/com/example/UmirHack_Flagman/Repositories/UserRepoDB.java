package com.example.UmirHack_Flagman.Repositories;

import com.example.UmirHack_Flagman.Entity.UserEntity;
import com.example.UmirHack_Flagman.models.Role;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepoDB
        extends JpaRepository<UserEntity, UUID>, JpaSpecificationExecutor<UserEntity> {

    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);

    // Блокировка не дает двум админам одновременно разжаловать друг друга
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserEntity u where u.role = com.example.UmirHack_Flagman.models.Role.ADMIN and u.status = com.example.UmirHack_Flagman.models.UserStatus.ACTIVE order by u.id")
    List<UserEntity> lockActiveAdmins();
}
