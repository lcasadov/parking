package com.aleatica.parking.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aleatica.parking.support.BaseIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Tests de integracion del reenvio de aviso de una solicitud pendiente estancada (change
 * {@code request-resend-notice}) contra un SQL Server real (Testcontainers): verificacion de
 * pertenencia (BOLA), maquina de estados ({@code REQUEST_NOT_PENDING}) y el periodo minimo de
 * 24h desde la creacion o el ultimo reenvio ({@code RESEND_TOO_SOON}). El envio de email lo
 * verifica contra el mock de {@link BaseIntegrationTest#emailSenderPort}, reutilizando el mismo
 * evento {@code REQUEST_CREATED} que la creacion de la solicitud (un email por ADMIN activo; el
 * unico activo en el estado base es el seed {@code admin}).
 */
class RequestResendIT extends BaseIntegrationTest {

    private static final String BASE_URL = "/api/v1/requests";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String SESSION_COOKIE = "parking_SESSION";

    private static final String EMP_A_LOGIN = "ittest.resend.empa";
    private static final String EMP_B_LOGIN = "ittest.resend.empb";
    private static final String EMP_PASSWORD = "Resend#Pass1word";

    private static final LocalDate WITHIN = LocalDate.now(ZoneOffset.UTC).plusDays(3);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Cookie empASession;
    private Cookie empBSession;
    private long empAId;
    private long empBId;

    @BeforeEach
    void seed() throws Exception {
        empAId = insertEmployee(EMP_A_LOGIN);
        empBId = insertEmployee(EMP_B_LOGIN);
        empASession = login(EMP_A_LOGIN);
        empBSession = login(EMP_B_LOGIN);
    }

    @Test
    void shouldResendNotice_whenPendingAndCreatedOver24hAgo() throws Exception {
        // Arrange: PENDING propia creada hace 25h (sin reenvio previo) -> supera el periodo minimo
        long id = insertRequest(empAId, "PENDING", Instant.now().minusSeconds(25 * 3600L), null);

        // Act / Assert: 200, sigue PENDING y lastRemindedAt queda fijado
        resend(empASession, id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.lastRemindedAt").exists());
        assertThat(lastRemindedAtOf(id)).isNotNull();

        // Assert: se reutiliza el evento REQUEST_CREATED -> un email al (unico) ADMIN activo
        verify(emailSenderPort, times(1)).send(any());
    }

    @Test
    void shouldReturn403_whenResendingOtherEmployeeRequest() throws Exception {
        // Arrange: la solicitud es de B; A intenta reenviarla (BOLA)
        long id = insertRequest(empBId, "PENDING", Instant.now().minusSeconds(25 * 3600L), null);

        // Act / Assert
        resend(empASession, id)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        assertThat(lastRemindedAtOf(id)).isNull();
    }

    @Test
    void shouldReturn409_whenResendingApprovedRequest() throws Exception {
        // Arrange: la solicitud ya fue resuelta (APPROVED); no admite reenvio
        long id = insertRequest(empAId, "APPROVED", Instant.now().minusSeconds(25 * 3600L), null);

        // Act / Assert
        resend(empASession, id)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("REQUEST_NOT_PENDING"));
    }

    @Test
    void shouldReturn409_whenResendingBeforeCooldownElapsed() throws Exception {
        // Arrange: PENDING creada hace solo 1h (sin reenvio previo)
        long id = insertRequest(empAId, "PENDING", Instant.now().minusSeconds(3600L), null);

        // Act / Assert
        resend(empASession, id)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("RESEND_TOO_SOON"));
        assertThat(lastRemindedAtOf(id)).isNull();
    }

    @Test
    void shouldReturn409_whenLastReminderWasLessThan24hAgo() throws Exception {
        // Arrange: creada hace 48h, pero ya reenviada hace solo 2h (referencia = ultimo reenvio)
        long id = insertRequest(empAId, "PENDING",
                Instant.now().minusSeconds(48 * 3600L), Instant.now().minusSeconds(2 * 3600L));

        // Act / Assert
        resend(empASession, id)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("RESEND_TOO_SOON"));
    }

    @Test
    void shouldReturn404_whenResendingUnknownRequest() throws Exception {
        resend(empASession, 999999L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // ---- Helpers ----

    private ResultActions resend(Cookie session, long id) throws Exception {
        return mockMvc.perform(post(BASE_URL + "/" + id + "/resend").cookie(session));
    }

    private Cookie login(String login) throws Exception {
        Cookie cookie = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"" + login + "\",\"password\":\"" + EMP_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie(SESSION_COOKIE);
        assertThat(cookie).isNotNull();
        return cookie;
    }

    private long insertRequest(long employeeId, String status, Instant createdAt, Instant lastRemindedAt) {
        jdbcTemplate.update(
                "INSERT INTO dbo.requests (employee_id, requested_date, status, created_at, "
                        + "last_reminded_at) VALUES (?, ?, ?, ?, ?)",
                employeeId, Date.valueOf(WITHIN), status, Timestamp.from(createdAt),
                lastRemindedAt == null ? null : Timestamp.from(lastRemindedAt));
        Long id = jdbcTemplate.queryForObject(
                "SELECT TOP 1 id FROM dbo.requests WHERE employee_id = ? AND requested_date = ? "
                        + "AND status = ? ORDER BY id DESC",
                Long.class, employeeId, Date.valueOf(WITHIN), status);
        return id == null ? 0L : id;
    }

    private Instant lastRemindedAtOf(long id) {
        Timestamp value = jdbcTemplate.queryForObject(
                "SELECT last_reminded_at FROM dbo.requests WHERE id = ?", Timestamp.class, id);
        return value == null ? null : value.toInstant();
    }

    private long insertEmployee(String login) {
        jdbcTemplate.update(
                "INSERT INTO dbo.employees "
                        + "(first_name, last_name, login, email, password_hash, password_must_change, "
                        + "is_corporate, auth_origin, role, enabled, active) "
                        + "VALUES ('IT', 'User', ?, ?, ?, 0, 0, 'LOCAL', 'EMPLOYEE', 1, 1)",
                login, login + "@aleatica.com", passwordEncoder.encode(EMP_PASSWORD));
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM dbo.employees WHERE login = ?", Long.class, login);
        return id == null ? 0L : id;
    }
}
