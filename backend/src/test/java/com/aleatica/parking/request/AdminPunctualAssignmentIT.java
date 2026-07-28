package com.aleatica.parking.request;

import static org.assertj.core.api.Assertions.assertThat;
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
 * Tests de integracion de la asignacion puntual del admin ({@code POST /requests/admin}, change
 * {@code restructure-admin-workflows}, capability {@code admin-punctual-assignment}) contra un
 * SQL Server real (Testcontainers): camino feliz de plaza y puesto elegidos, auto-asignacion de
 * plaza, {@code 409} por recurso ya ocupado, {@code 409 NO_AVAILABILITY} sin plazas libres,
 * {@code 403} para {@code EMPLOYEE}, y la traza en {@code audit_log} con el admin como actor.
 */
class AdminPunctualAssignmentIT extends BaseIntegrationTest {

    private static final String ADMIN_ASSIGN_URL = "/api/v1/requests/admin";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String SESSION_COOKIE = "parking_SESSION";

    private static final String ADMIN_LOGIN = "admin";
    private static final String ADMIN_PASSWORD = "Admin#Parking2026";

    private static final String EMPLOYEE_LOGIN = "ittest.punctual.emp";
    private static final String TARGET_LOGIN = "ittest.punctual.target";
    private static final String USER_PASSWORD = "Punctual#Pass1word";

    private static final LocalDate FUTURE = LocalDate.now(ZoneOffset.UTC).plusDays(5);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Cookie adminSession;
    private Cookie employeeSession;
    private long targetId;

    @BeforeEach
    void seed() throws Exception {
        insertEmployee(EMPLOYEE_LOGIN, "EMPLOYEE");
        targetId = insertEmployee(TARGET_LOGIN, "EMPLOYEE");

        adminSession = login(ADMIN_LOGIN, ADMIN_PASSWORD);
        employeeSession = login(EMPLOYEE_LOGIN, USER_PASSWORD);
    }

    // ---- Camino feliz: plaza elegida ----

    @Test
    void shouldCreateApprovedParkingAssignment_whenAdminChoosesSpace() throws Exception {
        // Arrange
        long spaceId = insertSpace("P-PUNCT-01");

        // Act / Assert
        adminAssign(adminSession, targetId, "PARKING", spaceId, FUTURE)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.employeeId").value((int) targetId))
                .andExpect(jsonPath("$.parkingSpaceId").value((int) spaceId))
                .andExpect(jsonPath("$.resolvedById").value((int) idOfEmployee(ADMIN_LOGIN)))
                .andExpect(jsonPath("$.resourceType").value("PARKING"));

        // Assert: auditoria con el admin como actor
        assertThat(auditCountForAction("ADMIN_PUNCTUAL_ASSIGNMENT", idOfEmployee(ADMIN_LOGIN))).isEqualTo(1);
    }

    // ---- Camino feliz: puesto elegido ----

    @Test
    void shouldCreateApprovedDeskAssignment_whenAdminChoosesDesk() throws Exception {
        // Arrange
        long deskId = insertDesk(12);

        // Act / Assert
        adminAssign(adminSession, targetId, "DESK", deskId, FUTURE)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.parkingSpaceId").value((int) deskId))
                .andExpect(jsonPath("$.resourceType").value("DESK"));
        assertThat(auditCountForAction("ADMIN_PUNCTUAL_ASSIGNMENT", idOfEmployee(ADMIN_LOGIN))).isEqualTo(1);
    }

    // ---- Auto-asignacion de plaza ----

    @Test
    void shouldAutoAssignFreeParkingSpace_whenResourceIdOmitted() throws Exception {
        // Arrange: una unica plaza libre
        long spaceId = insertSpace("P-PUNCT-02");
        String body = "{\"employeeId\":" + targetId + ",\"requestedDate\":\"" + FUTURE + "\"}";

        // Act / Assert
        mockMvc.perform(post(ADMIN_ASSIGN_URL).cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.parkingSpaceId").value((int) spaceId));
    }

    @Test
    void shouldReturn409NoAvailability_whenAutoAssignAndNoFreeSpace() throws Exception {
        // Arrange: sin plazas en absoluto -> no hay ninguna libre
        String body = "{\"employeeId\":" + targetId + ",\"requestedDate\":\"" + FUTURE + "\"}";

        // Act / Assert
        mockMvc.perform(post(ADMIN_ASSIGN_URL).cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("NO_AVAILABILITY"));
        assertThat(auditCountForAction("ADMIN_PUNCTUAL_ASSIGNMENT", idOfEmployee(ADMIN_LOGIN))).isZero();
    }

    // ---- 409 recurso ya ocupado ----

    @Test
    void shouldReturn409_whenChosenSpaceAlreadyOccupiedByApprovedRequest() throws Exception {
        // Arrange: la plaza ya la ocupa otra solicitud APPROVED esa fecha
        long spaceId = insertSpace("P-PUNCT-03");
        long otherEmployeeId = insertEmployee("ittest.punctual.other", "EMPLOYEE");
        insertApprovedRequest(otherEmployeeId, spaceId, FUTURE);

        // Act / Assert
        adminAssign(adminSession, targetId, "PARKING", spaceId, FUTURE)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("SPACE_NOT_AVAILABLE"));
    }

    // ---- 403 EMPLOYEE ----

    @Test
    void shouldReturn403_whenEmployeeUsesAdminAssign() throws Exception {
        // Arrange
        long spaceId = insertSpace("P-PUNCT-04");

        // Act / Assert
        adminAssign(employeeSession, targetId, "PARKING", spaceId, FUTURE)
                .andExpect(status().isForbidden());
    }

    // ---- Helpers ----

    private ResultActions adminAssign(
            Cookie session, long employeeId, String resourceType, long resourceId, LocalDate date) throws Exception {
        String body = "{\"employeeId\":" + employeeId + ",\"requestedDate\":\"" + date
                + "\",\"resourceType\":\"" + resourceType + "\",\"resourceId\":" + resourceId + "}";
        return mockMvc.perform(post(ADMIN_ASSIGN_URL).cookie(session)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private Cookie login(String login, String password) throws Exception {
        Cookie cookie = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"" + login + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie(SESSION_COOKIE);
        assertThat(cookie).isNotNull();
        return cookie;
    }

    private long insertEmployee(String login, String role) {
        jdbcTemplate.update(
                "INSERT INTO dbo.employees "
                        + "(first_name, last_name, login, email, password_hash, password_must_change, "
                        + "is_corporate, auth_origin, role, enabled, active) "
                        + "VALUES ('IT', 'User', ?, ?, ?, 0, 0, 'LOCAL', ?, 1, 1)",
                login, login + "@aleatica.com", passwordEncoder.encode(USER_PASSWORD), role);
        return idOfEmployee(login);
    }

    private long insertSpace(String label) {
        jdbcTemplate.update(
                "INSERT INTO dbo.parking_spaces (number, label, active) "
                        + "VALUES ((SELECT ISNULL(MAX(number),1000)+1 FROM dbo.parking_spaces), ?, 1)",
                label);
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM dbo.parking_spaces WHERE label = ?", Long.class, label);
        return id == null ? 0L : id;
    }

    private long insertDesk(int number) {
        jdbcTemplate.update(
                "INSERT INTO dbo.desks (number, category, active) VALUES (?, 'STANDARD', 1)", number);
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM dbo.desks WHERE number = ?", Long.class, number);
        return id == null ? 0L : id;
    }

    private void insertApprovedRequest(long employeeId, long resourceId, LocalDate date) {
        jdbcTemplate.update(
                "INSERT INTO dbo.requests (employee_id, requested_date, status, resource_id, "
                        + "resource_type, resolved_by_id, resolved_at, created_at) "
                        + "VALUES (?, ?, 'APPROVED', ?, 'PARKING', ?, SYSUTCDATETIME(), SYSUTCDATETIME())",
                employeeId, java.sql.Date.valueOf(date), resourceId, idOfEmployee(ADMIN_LOGIN));
    }

    private long idOfEmployee(String login) {
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM dbo.employees WHERE login = ?", Long.class, login);
        return id == null ? 0L : id;
    }

    private int auditCountForAction(String action, long actorEmployeeId) {
        Integer value = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.audit_log WHERE action = ? AND actor_employee_id = ?",
                Integer.class, action, actorEmployeeId);
        return value == null ? 0 : value;
    }
}
