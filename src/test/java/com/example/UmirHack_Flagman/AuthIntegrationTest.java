package com.example.UmirHack_Flagman;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.example.UmirHack_Flagman.auth.UserEntity;
import com.example.UmirHack_Flagman.auth.dto.Role;
import com.example.UmirHack_Flagman.auth.dto.UserStatus;
import com.example.UmirHack_Flagman.support.IntegrationTest;

class AuthIntegrationTest extends IntegrationTest {
    private static final String EMAIL = "user@test.local";

    @Test
    void registerLoginAndMe() {
        Response registered = post("/auth/register", null, registration(EMAIL, inviteCode(Role.EMPLOYEE)));
        assertThat(registered.status()).isEqualTo(201);
        assertThat(registered.body().get("user").get("role").asString()).isEqualTo("employee");
        assertThat(registered.body().get("user").has("password")).isFalse();

        Response login = login(EMAIL, PASSWORD);
        assertThat(login.status()).isEqualTo(200);
        assertThat(login.headers().firstValue("cache-control")).hasValue("no-store");
        assertThat(login.cookie("refresh_token")).contains("HttpOnly").contains("Path=/api/v1/auth");

        Response me = get("/auth/me", login.body().get("accessToken").asString());
        assertThat(me.status()).isEqualTo(200);
        assertThat(me.body().get("email").asString()).isEqualTo(EMAIL);
    }

    @Test
    void emailIsUniqueIgnoringCase() {
        existingUser(EMAIL, Role.EMPLOYEE);
        String code = inviteCode(Role.EMPLOYEE);

        Response duplicate = post("/auth/register", null, registration(EMAIL.toUpperCase(), code));

        assertThat(duplicate.status()).isEqualTo(409);
        assertThat(duplicate.code()).isEqualTo("EMAIL_ALREADY_EXISTS");
    }

    @Test
    void registrationIsValidated() {
        Response r = post("/auth/register", null, Map.of("email", "bad", "password", "short", "name", ""));

        assertThat(r.status()).isEqualTo(400);
        assertThat(r.code()).isEqualTo("VALIDATION_ERROR");
        // email, password, name и отсутствующий code
        assertThat(r.body().get("error").get("details").size()).isEqualTo(4);
    }

    @Test
    void wrongPasswordAndUnknownEmailLookTheSame() {
        existingUser(EMAIL, Role.EMPLOYEE);

        Response wrongPassword = login(EMAIL, "WrongPassword-123");
        Response unknownEmail = login("nobody@test.local", "WrongPassword-123");

        assertThat(wrongPassword.status()).isEqualTo(401);
        assertThat(unknownEmail.status()).isEqualTo(401);
        assertThat(wrongPassword.code()).isEqualTo(unknownEmail.code()).isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    void disabledAccountCannotLogin() {
        existingUser(EMAIL, Role.EMPLOYEE);
        UserEntity user = userRepo.findByEmail(EMAIL).orElseThrow();
        user.setStatus(UserStatus.DISABLED);
        userRepo.save(user);

        Response r = login(EMAIL, PASSWORD);

        assertThat(r.status()).isEqualTo(403);
        assertThat(r.code()).isEqualTo("ACCOUNT_DISABLED");
    }

    @Test
    void protectedEndpointsRequireValidToken() {
        existingUser(EMAIL, Role.EMPLOYEE);
        String token = login(EMAIL, PASSWORD).body().get("accessToken").asString();

        assertThat(get("/auth/me", null).status()).isEqualTo(401);
        assertThat(get("/auth/me", token + "x").status()).isEqualTo(401);
        assertThat(get("/dishes", null).status()).isEqualTo(401);
    }

    @Test
    void refreshRotatesTokenAndDetectsReuse() {
        existingUser(EMAIL, Role.EMPLOYEE);
        String first = login(EMAIL, PASSWORD).cookieValue("refresh_token");

        Response refreshed = refresh(first);
        String second = refreshed.cookieValue("refresh_token");
        assertThat(refreshed.status()).isEqualTo(200);
        assertThat(refreshed.body().get("accessToken").asString()).isNotBlank();
        assertThat(second).isNotEqualTo(first);

        Response reused = refresh(first);
        assertThat(reused.status()).isEqualTo(401);
        assertThat(reused.code()).isEqualTo("REFRESH_TOKEN_REUSED");
        assertThat(reused.cookie("refresh_token")).contains("Max-Age=0");

        // После обнаружения повтора отозвана вся сессия, включая новый токен
        Response afterReuse = refresh(second);
        assertThat(afterReuse.status()).isEqualTo(401);
        assertThat(afterReuse.code()).isEqualTo("INVALID_REFRESH_TOKEN");
    }

    @Test
    void logoutRevokesSessionAndIsIdempotent() {
        existingUser(EMAIL, Role.EMPLOYEE);
        String token = login(EMAIL, PASSWORD).cookieValue("refresh_token");

        Response logout = post("/auth/logout", null, null, "Cookie", "refresh_token=" + token);
        assertThat(logout.status()).isEqualTo(204);
        assertThat(logout.cookie("refresh_token")).contains("Max-Age=0");

        assertThat(refresh(token).status()).isEqualTo(401);
        assertThat(post("/auth/logout", null, null, "Cookie", "refresh_token=" + token).status()).isEqualTo(204);
    }

    private Response refresh(String token) {
        return post("/auth/refresh", null, null, "Cookie", "refresh_token=" + token);
    }
}
