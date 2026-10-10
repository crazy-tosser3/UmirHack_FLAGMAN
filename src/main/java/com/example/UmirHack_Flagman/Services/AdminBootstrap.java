package com.example.UmirHack_Flagman.Services;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.UmirHack_Flagman.Entity.UserEntity;
import com.example.UmirHack_Flagman.Repositories.UserRepoDB;
import com.example.UmirHack_Flagman.models.Role;
import com.example.UmirHack_Flagman.models.UserStatus;

@Component
@Order(0)
public class AdminBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);
    private static final int MIN_PASSWORD_LENGTH = 12;

    private final UserRepoDB userRepo;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String name;

    public AdminBootstrap(UserRepoDB userRepo, PasswordEncoder passwordEncoder,
            @Value("${app.admin.email:admin@flagman.local}") String email,
            @Value("${app.admin.password:}") String password,
            @Value("${app.admin.name:Администратор}") String name) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.email = email.trim().toLowerCase(Locale.ROOT);
        this.password = password;
        this.name = name;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepo.existsByRole(Role.ADMIN)) {
            return;
        }

        boolean generated = password == null || password.isBlank();
        String rawPassword = generated ? generatePassword() : password;
        if (rawPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException("ADMIN_PASSWORD должен быть не короче " + MIN_PASSWORD_LENGTH + " символов");
        }

        UserEntity existing = userRepo.findByEmail(email).orElse(null);
        if (existing == null) {
            UserEntity admin = new UserEntity(name, email, passwordEncoder.encode(rawPassword));
            admin.setRole(Role.ADMIN);
            userRepo.save(admin);
            if (generated) {
                log.warn("Создан администратор {} со сгенерированным паролем: {} — задайте ADMIN_PASSWORD, чтобы управлять им явно",
                        email, rawPassword);
            } else {
                log.info("Создан администратор {}", email);
            }
            return;
        }

        boolean noPassword = existing.getPasswordHash() == null;
        existing.setRole(Role.ADMIN);
        existing.setStatus(UserStatus.ACTIVE);
        if (!generated || noPassword) {
            existing.setPasswordHash(passwordEncoder.encode(rawPassword));
        }
        userRepo.save(existing);
        if (generated && noPassword) {
            log.warn("Пользователь {} назначен администратором со сгенерированным паролем: {}", email, rawPassword);
        } else {
            log.info("Пользователь {} назначен администратором", email);
        }
    }

    private static String generatePassword() {
        byte[] bytes = new byte[18];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
