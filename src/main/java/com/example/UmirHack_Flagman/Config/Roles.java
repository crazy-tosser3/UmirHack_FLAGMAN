package com.example.UmirHack_Flagman.Config;

import java.util.Set;

import org.springframework.http.HttpStatus;

import com.example.UmirHack_Flagman.Exceptions.ApiException;

public final class Roles {
    public static final Set<String> STAFF = Set.of("manager", "admin");
    public static final Set<String> ADMIN = Set.of("admin");

    private Roles() {
    }

    public static void require(String role, Set<String> allowed) {
        if (!allowed.contains(role)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Недостаточно прав для выполнения операции");
        }
    }
}
