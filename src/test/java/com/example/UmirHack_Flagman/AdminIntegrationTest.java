package com.example.UmirHack_Flagman;

import static org.assertj.core.api.Assertions.assertThat;

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
    void invitedUserSetsPasswordByLinkAndIsLoggedIn() {
        Response created = post("/admin/users", admin, newUser(EMAIL, "manager"));
        assertThat(created.status()).isEqualTo(201);
        assertThat(created.body().get("user").get("status").asString()).isEqualTo("pending");
        String token = tokenFrom(created.body().get("invite"));

        // До принятия приглашения войти нельзя — пароля еще нет
        assertThat(login(EMAIL, PASSWORD).code()).isEqualTo("INVALID_CREDENTIALS");

        Response info = post("/auth/invite/verify", null, Map.of("token", token));
        assertThat(info.status()).isEqualTo(200);
        assertThat(info.body().get("email").asString()).isEqualTo(EMAIL);
        assertThat(info.body().get("role").asString()).isEqualTo("manager");

        Response accepted = post("/auth/invite/accept", null, Map.of("token", token, "password", PASSWORD));
        assertThat(accepted.status()).isEqualTo(200);
        assertThat(accepted.cookie("refresh_token")).contains("HttpOnly");
        assertThat(get("/stock", accepted.body().get("accessToken").asString()).status()).isEqualTo(200);

        assertThat(login(EMAIL, PASSWORD).status()).isEqualTo(200);
        assertThat(get("/admin/users/" + created.body().get("user").get("id").asString(), admin)
                .body().get("status").asString()).isEqualTo("active");
    }

    @Test
    void inviteLinkWorksOnlyOnce() {
        String token = tokenFrom(post("/admin/users", admin, newUser(EMAIL, "employee")).body().get("invite"));

        assertThat(post("/auth/invite/accept", null, Map.of("token", token, "password", "short")).status()).isEqualTo(400);
        assertThat(post("/auth/invite/accept", null, Map.of("token", token, "password", PASSWORD)).status()).isEqualTo(200);

        Response reused = post("/auth/invite/accept", null, Map.of("token", token, "password", "Another-password-1"));
        assertThat(reused.status()).isEqualTo(404);
        assertThat(reused.code()).isEqualTo("INVITE_NOT_FOUND");
        assertThat(post("/auth/invite/verify", null, Map.of("token", token)).status()).isEqualTo(404);
        assertThat(post("/auth/invite/verify", null, Map.of("token", "garbage")).status()).isEqualTo(404);
    }

    @Test
    void resendingInviteInvalidatesPreviousLink() {
        Response created = post("/admin/users", admin, newUser(EMAIL, "employee"));
        String id = created.body().get("user").get("id").asString();
        String first = tokenFrom(created.body().get("invite"));

        Response resent = post("/admin/users/" + id + "/invite", admin, null);
        assertThat(resent.status()).isEqualTo(200);
        String second = tokenFrom(resent.body());

        assertThat(post("/auth/invite/verify", null, Map.of("token", first)).status()).isEqualTo(404);
        assertThat(post("/auth/invite/accept", null, Map.of("token", second, "password", PASSWORD)).status()).isEqualTo(200);
        assertThat(post("/admin/users/" + id + "/invite", admin, null).code()).isEqualTo("USER_ALREADY_ACTIVATED");
    }

    @Test
    void blockingPendingUserCancelsInvite() {
        Response created = post("/admin/users", admin, newUser(EMAIL, "employee"));
        String id = created.body().get("user").get("id").asString();
        String token = tokenFrom(created.body().get("invite"));

        patch("/admin/users/" + id + "/status", Map.of("status", "blocked"));
        assertThat(post("/auth/invite/verify", null, Map.of("token", token)).status()).isEqualTo(404);
        assertThat(post("/admin/users/" + id + "/invite", admin, null).code()).isEqualTo("USER_BLOCKED");

        // Без пароля аккаунт не может стать активным — после разблокировки снова ждет приглашения
        Response unblocked = patch("/admin/users/" + id + "/status", Map.of("status", "active"));
        assertThat(unblocked.body().get("status").asString()).isEqualTo("pending");
        String fresh = tokenFrom(post("/admin/users/" + id + "/invite", admin, null).body());
        assertThat(post("/auth/invite/accept", null, Map.of("token", fresh, "password", PASSWORD)).status()).isEqualTo(200);
    }

    @Test
    void createValidation() {
        post("/admin/users", admin, newUser(EMAIL, "manager"));

        Response duplicate = post("/admin/users", admin, newUser(EMAIL.toUpperCase(), "employee"));
        assertThat(duplicate.status()).isEqualTo(409);
        assertThat(duplicate.code()).isEqualTo("USER_ALREADY_EXISTS");

        assertThat(post("/admin/users", admin, newUser("other@test.local", "superuser")).status()).isEqualTo(400);
        assertThat(post("/admin/users", admin, Map.of("name", "Иван", "email", "bad", "role", "employee")).status()).isEqualTo(400);
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
        post("/auth/register", null, registration("admin@flagman.local"));

        bootstrap.run(null);

        UserEntity user = userRepo.findByEmail("admin@flagman.local").orElseThrow();
        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
        assertThat(login("admin@flagman.local", PASSWORD).status()).isEqualTo(200);
    }

    private Map<String, Object> newUser(String email, String role) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "Иван");
        body.put("email", email);
        body.put("role", role);
        return body;
    }

    // Создает пользователя и сразу принимает приглашение с паролем PASSWORD
    private UUID createUser(String email, String role) {
        JsonNode created = post("/admin/users", admin, newUser(email, role)).body();
        post("/auth/invite/accept", null, Map.of("token", tokenFrom(created.get("invite")), "password", PASSWORD));
        return UUID.fromString(created.get("user").get("id").asString());
    }

    private static String tokenFrom(JsonNode invite) {
        String url = invite.get("inviteUrl").asString();
        assertThat(url).startsWith("http://localhost:5173/accept-invite?token=");
        return url.substring(url.indexOf("token=") + "token=".length());
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
