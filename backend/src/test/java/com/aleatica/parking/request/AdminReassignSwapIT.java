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

/**
 * Tests de integracion de la reasignacion e intercambio (swap) administrativos de recurso por
 * fecha ({@code POST /requests/admin/reassign} y {@code POST /requests/admin/swap}, change
 * {@code reservas-employee-admin-reassign}, capability {@code admin-resource-reassignment}) contra
 * un SQL Server real (Testcontainers). Valida especialmente que el <strong>swap de un 2-ciclo no
 * viola el indice unico filtrado</strong> {@code UX_requests_space_date_approved}
 * ({@code resource_id, resource_type, requested_date} WHERE {@code status = 'APPROVED'}), gracias
 * al valor centinela transitorio; y que la reasignacion libera el recurso anterior y rechaza un
 * destino ocupado con {@code 409}.
 */
class AdminReassignSwapIT extends BaseIntegrationTest {

    private static final String REASSIGN_URL = "/api/v1/requests/admin/reassign";
    private static final String SWAP_URL = "/api/v1/requests/admin/swap";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String SESSION_COOKIE = "parking_SESSION";

    private static final String ADMIN_LOGIN = "admin";
    private static final String ADMIN_PASSWORD = "Admin#Parking2026";
    private static final String USER_PASSWORD = "Reassign#Pass1word";

    private static final LocalDate FUTURE = LocalDate.now(ZoneOffset.UTC).plusDays(6);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Cookie adminSession;
    private long employeeA;
    private long employeeB;

    @BeforeEach
    void seed() throws Exception {
        employeeA = insertEmployee("ittest.reassign.a");
        employeeB = insertEmployee("ittest.reassign.b");
        adminSession = login(ADMIN_LOGIN, ADMIN_PASSWORD);
    }

    // ---- Reasignacion ----

    @Test
    void shouldReassignToFreeSpaceAndFreePrevious() throws Exception {
        // Arrange: A tiene la plaza 1; la plaza 2 esta libre esa fecha
        long space1 = insertSpace("P-REASSIGN-01");
        long space2 = insertSpace("P-REASSIGN-02");
        long requestId = insertApprovedRequest(employeeA, space1, "PARKING", FUTURE);

        // Act / Assert: reasigna a la plaza 2
        mockMvc.perform(post(REASSIGN_URL).cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestId\":" + requestId + ",\"newResourceId\":" + space2 + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parkingSpaceId").value((int) space2))
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // Assert: la fila ahora ocupa la plaza 2; la 1 queda libre; auditoria con el admin
        assertThat(resourceOfRequest(requestId)).isEqualTo(space2);
        assertThat(auditCountForAction("ADMIN_REASSIGN_RESOURCE", idOfEmployee(ADMIN_LOGIN))).isEqualTo(1);
    }

    @Test
    void shouldReturn409_whenReassignTargetOccupied() throws Exception {
        // Arrange: A tiene la plaza 1; B ya ocupa la plaza 2 esa fecha
        long space1 = insertSpace("P-REASSIGN-03");
        long space2 = insertSpace("P-REASSIGN-04");
        long requestId = insertApprovedRequest(employeeA, space1, "PARKING", FUTURE);
        insertApprovedRequest(employeeB, space2, "PARKING", FUTURE);

        // Act / Assert: la plaza 2 no esta disponible -> 409, sin cambios
        mockMvc.perform(post(REASSIGN_URL).cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestId\":" + requestId + ",\"newResourceId\":" + space2 + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("SPACE_NOT_AVAILABLE"));
        assertThat(resourceOfRequest(requestId)).isEqualTo(space1);
    }

    // ---- Intercambio (swap) ----

    @Test
    void shouldSwapResources_withoutViolatingUniqueIndex() throws Exception {
        // Arrange: A ocupa la plaza 1 y B la plaza 2, misma fecha (2-ciclo)
        long space1 = insertSpace("P-SWAP-01");
        long space2 = insertSpace("P-SWAP-02");
        long requestA = insertApprovedRequest(employeeA, space1, "PARKING", FUTURE);
        long requestB = insertApprovedRequest(employeeB, space2, "PARKING", FUTURE);

        // Act / Assert: el swap no viola UX_requests_space_date_approved (centinela transitorio)
        mockMvc.perform(post(SWAP_URL).cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestIdA\":" + requestA + ",\"requestIdB\":" + requestB + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestA.parkingSpaceId").value((int) space2))
                .andExpect(jsonPath("$.requestB.parkingSpaceId").value((int) space1));

        // Assert: recursos intercambiados en BD; ambas siguen APPROVED; auditoria con el admin
        assertThat(resourceOfRequest(requestA)).isEqualTo(space2);
        assertThat(resourceOfRequest(requestB)).isEqualTo(space1);
        assertThat(auditCountForAction("ADMIN_SWAP_RESOURCES", idOfEmployee(ADMIN_LOGIN))).isEqualTo(1);
    }

    @Test
    void shouldReturn409_whenSwapDifferentDates() throws Exception {
        // Arrange: mismas plazas pero fechas distintas
        long space1 = insertSpace("P-SWAP-03");
        long space2 = insertSpace("P-SWAP-04");
        long requestA = insertApprovedRequest(employeeA, space1, "PARKING", FUTURE);
        long requestB = insertApprovedRequest(employeeB, space2, "PARKING", FUTURE.plusDays(1));

        // Act / Assert
        mockMvc.perform(post(SWAP_URL).cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestIdA\":" + requestA + ",\"requestIdB\":" + requestB + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
        // Sin cambios
        assertThat(resourceOfRequest(requestA)).isEqualTo(space1);
        assertThat(resourceOfRequest(requestB)).isEqualTo(space2);
    }

    // ---- Helpers ----

    private Cookie login(String login, String password) throws Exception {
        Cookie cookie = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"" + login + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie(SESSION_COOKIE);
        assertThat(cookie).isNotNull();
        return cookie;
    }

    private long insertEmployee(String login) {
        jdbcTemplate.update(
                "INSERT INTO dbo.employees "
                        + "(first_name, last_name, login, email, password_hash, password_must_change, "
                        + "is_corporate, auth_origin, role, enabled, active) "
                        + "VALUES ('IT', 'User', ?, ?, ?, 0, 0, 'LOCAL', 'EMPLOYEE', 1, 1)",
                login, login + "@aleatica.com", passwordEncoder.encode(USER_PASSWORD));
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

    private long insertApprovedRequest(long employeeId, long resourceId, String type, LocalDate date) {
        jdbcTemplate.update(
                "INSERT INTO dbo.requests (employee_id, requested_date, status, resource_id, "
                        + "resource_type, resolved_by_id, resolved_at, created_at) "
                        + "VALUES (?, ?, 'APPROVED', ?, ?, ?, SYSUTCDATETIME(), SYSUTCDATETIME())",
                employeeId, java.sql.Date.valueOf(date), resourceId, type, idOfEmployee(ADMIN_LOGIN));
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM dbo.requests WHERE employee_id = ? AND resource_id = ? "
                        + "AND requested_date = ? AND status = 'APPROVED'",
                Long.class, employeeId, resourceId, java.sql.Date.valueOf(date));
        return id == null ? 0L : id;
    }

    private long resourceOfRequest(long requestId) {
        Long id = jdbcTemplate.queryForObject(
                "SELECT resource_id FROM dbo.requests WHERE id = ?", Long.class, requestId);
        return id == null ? 0L : id;
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
