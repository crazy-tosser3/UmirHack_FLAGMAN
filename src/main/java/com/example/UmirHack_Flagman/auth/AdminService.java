package com.example.UmirHack_Flagman.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.UmirHack_Flagman.auth.dto.CreateInvite;
import com.example.UmirHack_Flagman.auth.dto.InviteResponse;
import com.example.UmirHack_Flagman.auth.dto.Role;
import com.example.UmirHack_Flagman.auth.dto.UpdateUserData;
import com.example.UmirHack_Flagman.auth.dto.UserChangeResponse;
import com.example.UmirHack_Flagman.auth.dto.UserList;
import com.example.UmirHack_Flagman.auth.dto.UserResponse;
import com.example.UmirHack_Flagman.auth.dto.UserStatus;
import com.example.UmirHack_Flagman.common.ApiException;
import com.example.UmirHack_Flagman.common.Paging;

import jakarta.persistence.criteria.Predicate;

@Service
public class AdminService {
    private final UserRepoDB userRepo;
    private final RefreshTokenService refreshTokenService;
    private final InviteService inviteService;
    private final Duration inviteTtl;

    private static final Duration MAX_INVITE_TTL = Duration.ofDays(30);

    public AdminService(UserRepoDB userRepo, RefreshTokenService refreshTokenService, InviteService inviteService,
            @Value("${app.invite.ttl:48h}") Duration inviteTtl) {
        this.userRepo = userRepo;
        this.refreshTokenService = refreshTokenService;
        this.inviteService = inviteService;
        this.inviteTtl = inviteTtl;
    }

    @Transactional(readOnly = true)
    public UserList getUsers(int page, int limit, String search, String role, String status) {
        Role roleFilter = role == null || role.isBlank() ? null : parseRole(role);
        UserStatus statusFilter = status == null || status.isBlank() ? null : parseStatus(status);

        Specification<UserEntity> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String pattern = Paging.likePattern(search);
                p.add(cb.or(cb.like(cb.lower(root.get("name")), pattern, '\\'),
                        cb.like(cb.lower(root.get("email")), pattern, '\\')));
            }
            if (roleFilter != null) {
                p.add(cb.equal(root.get("role"), roleFilter));
            }
            if (statusFilter != null) {
                p.add(cb.equal(root.get("status"), statusFilter));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };

        Page<UserEntity> result = userRepo.findAll(spec,
                Paging.pageable(page, limit, Sort.by(Sort.Direction.DESC, "createdAt")));

        UserList response = new UserList();
        response.setItems(result.map(AdminService::toResponse).getContent());
        response.setPage(page);
        response.setLimit(limit);
        response.setTotal(result.getTotalElements());
        return response;
    }

    public InviteResponse createInvite(CreateInvite data) {
        Instant now = Instant.now();
        Instant expiresAt = data.getExpiresAt() != null ? data.getExpiresAt() : now.plus(inviteTtl);
        if (!expiresAt.isAfter(now)) {
            throw Paging.validation("expiresAt должен быть в будущем");
        }
        if (expiresAt.isAfter(now.plus(MAX_INVITE_TTL))) {
            throw Paging.validation("expiresAt — не позже чем через " + MAX_INVITE_TTL.toDays() + " дней");
        }
        int clicks = data.getClicks() != null ? data.getClicks() : 1;
        String code = UUID.randomUUID().toString();

        InviteService.Invite invite = inviteService.create(
                code, data.getRole().name().toLowerCase(Locale.ROOT), expiresAt.toString(), clicks);

        InviteResponse response = new InviteResponse();
        response.setInviteUrl(invite.url());
        response.setExpiresAt(invite.expiresAt());
        response.setClicks(clicks);
        return response;
    }

    public void revokeInvite(String code) {
        inviteService.revoke(code);
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(UUID userId) {
        return toResponse(find(userId));
    }

    @Transactional
    public UserResponse updateUser(UUID userId, UpdateUserData data) {
        UserEntity user = find(userId);
        if (data.getName() != null) {
            user.setName(data.getName().trim());
        }
        if (data.getEmail() != null) {
            String email = normalizeEmail(data.getEmail());
            if (!email.equals(user.getEmail()) && userRepo.existsByEmail(email)) {
                throw userExists();
            }
            user.setEmail(email);
        }
        try {
            return toResponse(userRepo.saveAndFlush(user));
        } catch (DataIntegrityViolationException e) {
            throw userExists();
        }
    }

    @Transactional
    public UserChangeResponse changeRole(UUID userId, Role role) {
        UserEntity user = find(userId);
        if (user.getRole() == Role.ADMIN && role != Role.ADMIN) {
            ensureNotLastAdmin(user);
        }
        user.setRole(role);
        userRepo.saveAndFlush(user);

        UserChangeResponse response = new UserChangeResponse();
        response.setId(user.getId());
        response.setRole(user.getRole());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }

    @Transactional
    public UserChangeResponse changeStatus(UUID userId, String status) {
        UserStatus newStatus = switch (parseStatus(status)) {
            case PENDING_VERIFICATION -> throw Paging.validation("status: допустимы active, blocked");
            case UserStatus s -> s;
        };
        UserEntity user = find(userId);
        if (newStatus == UserStatus.ACTIVE && user.getPasswordHash() == null) {
            newStatus = UserStatus.PENDING_VERIFICATION;
        }
        if (newStatus == UserStatus.DISABLED && user.getRole() == Role.ADMIN) {
            ensureNotLastAdmin(user);
        }
        user.setStatus(newStatus);
        userRepo.saveAndFlush(user);
        if (newStatus == UserStatus.DISABLED) {
            refreshTokenService.revokeAll(user.getId());
        }

        UserChangeResponse response = new UserChangeResponse();
        response.setId(user.getId());
        response.setStatus(user.getStatus());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }

    private void ensureNotLastAdmin(UserEntity target) {
        List<UserEntity> activeAdmins = userRepo.lockActiveAdmins();
        boolean othersRemain = activeAdmins.stream().anyMatch(a -> !a.getId().equals(target.getId()));
        if (!othersRemain) {
            throw new ApiException(HttpStatus.CONFLICT, "LAST_ADMIN_REQUIRED",
                    "Нельзя понизить или заблокировать последнего активного администратора");
        }
    }

    private UserEntity find(UUID userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Пользователь не найден"));
    }

    static UserResponse toResponse(UserEntity user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setName(user.getName());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        response.setTelegramLinked(user.getTelegramId() != null);
        response.setTelegramUsername(user.getTelegramUsername());
        return response;
    }

    private static Role parseRole(String value) {
        try {
            return Role.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw Paging.validation("role: допустимы admin, manager, employee");
        }
    }

    private static UserStatus parseStatus(String value) {
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "active" -> UserStatus.ACTIVE;
            case "blocked" -> UserStatus.DISABLED;
            case "pending" -> UserStatus.PENDING_VERIFICATION;
            default -> throw Paging.validation("status: допустимы active, blocked, pending");
        };
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static ApiException userExists() {
        return new ApiException(HttpStatus.CONFLICT, "USER_ALREADY_EXISTS", "Пользователь с таким email уже существует");
    }
}
