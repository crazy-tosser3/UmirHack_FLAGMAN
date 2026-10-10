package com.example.UmirHack_Flagman.Handlers;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.UmirHack_Flagman.Config.AuthInterceptor;
import com.example.UmirHack_Flagman.Config.Roles;
import com.example.UmirHack_Flagman.Services.AdminService;
import com.example.UmirHack_Flagman.models.ChangeRole;
import com.example.UmirHack_Flagman.models.ChangeStatus;
import com.example.UmirHack_Flagman.models.CreateUserData;
import com.example.UmirHack_Flagman.models.CreatedUserResponse;
import com.example.UmirHack_Flagman.models.InviteResponse;
import com.example.UmirHack_Flagman.models.UpdateUserData;
import com.example.UmirHack_Flagman.models.UserChangeResponse;
import com.example.UmirHack_Flagman.models.UserList;
import com.example.UmirHack_Flagman.models.UserResponse;

import jakarta.validation.Valid;

@RestController 
@RequestMapping("/api/v1/admin/")
public class AdminHandler {
    private final AdminService adminService;

    public AdminHandler(AdminService adminService) {
        this.adminService = adminService;
    }

    // Пользователи

    @GetMapping("users")
    public ResponseEntity<UserList> getUsers(
            @RequestAttribute(AuthInterceptor.ROLE_ATTR) String role,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String search,
            @RequestParam(name = "role", required = false) String roleFilter,
            @RequestParam(required = false) String status) {
        Roles.require(role, Roles.ADMIN);
        return ResponseEntity.ok(adminService.getUsers(page, limit, search, roleFilter, status));
    }

    // Возвращает ссылку-приглашение: админ пересылает ее пользователю, тот задает пароль сам
    @PostMapping("users")
    public ResponseEntity<CreatedUserResponse> createUser(
            @RequestAttribute(AuthInterceptor.ROLE_ATTR) String role,
            @Valid @RequestBody CreateUserData req) {
        Roles.require(role, Roles.ADMIN);
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createUser(req));
    }

    @PostMapping("users/{userId}/invite")
    public ResponseEntity<InviteResponse> resendInvite(
            @RequestAttribute(AuthInterceptor.ROLE_ATTR) String role,
            @PathVariable UUID userId) {
        Roles.require(role, Roles.ADMIN);
        return ResponseEntity.ok(adminService.resendInvite(userId));
    }

    @GetMapping("users/{userId}")
    public ResponseEntity<UserResponse> getUser(
            @RequestAttribute(AuthInterceptor.ROLE_ATTR) String role,
            @PathVariable UUID userId) {
        Roles.require(role, Roles.ADMIN);
        return ResponseEntity.ok(adminService.getUser(userId));
    }

    @PatchMapping("users/{userId}")
    public ResponseEntity<UserResponse> updateUser(
            @RequestAttribute(AuthInterceptor.ROLE_ATTR) String role,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserData req) {
        Roles.require(role, Roles.ADMIN);
        return ResponseEntity.ok(adminService.updateUser(userId, req));
    }

    @PatchMapping("users/{userId}/role")
    public ResponseEntity<UserChangeResponse> changeRole(
            @RequestAttribute(AuthInterceptor.ROLE_ATTR) String role,
            @PathVariable UUID userId,
            @Valid @RequestBody ChangeRole req) {
        Roles.require(role, Roles.ADMIN);
        return ResponseEntity.ok(adminService.changeRole(userId, req.getRole()));
    }

    @PatchMapping("users/{userId}/status")
    public ResponseEntity<UserChangeResponse> changeStatus(
            @RequestAttribute(AuthInterceptor.ROLE_ATTR) String role,
            @PathVariable UUID userId,
            @Valid @RequestBody ChangeStatus req) {
        Roles.require(role, Roles.ADMIN);
        return ResponseEntity.ok(adminService.changeStatus(userId, req.getStatus()));
    }
}
