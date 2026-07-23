package com.aleatica.parking.release;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aleatica.parking.support.BaseIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Tests de integracion del flujo "liberar por empleado/semana" (change
 * {@code admin-release-by-employee-week}) contra un SQL Server real (Testcontainers). Cubre los
 * dos endpoints de solo lectura del selector y la ampliacion RBAC de {@code admin-cancel}:
 *
 * <ul>
 *   <li><b>Inclusion:</b> {@code ADMIN} y {@code AGENCIA} obtienen el listado de empleados
 *       seleccionables y la ocupacion semanal de un empleado (plaza y puesto), y las reservas por
 *       solicitud aprobada incluyen su {@code requestId}.</li>
 *   <li><b>admin-cancel:</b> {@code ADMIN} y {@code AGENCIA} pueden cancelar administrativamente
 *       una solicitud {@code APPROVED} futura (200); {@code EMPLOYEE} recibe 403.</li>
 *   <li><b>Exclusion fail-closed:</b> {@code EMPLOYEE} recibe 403 en ambos endpoints del
 *       selector.</li>
 * </ul>
 *
 * <p>Los usuarios se siembran via JDBC como en el resto de ITs; la limpieza FK-safe la realiza
 * {@link BaseIntegrationTest#resetDomainState}.</p>
 */
class AdminReleaseByEmployeeWeekIT extends BaseIntegrationTest {

    private static final String EMPLOYEES_URL = "/api/v1/releases/employees";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String SESSION_COOKIE = "parking_SESSION";

    private static final String ADMIN_LOGIN = "admin";
    private static final String ADMIN_PASSWORD = "Admin#Parking2026";

    private static final String AGENCY_LOGIN = "ittest.week.agency";
    private static final String EMPLOYEE_LOGIN = "ittest.week.emp";
    private static final String TARGET_LOGIN = "ittest.week.target";
    private static final String USER_PASSWORD = "Week#Pass1word";

    private static final String ROLE_AGENCIA = "AGENCIA";
    private static final String ROLE_EMPLOYEE = "EMPLOYEE";

    // Lunes futuro: garantiza que la fecha APPROVED usada en admin-cancel sea futura.
    private static final LocalDate WEEK_START =
            LocalDate.now(ZoneOffset.UTC).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    private static final LocalDate WEDNESDAY = WEEK_START.plusDays(2);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Cookie adminSession;
    private Cookie agencySession;
    private Cookie employeeSession;
    private long targetId;
    private long spaceFixed;
    private long spaceRequested;

    @BeforeEach
    void seed() throws Exception {
        insertEmployee(AGENCY_LOGIN, ROLE_AGENCIA);
        insertEmployee(EMPLOYEE_LOGIN, ROLE_EMPLOYEE);
        targetId = insertEmployee(TARGET_LOGIN, ROLE_EMPLOYEE);
        spaceFixed = insertSpace("P-WK-FIX");
        spaceRequested = insertSpace("P-WK-REQ");

        // El objetivo tiene plaza fija el lunes (dow=1) y una solicitud APPROVED el miercoles.
        insertFixedAssignment(spaceFixed, targetId, WEEK_START.getDayOfWeek().getValue());

        adminSession = login(ADMIN_LOGIN, ADMIN_PASSWORD);
        agencySession = login(AGENCY_LOGIN, USER_PASSWORD);
        employeeSession = login(EMPLOYEE_LOGIN, USER_PASSWORD);
    }

    // ---- Listado de empleados seleccionables ----

    @Test
    void shouldListSelectableEmployees_whenAdmin() throws Exception {
        mockMvc.perform(get(EMPLOYEES_URL).cookie(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(hasItem((int) targetId)));
    }

    @Test
    void shouldListSelectableEmployees_whenAgency() throws Exception {
        mockMvc.perform(get(EMPLOYEES_URL).cookie(agencySession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(hasItem((int) targetId)));
    }

    @Test
    void shouldReturn403_whenEmployeeListsSelectableEmployees() throws Exception {
        mockMvc.perform(get(EMPLOYEES_URL).cookie(employeeSession))
                .andExpect(status().isForbidden());
    }

    // ---- Ocupacion semanal de un empleado ----

    @Test
    void shouldReturnWeeklyOccupancyWithRequestId_whenAdmin() throws Exception {
        long approvedId = insertApproved(targetId, WEDNESDAY, spaceRequested);

        weeklyOccupancy(adminSession, targetId, WEEK_START)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value((int) targetId))
                .andExpect(jsonPath("$.days.length()").value(7))
                // Lunes: plaza por asignacion fija (sin requestId)
                .andExpect(jsonPath("$.days[0].reservations[0].resourceType").value("PARKING"))
                .andExpect(jsonPath("$.days[0].reservations[0].resourceId").value((int) spaceFixed))
                .andExpect(jsonPath("$.days[0].reservations[0].origin").value("FIXED_ASSIGNMENT"))
                .andExpect(jsonPath("$.days[0].reservations[0].requestId").doesNotExist())
                // Miercoles: plaza por solicitud aprobada (con requestId)
                .andExpect(jsonPath("$.days[2].reservations[0].origin").value("REQUEST_APPROVED"))
                .andExpect(jsonPath("$.days[2].reservations[0].requestId").value((int) approvedId));
    }

    @Test
    void shouldReturnWeeklyOccupancy_whenAgency() throws Exception {
        long approvedId = insertApproved(targetId, WEDNESDAY, spaceRequested);

        weeklyOccupancy(agencySession, targetId, WEEK_START)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days[0].reservations[0].origin").value("FIXED_ASSIGNMENT"))
                .andExpect(jsonPath("$.days[2].reservations[0].requestId").value((int) approvedId));
    }

    @Test
    void shouldReturn403_whenEmployeeRequestsWeeklyOccupancy() throws Exception {
        weeklyOccupancy(employeeSession, targetId, WEEK_START)
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn404_whenWeeklyOccupancyEmployeeUnknown() throws Exception {
        weeklyOccupancy(adminSession, 999_999L, WEEK_START)
                .andExpect(status().isNotFound());
    }

    // ---- admin-cancel: RBAC ampliado a AGENCIA ----

    @Test
    void shouldAdminCancelApprovedRequest_whenAdmin() throws Exception {
        long approvedId = insertApproved(targetId, WEDNESDAY, spaceRequested);

        adminCancel(adminSession, approvedId, "Motivo admin")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void shouldAdminCancelApprovedRequest_whenAgency() throws Exception {
        long approvedId = insertApproved(targetId, WEDNESDAY, spaceRequested);

        adminCancel(agencySession, approvedId, "Motivo agencia")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void shouldReturn403_whenEmployeeAdminCancels() throws Exception {
        long approvedId = insertApproved(targetId, WEDNESDAY, spaceRequested);

        adminCancel(employeeSession, approvedId, "Motivo")
                .andExpect(status().isForbidden());
        assertThat(statusOf(approvedId)).isEqualTo("APPROVED");
    }

    // ---- Helpers ----

    private ResultActions weeklyOccupancy(Cookie session, long employeeId, LocalDate weekStart)
            throws Exception {
        return mockMvc.perform(get(EMPLOYEES_URL + "/" + employeeId + "/occupancy")
                .param("weekStart", weekStart.toString())
                .cookie(session));
    }

    private ResultActions adminCancel(Cookie session, long requestId, String reason) throws Exception {
        return mockMvc.perform(post("/api/v1/requests/" + requestId + "/admin-cancel")
                .cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"" + reason + "\"}"));
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

    private void insertFixedAssignment(long spaceId, long employeeId, int dayOfWeek) {
        jdbcTemplate.update(
                "INSERT INTO dbo.fixed_assignments (resource_id, employee_id, day_of_week, "
                        + "active, created_by_id, created_at) VALUES (?, ?, ?, 1, ?, SYSUTCDATETIME())",
                spaceId, employeeId, dayOfWeek, idOfEmployee(ADMIN_LOGIN));
    }

    private long insertApproved(long employeeId, LocalDate date, long spaceId) {
        jdbcTemplate.update(
                "INSERT INTO dbo.requests (employee_id, requested_date, status, resource_id, "
                        + "resource_type, resolved_by_id, resolved_at, created_at) "
                        + "VALUES (?, ?, 'APPROVED', ?, 'PARKING', ?, SYSUTCDATETIME(), SYSUTCDATETIME())",
                employeeId, java.sql.Date.valueOf(date), spaceId, idOfEmployee(ADMIN_LOGIN));
        Long id = jdbcTemplate.queryForObject(
                "SELECT TOP 1 id FROM dbo.requests WHERE employee_id = ? AND requested_date = ? "
                        + "AND status = 'APPROVED' ORDER BY id DESC",
                Long.class, employeeId, java.sql.Date.valueOf(date));
        return id == null ? 0L : id;
    }

    private String statusOf(long requestId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM dbo.requests WHERE id = ?", String.class, requestId);
    }

    private long idOfEmployee(String login) {
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM dbo.employees WHERE login = ?", Long.class, login);
        return id == null ? 0L : id;
    }
}
