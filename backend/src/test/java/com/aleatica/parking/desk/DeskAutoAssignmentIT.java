package com.aleatica.parking.desk;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aleatica.parking.support.BaseIntegrationTest;
import jakarta.servlet.http.Cookie;
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
 * Tests de integracion de la <strong>auto-asignacion de puesto por categoria</strong> (change
 * {@code desk-auto-assignment}) contra un SQL Server real (Testcontainers): la creacion de
 * solicitud {@code DESK} en modo {@code AUTOMATIC} sin {@code resourceId} auto-asigna un puesto
 * segun el rango del empleado (EXECUTIVE exclusivo de categorias altas, fallback a STANDARD) y,
 * sin ningun puesto valido libre, cae a {@code PENDING} sin exigir eleccion en el plano ni
 * responder {@code 409}.
 *
 * <p>Nota de mantenimiento: el gate final ejecuta estos tests con {@code mvn clean verify}
 * (Testcontainers); NO se ejecutan en el ciclo de desarrollo habitual de la maquina local (ver
 * {@code docs/TESTING-STRATEGY.md}).</p>
 */
class DeskAutoAssignmentIT extends BaseIntegrationTest {

    private static final String REQUESTS_URL = "/api/v1/requests";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String SESSION_COOKIE = "parking_SESSION";

    private static final String EMP_HIGH_LOGIN = "ittest.deskauto.high";
    private static final String EMP_BASE_LOGIN = "ittest.deskauto.base";
    private static final String EMP_PASSWORD = "DeskAuto#Pass1word";

    private static final LocalDate WITHIN = LocalDate.now(ZoneOffset.UTC).plusDays(3);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Cookie empHighSession;
    private Cookie empBaseSession;

    @BeforeEach
    void seed() throws Exception {
        insertEmployee(EMP_HIGH_LOGIN, "DIRECTOR_N1");
        insertEmployee(EMP_BASE_LOGIN, "EMPLEADO");
        empHighSession = login(EMP_HIGH_LOGIN, EMP_PASSWORD);
        empBaseSession = login(EMP_BASE_LOGIN, EMP_PASSWORD);
        setApprovalMode("AUTOMATIC");
    }

    @Test
    void shouldAutoAssignExecutiveDesk_whenHighCategoryAndExecutiveFree() throws Exception {
        // Arrange: un puesto EXECUTIVE y uno STANDARD libres
        long executiveId = insertDesk(21, "EXECUTIVE");
        insertDesk(22, "STANDARD");

        // Act: el empleado de rango alto solicita sin elegir puesto (resourceId ausente)
        createDeskRequest(empHighSession, WITHIN)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.parkingSpaceId").value(executiveId))
                .andExpect(jsonPath("$.resourceType").value("DESK"));
    }

    @Test
    void shouldFallBackToStandardDesk_whenHighCategoryAndNoExecutiveFree() throws Exception {
        // Arrange: solo hay un puesto STANDARD libre
        long standardId = insertDesk(23, "STANDARD");

        // Act / Assert: el alto cae a STANDARD (no hay EXECUTIVE)
        createDeskRequest(empHighSession, WITHIN)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.parkingSpaceId").value(standardId));
    }

    @Test
    void shouldAutoAssignOnlyStandardDesk_whenNotHighCategory() throws Exception {
        // Arrange: un EXECUTIVE y un STANDARD libres
        insertDesk(24, "EXECUTIVE");
        long standardId = insertDesk(25, "STANDARD");

        // Act / Assert: el no-alto nunca recibe el EXECUTIVE
        createDeskRequest(empBaseSession, WITHIN)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.parkingSpaceId").value(standardId));
    }

    @Test
    void shouldCreatePendingRequest_whenNotHighCategoryAndOnlyExecutiveFree() throws Exception {
        // Arrange: el unico puesto libre es EXECUTIVE
        insertDesk(26, "EXECUTIVE");

        // Act / Assert: el no-alto NO recibe el EXECUTIVE; la solicitud queda PENDING (sin 409)
        createDeskRequest(empBaseSession, WITHIN)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.parkingSpaceId").doesNotExist());
    }

    @Test
    void shouldCreatePendingRequest_whenAutomaticAndNoDeskFreeAtAll() throws Exception {
        // Arrange: sin ningun puesto activo -> ninguno libre

        // Act / Assert: PENDING (fallback), NO 409 ni exigencia de elegir puesto
        createDeskRequest(empHighSession, WITHIN)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    // ---- Helpers HTTP ----

    private ResultActions createDeskRequest(Cookie session, LocalDate date) throws Exception {
        String body = "{\"requestedDate\":\"" + date + "\",\"resourceType\":\"DESK\"}";
        return mockMvc.perform(post(REQUESTS_URL).cookie(session)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private void setApprovalMode(String mode) {
        jdbcTemplate.update("UPDATE dbo.system_settings SET approval_mode = ? WHERE id = 1", mode);
    }

    private Cookie login(String login, String password) throws Exception {
        return mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"" + login + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie(SESSION_COOKIE);
    }

    // ---- Helpers de siembra (jdbc) ----

    private void insertEmployee(String login, String category) {
        jdbcTemplate.update(
                "INSERT INTO dbo.employees "
                        + "(first_name, last_name, login, email, password_hash, password_must_change, "
                        + "is_corporate, auth_origin, role, category, enabled, active) "
                        + "VALUES ('IT', 'User', ?, ?, ?, 0, 0, 'LOCAL', 'EMPLOYEE', ?, 1, 1)",
                login, login + "@aleatica.com", passwordEncoder.encode(EMP_PASSWORD), category);
    }

    private long insertDesk(int number, String category) {
        jdbcTemplate.update(
                "INSERT INTO dbo.desks (number, category, coord_x, coord_y, active) "
                        + "VALUES (?, ?, 50, 50, 1)", number, category);
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM dbo.desks WHERE number = ?", Long.class, number);
        return id == null ? 0L : id;
    }
}
