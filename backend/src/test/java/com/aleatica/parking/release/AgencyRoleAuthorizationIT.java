package com.aleatica.parking.release;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aleatica.parking.support.BaseIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Matriz de autorizacion del rol {@code AGENCIA} (change funcional agency-role) contra un
 * SQL Server real (Testcontainers). Verifica el doble filo del cambio RBAC:
 *
 * <ul>
 *   <li><b>Inclusion:</b> {@code AGENCIA} SI puede crear una liberacion administrativa
 *       ({@code POST /releases/administrative} -&gt; 201), igual que {@code ADMIN}.</li>
 *   <li><b>Regresion:</b> {@code ADMIN} sigue liberando (201) y {@code EMPLOYEE} sigue
 *       recibiendo 403 en ese mismo endpoint.</li>
 *   <li><b>Exclusion fail-closed:</b> {@code AGENCIA} recibe 403 en endpoints
 *       representativos de cada area admin (empleados, plazas, desks, visitantes,
 *       aprobar/rechazar solicitudes, auditoria, login-logs, calendario admin,
 *       asignaciones fijas) y del portal de empleado ({@code POST /releases},
 *       {@code GET /releases/mine}, {@code DELETE /releases/&#123;id&#125;}).</li>
 * </ul>
 *
 * <p>Los usuarios {@code AGENCIA} se siembran via JDBC con {@code role = 'AGENCIA'}, como
 * el resto de IT crean sus empleados. La limpieza FK-safe la realiza
 * {@link BaseIntegrationTest#resetDomainState}.</p>
 */
class AgencyRoleAuthorizationIT extends BaseIntegrationTest {

    private static final String RELEASES_URL = "/api/v1/releases";
    private static final String ADMIN_RELEASE_URL = RELEASES_URL + "/administrative";
    private static final String ADMIN_RELEASE_MINE_URL = ADMIN_RELEASE_URL + "/mine";
    private static final String OCCUPANCY_URL = "/api/v1/occupancy";
    private static final String REQUESTS_ADMIN_URL = "/api/v1/requests/admin";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String SESSION_COOKIE = "parking_SESSION";

    private static final String ADMIN_LOGIN = "admin";
    private static final String ADMIN_PASSWORD = "Admin#Parking2026";

    private static final String AGENCY_LOGIN = "ittest.agency.user";
    private static final String EMPLOYEE_LOGIN = "ittest.agency.emp";
    private static final String TARGET_LOGIN = "ittest.agency.target";
    private static final String USER_PASSWORD = "Agency#Pass1word";

    private static final String ROLE_AGENCIA = "AGENCIA";
    private static final String ROLE_EMPLOYEE = "EMPLOYEE";

    private static final LocalDate FUTURE = LocalDate.now(ZoneOffset.UTC).plusDays(3);
    private static final int FUTURE_DOW = FUTURE.getDayOfWeek().getValue();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Cookie adminSession;
    private Cookie agencySession;
    private Cookie employeeSession;
    private long targetId;
    private long spaceX;

    @BeforeEach
    void seed() throws Exception {
        insertEmployee(AGENCY_LOGIN, ROLE_AGENCIA);
        insertEmployee(EMPLOYEE_LOGIN, ROLE_EMPLOYEE);
        targetId = insertEmployee(TARGET_LOGIN, ROLE_EMPLOYEE);
        spaceX = insertSpace("P-AGN-01");

        adminSession = login(ADMIN_LOGIN, ADMIN_PASSWORD);
        agencySession = login(AGENCY_LOGIN, USER_PASSWORD);
        employeeSession = login(EMPLOYEE_LOGIN, USER_PASSWORD);
    }

    // ---- Inclusion: AGENCIA SI puede liberar administrativamente ----

    @Test
    void shouldCreateAdministrativeRelease_whenAgencyRoleAndReasonPresent() throws Exception {
        // Arrange: el empleado objetivo tiene asignacion fija sobre spaceX el dia de FUTURE
        insertFixedAssignment(spaceX, targetId, FUTURE_DOW);

        // Act / Assert: AGENCIA libera igual que ADMIN -> 201 ADMINISTRATIVE
        administrativeRelease(agencySession, targetId, spaceX, FUTURE, "Ausencia justificada")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("ADMINISTRATIVE"))
                .andExpect(jsonPath("$.employeeId").value((int) targetId))
                .andExpect(jsonPath("$.releasedById").value((int) idOfEmployee(AGENCY_LOGIN)))
                .andExpect(jsonPath("$.reason").value("Ausencia justificada"));
        assertThat(releasesForSpaceDate(spaceX, FUTURE)).isEqualTo(1);
    }

    // ---- Inclusion ampliada (change restructure-admin-workflows, design D5) ----

    @Test
    void shouldAllowAgency_whenQueryingOccupancyByDate() throws Exception {
        // Arrange: recurso ocupado por asignacion fija esa fecha (pivote por-fecha, solo lectura)
        insertFixedAssignment(spaceX, targetId, FUTURE_DOW);

        // Act / Assert
        mockMvc.perform(get(OCCUPANCY_URL).param("date", FUTURE.toString()).cookie(agencySession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.occupiedResources[0].resourceId").value((int) spaceX));
    }

    @Test
    void shouldAllowAgency_whenListingOwnAdministrativeReleaseHistory() throws Exception {
        // Arrange: AGENCIA libera administrativamente (queda como releasedById = agencia)
        insertFixedAssignment(spaceX, targetId, FUTURE_DOW);
        administrativeRelease(agencySession, targetId, spaceX, FUTURE, "Ausencia justificada")
                .andExpect(status().isCreated());

        // Act / Assert: su propio historial lo ve; no incluye liberaciones ajenas
        mockMvc.perform(get(ADMIN_RELEASE_MINE_URL).cookie(agencySession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("ADMINISTRATIVE"))
                .andExpect(jsonPath("$.content[0].releasedById").value((int) idOfEmployee(AGENCY_LOGIN)))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ---- Regresion: ADMIN sigue liberando, EMPLOYEE sigue excluido ----

    @Test
    void shouldStillAllowAdmin_whenCreatingAdministrativeRelease() throws Exception {
        // Arrange
        insertFixedAssignment(spaceX, targetId, FUTURE_DOW);

        // Act / Assert: la ampliacion a AGENCIA no rompe el acceso del ADMIN
        administrativeRelease(adminSession, targetId, spaceX, FUTURE, "Motivo admin")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("ADMINISTRATIVE"));
        assertThat(releasesForSpaceDate(spaceX, FUTURE)).isEqualTo(1);
    }

    @Test
    void shouldStillForbidEmployee_whenCreatingAdministrativeRelease() throws Exception {
        // Arrange
        insertFixedAssignment(spaceX, targetId, FUTURE_DOW);

        // Act / Assert: EMPLOYEE sigue sin acceso a la liberacion administrativa
        administrativeRelease(employeeSession, targetId, spaceX, FUTURE, "Motivo")
                .andExpect(status().isForbidden());
        assertThat(releasesForSpaceDate(spaceX, FUTURE)).isZero();
    }

    // ---- Exclusion fail-closed: AGENCIA -> 403 en cada area admin ----

    static Stream<String> adminOnlyGetEndpoints() {
        return Stream.of(
                "/api/v1/employees",            // empleados
                "/api/v1/parking-spaces",       // plazas de garaje
                "/api/v1/desks",                // puestos (desks)
                "/api/v1/visitors",             // visitantes
                "/api/v1/requests/pending",     // solicitudes (bandeja admin)
                "/api/v1/audit",                // auditoria
                "/api/v1/login-logs",           // registros de acceso
                "/api/v1/calendar/admin?weekStart=2026-07-13", // calendario admin (weekStart requerido para llegar al RBAC, no al 400 de binding)
                "/api/v1/fixed-assignments"     // asignaciones fijas
        );
    }

    @ParameterizedTest
    @MethodSource("adminOnlyGetEndpoints")
    void shouldReturn403_whenAgencyAccessesAdminEndpoint(String url) throws Exception {
        // Act / Assert: fail-closed -> AGENCIA no accede a ninguna otra area admin
        mockMvc.perform(get(url).cookie(agencySession))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403_whenAgencyApprovesRequest() throws Exception {
        // Act / Assert: cuerpo valido -> el 403 lo impone el RBAC, no la validacion
        mockMvc.perform(post("/api/v1/requests/1/approve").cookie(agencySession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parkingSpaceId\":" + spaceX + "}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403_whenAgencyRejectsRequest() throws Exception {
        // Act / Assert: cuerpo valido -> el 403 lo impone el RBAC, no la validacion
        mockMvc.perform(post("/api/v1/requests/1/reject").cookie(agencySession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reasonCode\":\"NO_AVAILABILITY\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403_whenAgencyUsesAdminPunctualAssignment() throws Exception {
        // Act / Assert: la asignacion puntual (capability admin-punctual-assignment) es ADMIN-only
        String body = "{\"employeeId\":" + targetId + ",\"requestedDate\":\"" + FUTURE
                + "\",\"resourceType\":\"PARKING\",\"resourceId\":" + spaceX + "}";
        mockMvc.perform(post(REQUESTS_ADMIN_URL).cookie(agencySession)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    // ---- Exclusion del portal de empleado: AGENCIA -> 403 ----

    @Test
    void shouldReturn403_whenAgencyCreatesVoluntaryRelease() throws Exception {
        // Act / Assert: cuerpo valido (fecha futura) -> el 403 lo impone el RBAC
        mockMvc.perform(post(RELEASES_URL).cookie(agencySession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"releaseDate\":\"" + FUTURE + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403_whenAgencyListsOwnReleases() throws Exception {
        // Act / Assert
        mockMvc.perform(get(RELEASES_URL + "/mine").cookie(agencySession))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403_whenAgencyCancelsRelease() throws Exception {
        // Act / Assert
        mockMvc.perform(delete(RELEASES_URL + "/1").cookie(agencySession))
                .andExpect(status().isForbidden());
    }

    // ---- Helpers ----

    private ResultActions administrativeRelease(
            Cookie session, long employeeId, long spaceId, LocalDate date, String reason) throws Exception {
        String body = "{\"employeeId\":" + employeeId + ",\"parkingSpaceId\":" + spaceId
                + ",\"releaseDate\":\"" + date + "\",\"reason\":\"" + reason + "\"}";
        return mockMvc.perform(post(ADMIN_RELEASE_URL).cookie(session)
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

    private void insertFixedAssignment(long spaceId, long employeeId, int dayOfWeek) {
        jdbcTemplate.update(
                "INSERT INTO dbo.fixed_assignments (resource_id, employee_id, day_of_week, "
                        + "active, created_by_id, created_at) VALUES (?, ?, ?, 1, ?, SYSUTCDATETIME())",
                spaceId, employeeId, dayOfWeek, idOfEmployee(ADMIN_LOGIN));
    }

    private long idOfEmployee(String login) {
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM dbo.employees WHERE login = ?", Long.class, login);
        return id == null ? 0L : id;
    }

    private int releasesForSpaceDate(long spaceId, LocalDate date) {
        Integer value = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.releases WHERE resource_id = ? AND release_date = ?",
                Integer.class, spaceId, java.sql.Date.valueOf(date));
        return value == null ? 0 : value;
    }
}
