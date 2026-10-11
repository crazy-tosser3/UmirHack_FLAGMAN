package com.example.UmirHack_Flagman.auth;

import java.time.Instant;
import java.util.UUID;

import com.example.UmirHack_Flagman.auth.dto.Role;
import com.example.UmirHack_Flagman.auth.dto.UserStatus;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Setter
@Getter
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private UserStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant updatedAt;

    // Привязанный Telegram: вход через виджет и команды бота (только manager/admin)
    @Column(unique = true)
    private Long telegramId;

    @Column(length = 64)
    private String telegramUsername;

    protected UserEntity() {
    }

    public UserEntity(String name, String email, String passwordHash) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = Role.EMPLOYEE;
        this.status = UserStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }

}
