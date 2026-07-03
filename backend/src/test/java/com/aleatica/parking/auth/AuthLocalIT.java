package com.aleatica.parking.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aleatica.parking.support.BaseIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Tests de integracion de autenticacion local contra un SQL Server real
 * (Testcontainers): verifica el flujo completo end-to-end con Flyway (V4
 * employees + V5 seed admin), la sesion JDBC y la cookie parking_SESSION.
 *
 * <p>Flujo critico 100 %: login OK/KO, bloqueo tras 5 intentos y cambio de
 * contrasena reales contra la BD.</p>
 */
class AuthLocalIT extends BaseIntegrationTest {

    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String ME_URL = "/api/v1/auth/me";
    private static final String CHANGE_PWD_URL = "/api/v1/auth/change-password";
    private static final String SESSION_COOKIE = "parking_SESSION";

    private static final String EMP_LOGIN = "ituser";
    private static final String EMP_EMAIL = "ituser@aleatica.com";
    private static final String EMP_PASSWORD = "ItUser#Pass1word";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanAndSeed() {
        jdbcTemplate.update("DELETE FROM dbo.login_log");
        jdbcTemplate.update("DELETE FROM dbo.employees WHERE login = ?", EMP_LOGIN);
        insertEmployee(EMP_LOGIN, EMP_EMAIL, passwordEncoder.encode(EMP_PASSWORD), "EMPLOYEE", true);
    }

    @Test
    void shouldAuthenticateAndIssueCookie_whenCredentialsValid() throws Exception {
        // Act
        MvcResult result = login(EMP_LOGIN, EMP_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value(EMP_LOGIN))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"))
                .andExpect(jsonPath("$.passwordMustChange").value(false))
                .andExpect(cookie().exists(SESSION_COOKIE))
                .andReturn();

        // Assert: the issued cookie authenticates a follow-up /me request
        Cookie session = result.getResponse().getCookie(SESSION_COOKIE);
        assertThat(session).isNotNull();
        mockMvc.perform(get(ME_URL).cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value(EMP_LOGIN));

        assertThat(loginLogResult(EMP_LOGIN)).isEqualTo("OK");
    }

    @Test
    void shouldReturn401_whenPasswordIncorrect() throws Exception {
        // Act / Assert
        login(EMP_LOGIN, "WrongPassword#1")
                .andExpect(status().isUnauthorized());
        assertThat(failedAttempts(EMP_LOGIN)).isEqualTo(1);
        assertThat(loginLogResult(EMP_LOGIN)).isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    void shouldReturn401WithoutRevealingExistence_whenLoginUnknown() throws Exception {
        // Act / Assert: unknown login yields the same generic 401
        login("doesnotexist", "Whatever#Pass1")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
        assertThat(loginLogResult("doesnotexist")).isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    void shouldLockAccount_whenFiveConsecutiveFailures() throws Exception {
        // Arrange: five wrong attempts
        for (int i = 0; i < 5; i++) {
            login(EMP_LOGIN, "WrongPassword#1").andExpect(status().isUnauthorized());
        }

        // Assert: account locked, and a now-correct password is still rejected
        assertThat(failedAttempts(EMP_LOGIN)).isEqualTo(5);
        assertThat(lockedUntil(EMP_LOGIN)).isNotNull();
        login(EMP_LOGIN, EMP_PASSWORD).andExpect(status().isUnauthorized());
        assertThat(loginLogResult(EMP_LOGIN)).isEqualTo("LOCKED");
    }

    @Test
    void shouldReturn401_whenAccountInactive() throws Exception {
        // Arrange
        jdbcTemplate.update("UPDATE dbo.employees SET active = 0 WHERE login = ?", EMP_LOGIN);

        // Act / Assert
        login(EMP_LOGIN, EMP_PASSWORD).andExpect(status().isUnauthorized());
        assertThat(loginLogResult(EMP_LOGIN)).isEqualTo("INACTIVE");
    }

    @Test
    void shouldChangePassword_whenCurrentValidAndNewMeetsPolicy() throws Exception {
        // Arrange: authenticate to obtain a session
        Cookie session = login(EMP_LOGIN, EMP_PASSWORD).andReturn()
                .getResponse().getCookie(SESSION_COOKIE);
        String newPassword = "Changed#Pass2word";

        // Act
        mockMvc.perform(post(CHANGE_PWD_URL).cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changeBody(EMP_PASSWORD, newPassword)))
                .andExpect(status().isNoContent());

        // Assert: old password no longer works, new one does
        login(EMP_LOGIN, EMP_PASSWORD).andExpect(status().isUnauthorized());
        login(EMP_LOGIN, newPassword).andExpect(status().isOk());
    }

    @Test
    void shouldReturn400_whenNewPasswordViolatesPolicy() throws Exception {
        // Arrange
        Cookie session = login(EMP_LOGIN, EMP_PASSWORD).andReturn()
                .getResponse().getCookie(SESSION_COOKIE);

        // Act / Assert: new password without symbol fails the policy
        mockMvc.perform(post(CHANGE_PWD_URL).cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changeBody(EMP_PASSWORD, "NoSymbol1Password")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.newPassword").exists());
    }

    @Test
    void shouldAuthenticateSeedDevAdmin_whenUsingSeededCredentials() throws Exception {
        // Act / Assert: the V5 seed admin can log in with documented credentials
        login("admin", "Admin#Parking2026")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    private org.springframework.test.web.servlet.ResultActions login(String login, String password)
            throws Exception {
        return mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(login, password)));
    }

    private void insertEmployee(
            String login, String email, String hash, String role, boolean active) {
        jdbcTemplate.update(
                "INSERT INTO dbo.employees "
                        + "(first_name, last_name, login, email, password_hash, password_must_change, "
                        + "is_corporate, auth_origin, role, enabled, active) "
                        + "VALUES (?, ?, ?, ?, ?, 0, 0, 'LOCAL', ?, 1, ?)",
                "IT", "User", login, email, hash, role, active ? 1 : 0);
    }

    private int failedAttempts(String login) {
        Integer value = jdbcTemplate.queryForObject(
                "SELECT failed_login_attempts FROM dbo.employees WHERE login = ?",
                Integer.class, login);
        return value == null ? 0 : value;
    }

    private Object lockedUntil(String login) {
        return jdbcTemplate.queryForObject(
                "SELECT locked_until FROM dbo.employees WHERE login = ?", Object.class, login);
    }

    private String loginLogResult(String loginAttempted) {
        return jdbcTemplate.queryForObject(
                "SELECT TOP 1 result FROM dbo.login_log WHERE login_attempted = ? "
                        + "ORDER BY occurred_at DESC, id DESC",
                String.class, loginAttempted);
    }

    private static String loginBody(String login, String password) {
        return "{\"login\":\"" + login + "\",\"password\":\"" + password + "\"}";
    }

    private static String changeBody(String current, String next) {
        return "{\"currentPassword\":\"" + current + "\",\"newPassword\":\"" + next + "\"}";
    }
}
