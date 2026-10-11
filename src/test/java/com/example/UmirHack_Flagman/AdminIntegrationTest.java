package com.example.UmirHack_Flagman;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.UmirHack_Flagman.Entity.UserEntity;
import com.example.UmirHack_Flagman.Services.AdminBootstrap;
import com.example.UmirHack_Flagman.models.Role;
import com.example.UmirHack_Flagman.support.IntegrationTest;

import tools.jackson.databind.JsonNode;

class AdminIntegrationTest extends IntegrationTest {
    private static final String EMAIL = "ivan@test.local";

    @Autowired
    private AdminBootstrap bootstrap;

    private String admin;

    @BeforeEach
    void setUp() {
        admin = tokenFor(Role.ADMIN);
    }

    @Test
    void onlyAdminCanManageUsers() {
        assertThat(get("/admin/users", tokenFor(Role.MANAGER)).status()).isEqualTo(403);
        assertThat(get("/admin/users", tokenFor(Role.EMPLOYEE)).status()).isEqualTo(403);
        assertThat(get("/admin/users", null).status()).isEqualTo(401);
        assertThat(get("/admin/users", admin).status()).isEqualTo(200);
    }

    @Test
    void inviteGivesRoleOnRegistration() {
        Response created = post("/admin/users/invite", admin, Map.of("role", "manager"));
        assertThat(created.status()).isEqualTo(201);
        assertThat(created.body().get("clicks").asInt()).isEqualTo(1);
        assertThat(created.body().get("expiresAt").asString()).isNotBlank();
        assertThat(created.body().has("code")).isFalse();
        // Код генерирует сервер — это UUID в ссылке
        String code = codeFrom(created.body());
        UUID.fromString(code);

        Response info = post("/auth/invite/verify", null, Map.of("code", code));
        assertThat(info.status()).isEqualTo(200);
        assertThat(info.body().get("role").asString()).isEqualTo("manager");
        assertThat(info.body().get("remainingClicks").asInt()).isEqualTo(1);

        Response registered = post("/auth/register", null, registration(EMAIL, code));
        assertThat(registered.status()).isEqualTo(201);
        assertThat(registered.body().get("user").get("role").asString()).isEqualTo("manager");
        assertThat(get("/stock", login(EMAIL, PASSWORD).body().get("accessToken").asString()).status()).isEqualTo(200);

        Response reused = post("/auth/register", null, registration("second@test.local", code));
        assertThat(reused.status()).isEqualTo(404);
        assertThat(reused.code()).isEqualTo("INVITE_NOT_FOUND");
        assertThat(post("/auth/invite/verify", null, Map.of("code", code)).status()).isEqualTo(404);
    }

    @Test
    void multiUseAndUnlimitedInvites() {
        String twice = codeFrom(post("/admin/users/invite", admin, Map.of("role", "employee", "clicks", 2)).body());
        assertThat(post("/auth/register", null, registration("a@test.local", twice)).status()).isEqualTo(201);
        assertThat(post("/auth/invite/verify", null, Map.of("code", twice)).body().get("remainingClicks").asInt()).isEqualTo(1);
        assertThat(post("/auth/register", null, registration("b@test.local", twice)).status()).isEqualTo(201);
        assertThat(post("/auth/register", null, registration("c@test.local", twice)).status()).isEqualTo(404);

        String unlimited = codeFrom(post("/admin/users/invite", admin, Map.of("role", "employee", "clicks", 0)).body());
        for (String email : List.of("d@test.local", "e@test.local", "f@test.local")) {
            assertThat(post("/auth/register", null, registration(email, unlimited)).status()).isEqualTo(201);
        }
        assertThat(post("/auth/invite/verify", null, Map.of("code", unlimited)).body().get("remainingClicks").isNull()).isTrue();
    }

    @Test
    void failedRegistrationDoesNotSpendInvite() {
        createUser(EMAIL, "employee");
        String code = codeFrom(post("/admin/users/invite", admin, Map.of("role", "manager")).body());

        // Email занят — код не должен расходоваться
        assertThat(post("/auth/register", null, registration(EMAIL, code)).code()).isEqualTo("EMAIL_ALREADY_EXISTS");
        assertThat(post("/auth/register", null, registration("free@test.local", code)).status()).isEqualTo(201);
    }

    @Test
    void revokedAndUnknownInvitesAreRejected() {
        String code = codeFrom(post("/admin/users/invite", admin, Map.of("role", "admin")).body());

        assertThat(send("DELETE", "/admin/users/invite/" + code, admin, null).status()).isEqualTo(204);
        assertThat(post("/auth/register", null, registration(EMAIL, code)).code()).isEqualTo("INVITE_NOT_FOUND");
        assertThat(post("/auth/register", null, registration(EMAIL, UUID.randomUUID().toString())).code())
                .isEqualTo("INVITE_NOT_FOUND");
        assertThat(userRepo.findByEmail(EMAIL)).isEmpty();
    }

    @Test
    void registrationWithoutInviteIsImpossible() {
        Response withoutCode = post("/auth/register", null, registration(EMAIL));
        assertThat(withoutCode.status()).isEqualTo(400);
        assertThat(withoutCode.code()).isEqualTo("VALIDATION_ERROR");
        assertThat(post("/auth/register", null, registration(EMAIL, "  ")).status()).isEqualTo(400);
        assertThat(userRepo.findByEmail(EMAIL)).isEmpty();
    }

    @Test
    void inviteValidation() {
        assertThat(post("/admin/users/invite", admin, Map.of("role", "superuser")).status()).isEqualTo(400);
        assertThat(post("/admin/users/invite", admin, Map.of()).status()).isEqualTo(400);
        assertThat(post("/admin/users/invite", admin, Map.of("role", "employee", "clicks", -1)).status()).isEqualTo(400);
        assertThat(post("/admin/users/invite", admin, Map.of("role", "employee", "expiresAt", "2020-01-01T00:00:00Z")).status())
                .isEqualTo(400);
        assertThat(post("/admin/users/invite", admin, Map.of("role", "employee", "expiresAt", "2099-01-01T00:00:00Z")).status())
                .isEqualTo(400);
        assertThat(post("/admin/users/invite", tokenFor(Role.MANAGER), Map.of("role", "employee")).status()).isEqualTo(403);

        Instant custom = Instant.now().plus(Duration.ofDays(3)).truncatedTo(ChronoUnit.SECONDS);
        Response created = post("/admin/users/invite", admin, Map.of("role", "employee", "expiresAt", custom.toString()));
        assertThat(Instant.parse(created.body().get("expiresAt").asString())).isEqualTo(custom);
    }

    @Test
    void listFiltersBySearchRoleAndStatus() {
        UUID ivan = createUser(EMAIL, "manager");
        createUser("petr@test.local", "employee");
        patch("/admin/users/" + ivan + "/status", Map.of("status", "blocked"));

        assertThat(emails(get("/admin/users?role=manager", admin).body())).containsExactly(EMAIL);
        assertThat(emails(get("/admin/users?status=blocked", admin).body())).containsExactly(EMAIL);
        assertThat(emails(get("/admin/users?search=PETR", admin).body())).containsExactly("petr@test.local");
        assertThat(get("/admin/users?role=root", admin).status()).isEqualTo(400);
        assertThat(get("/admin/users?status=deleted", admin).status()).isEqualTo(400);
    }

    @Test
    void getAndUpdateProfile() {
        UUID id = createUser(EMAIL, "employee");
        createUser("taken@test.local", "employee");

        Response updated = patch("/admin/users/" + id, Map.of("name", "Иван Петров", "email", "New@Test.local"));
        assertThat(updated.status()).isEqualTo(200);
        assertThat(updated.body().get("name").asString()).isEqualTo("Иван Петров");
        assertThat(updated.body().get("email").asString()).isEqualTo("new@test.local");
        assertThat(updated.body().get("updatedAt").isNull()).isFalse();

        assertThat(patch("/admin/users/" + id, Map.of("email", "taken@test.local")).code()).isEqualTo("USER_ALREADY_EXISTS");
        assertThat(patch("/admin/users/" + id, Map.of("name", "  ")).status()).isEqualTo(400);
        assertThat(get("/admin/users/" + UUID.randomUUID(), admin).code()).isEqualTo("USER_NOT_FOUND");
    }

    @Test
    void roleChangeAppliesOnNextLogin() {
        UUID id = createUser(EMAIL, "employee");

        Response changed = patch("/admin/users/" + id + "/role", Map.of("role", "manager"));

        assertThat(changed.status()).isEqualTo(200);
        assertThat(changed.body().get("role").asString()).isEqualTo("manager");
        assertThat(changed.body().has("status")).isFalse();
        assertThat(login(EMAIL, PASSWORD).body().get("user").get("role").asString()).isEqualTo("manager");
        assertThat(patch("/admin/users/" + id + "/role", Map.of("role", "root")).status()).isEqualTo(400);
    }

    @Test
    void lastActiveAdminCannotBeDemotedOrBlocked() {
        UUID self = adminId();

        Response demote = patch("/admin/users/" + self + "/role", Map.of("role", "manager"));
        assertThat(demote.status()).isEqualTo(409);
        assertThat(demote.code()).isEqualTo("LAST_ADMIN_REQUIRED");
        assertThat(patch("/admin/users/" + self + "/status", Map.of("status", "blocked")).code()).isEqualTo("LAST_ADMIN_REQUIRED");

        // Когда есть второй активный админ, понижение разрешено
        UUID second = createUser(EMAIL, "admin");
        assertThat(patch("/admin/users/" + second + "/role", Map.of("role", "manager")).status()).isEqualTo(200);
        createUser("third@test.local", "admin");
        assertThat(patch("/admin/users/" + self + "/role", Map.of("role", "manager")).status()).isEqualTo(200);
    }

    @Test
    void blockingRevokesSessionsAndDeniesLogin() {
        UUID id = createUser(EMAIL, "employee");
        String refreshToken = login(EMAIL, PASSWORD).cookieValue("refresh_token");

        Response blocked = patch("/admin/users/" + id + "/status", Map.of("status", "blocked"));
        assertThat(blocked.status()).isEqualTo(200);
        assertThat(blocked.body().get("status").asString()).isEqualTo("blocked");

        assertThat(post("/auth/refresh", null, null, "Cookie", "refresh_token=" + refreshToken).code())
                .isEqualTo("INVALID_REFRESH_TOKEN");
        assertThat(login(EMAIL, PASSWORD).code()).isEqualTo("ACCOUNT_DISABLED");

        patch("/admin/users/" + id + "/status", Map.of("status", "active"));
        assertThat(login(EMAIL, PASSWORD).status()).isEqualTo(200);
        assertThat(patch("/admin/users/" + id + "/status", Map.of("status", "deleted")).status()).isEqualTo(400);
    }

    @Test
    void bootstrapCreatesAdminOnlyWhenThereIsNone() {
        jdbc.update("delete from users");

        bootstrap.run(null);
        bootstrap.run(null);

        assertThat(jdbc.queryForObject("select count(*) from users where role = 'ADMIN'", Long.class)).isEqualTo(1);
        assertThat(userRepo.findByEmail("admin@flagman.local").orElseThrow().getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void bootstrapPromotesExistingUserWithAdminEmail() {
        jdbc.update("delete from users");
        existingUser("admin@flagman.local", Role.EMPLOYEE);

        bootstrap.run(null);

        UserEntity user = userRepo.findByEmail("admin@flagman.local").orElseThrow();
        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
        assertThat(login("admin@flagman.local", PASSWORD).status()).isEqualTo(200);
    }

    // Пользователь регистрируется сам по приглашению с нужной ролью
    private UUID createUser(String email, String role) {
        String code = codeFrom(post("/admin/users/invite", admin, Map.of("role", role)).body());
        post("/auth/register", null, registration(email, code));
        return userRepo.findByEmail(email).orElseThrow().getId();
    }

    private static String codeFrom(JsonNode invite) {
        String url = invite.get("inviteUrl").asString();
        assertThat(url).startsWith("http://localhost:5173/register?code=");
        return url.substring(url.indexOf("code=") + "code=".length());
    }

    private UUID adminId() {
        return jdbc.queryForObject("select id from users where role = 'ADMIN'", UUID.class);
    }

    private Response patch(String path, Object body) {
        return send("PATCH", path, admin, body);
    }

    private static List<String> emails(JsonNode page) {
        List<String> result = new ArrayList<>();
        page.get("items").forEach(u -> result.add(u.get("email").asString()));
        return result;
    }
}
