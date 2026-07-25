package com.aleatica.parking.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aleatica.parking.support.BaseIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Tests de integracion de la <strong>lista de espera de solicitudes</strong> (change
 * {@code waitlist-requests}) contra un SQL Server real (Testcontainers): opt-in en modo
 * AUTOMATIC (409 clasico vs PENDING waitlisted), computo automatico en modo MANUAL, promocion
 * de punta a punta al liberarse un recurso (cancelacion de una APPROVED) y colas separadas por
 * tipo de recurso (plaza vs puesto).
 *
 * <p>Nota de mantenimiento: el gate final ejecuta estos tests con
 * {@code mvn clean verify} (Testcontainers); NO se ejecutan en el ciclo de desarrollo habitual
 * de la maquina local (ver {@code docs/TESTING-STRATEGY.md}).</p>
 */
class WaitlistRequestsIT extends BaseIntegrationTest {

    private static final String BASE_URL = "/api/v1/requests";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String SESSION_COOKIE = "parking_SESSION";

    private static final String ADMIN_LOGIN = "admin";
    private static final String ADMIN_PASSWORD = "Admin#Parking2026";

    private static final String EMP_A_LOGIN = "ittest.wl.empa";
    private static final String EMP_B_LOGIN = "ittest.wl.empb";
    private static final String EMP_PASSWORD = "Waitlist#Pass1word";

    private static final LocalDate TODAY = LocalDate.now(ZoneOffset.UTC);
    private static final LocalDate WITHIN = TODAY.plusDays(3);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Cookie adminSession;
    private Cookie empASession;
    private Cookie empBSession;
    private long empAId;
    private long empBId;
    private long spaceX;

    @BeforeEach
    void seed() throws Exception {
        empAId = insertEmployee(EMP_A_LOGIN);
        empBId = insertEmployee(EMP_B_LOGIN);
        adminSession = login(ADMIN_LOGIN, ADMIN_PASSWORD);
        empASession = login(EMP_A_LOGIN, EMP_PASSWORD);
        empBSession = login(EMP_B_LOGIN, EMP_PASSWORD);
        spaceX = insertSpace("P-WL-01");
    }

    // ---- Creacion: opt-in en AUTOMATIC (409 clasico vs waitlisted) ----

    @Test
    void shouldCreateWaitlistedPending_whenAutomaticNoSpaceAndOptIn() throws Exception {
        // Arrange: modo AUTOMATIC, unica plaza ya ocupada por B
        setApprovalMode("AUTOMATIC");
        createRequest(empBSession, WITHIN, null, null).andExpect(status().isCreated());

        // Act / Assert: A opta por la lista de espera -> 201 PENDING waitlisted (NO 409)
        createRequest(empASession, WITHIN, null, true)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.waitlisted").value(true))
                .andExpect(jsonPath("$.parkingSpaceId").doesNotExist());
        assertThat(waitlistedOf(empAId, WITHIN, "PARKING")).isTrue();
    }

    @Test
    void shouldReturn409_whenAutomaticNoSpaceWithoutOptIn() throws Exception {
        // Arrange: mismo escenario, SIN opt-in -> se preserva el 409 clasico (compatibilidad)
        setApprovalMode("AUTOMATIC");
        createRequest(empBSession, WITHIN, null, null).andExpect(status().isCreated());

        // Act / Assert
        createRequest(empASession, WITHIN, null, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("NO_AVAILABILITY"));
        assertThat(countRequests(empAId)).isZero();
    }

    // ---- Creacion: computo automatico en MANUAL (sin opt-in) ----

    @Test
    void shouldMarkWaitlisted_whenManualCreationHasNoAvailability() throws Exception {
        // Arrange: modo MANUAL (por defecto); B ya ocupa la unica plaza (APPROVED)
        insertApproved(empBId, WITHIN, spaceX);

        // Act / Assert: A crea sin opt-in -> PENDING waitlisted = true (computado, no pedido)
        createRequest(empASession, WITHIN, null, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.waitlisted").value(true));
    }

    @Test
    void shouldNotMarkWaitlisted_whenManualCreationHasAvailability() throws Exception {
        // Act / Assert: modo MANUAL, la plaza esta libre -> PENDING sin marca de espera
        createRequest(empASession, WITHIN, null, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.waitlisted").value(false));
    }

    // ---- Promocion de punta a punta (AUTOMATIC): cancelacion de una APPROVED libera y promueve ----

    @Test
    void shouldPromoteWaitlistedRequest_whenApprovedRequestCancelled() throws Exception {
        // Arrange: AUTOMATIC, B ocupa la unica plaza; A se apunta a la lista de espera
        setApprovalMode("AUTOMATIC");
        ResultActions approvedB = createRequest(empBSession, WITHIN, null, null)
                .andExpect(status().isCreated());
        long approvedRequestId = extractId(approvedB);
        createRequest(empASession, WITHIN, null, true)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.waitlisted").value(true));

        // Act: B cancela su APPROVED -> libera la plaza -> AFTER_COMMIT promociona a A
        mockMvc.perform(post(BASE_URL + "/" + approvedRequestId + "/cancel").cookie(empBSession))
                .andExpect(status().isOk());

        // Assert: A queda APPROVED con la plaza liberada, sin intervencion del ADMIN
        assertThat(statusOf(empAId, WITHIN, "PARKING")).isEqualTo("APPROVED");
        assertThat(resourceIdOf(empAId, WITHIN, "PARKING")).isEqualTo(spaceX);
    }

    // ---- Colas separadas por tipo de recurso ----

    @Test
    void shouldKeepSeparateQueues_whenPromotingParkingDoesNotAffectDeskWaitlist() throws Exception {
        // Arrange: AUTOMATIC; B ocupa la unica plaza; A espera PLAZA y espera PUESTO (sin puestos
        // activos, por lo que la solicitud de puesto tambien cae en manual/PENDING sin recurso;
        // se marca waitlisted a mano para simular la cola de puesto sin depender de la
        // auto-asignacion de puestos, que design declara fuera de alcance en la creacion).
        setApprovalMode("AUTOMATIC");
        ResultActions approvedB = createRequest(empBSession, WITHIN, null, null)
                .andExpect(status().isCreated());
        long approvedRequestId = extractId(approvedB);
        createRequest(empASession, WITHIN, null, true)
                .andExpect(status().isCreated());
        long deskWaitlistedId = insertWaitlistedDeskRequest(empAId, WITHIN);

        // Act: se libera la PLAZA (cancelacion de B)
        mockMvc.perform(post(BASE_URL + "/" + approvedRequestId + "/cancel").cookie(empBSession))
                .andExpect(status().isOk());

        // Assert: se promueve la cola de PLAZA; la cola de PUESTO permanece intacta (colas
        // separadas por tipo de recurso, sin cruzarse)
        assertThat(statusOf(empAId, WITHIN, "PARKING")).isEqualTo("APPROVED");
        assertThat(statusOf(deskWaitlistedId)).isEqualTo("PENDING");
        assertThat(waitlistedOf(empAId, WITHIN, "DESK")).isTrue();
    }

    // ---- Helpers ----

    private ResultActions createRequest(
            Cookie session, LocalDate date, String resourceType, Boolean waitlist) throws Exception {
        StringBuilder body = new StringBuilder("{\"requestedDate\":\"").append(date).append('"');
        if (resourceType != null) {
            body.append(",\"resourceType\":\"").append(resourceType).append('"');
        }
        if (waitlist != null) {
            body.append(",\"waitlist\":").append(waitlist);
        }
        body.append('}');
        return mockMvc.perform(post(BASE_URL).cookie(session)
                .contentType(MediaType.APPLICATION_JSON).content(body.toString()));
    }

    private long extractId(ResultActions result) throws Exception {
        String json = result.andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }

    private void setApprovalMode(String mode) {
        jdbcTemplate.update("UPDATE dbo.system_settings SET approval_mode = ? WHERE id = 1", mode);
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

    private void insertApproved(long employeeId, LocalDate date, long spaceId) {
        jdbcTemplate.update(
                "INSERT INTO dbo.requests (employee_id, requested_date, status, resource_id, "
                        + "resource_type, resolved_by_id, resolved_at, created_at) "
                        + "VALUES (?, ?, 'APPROVED', ?, 'PARKING', ?, ?, ?)",
                employeeId, Date.valueOf(date), spaceId, idOfEmployee(ADMIN_LOGIN),
                Timestamp.from(Instant.now()), Timestamp.from(Instant.now()));
    }

    /**
     * Siembra directamente una solicitud PENDING de PUESTO marcada {@code waitlisted} (sin pasar
     * por la API, que no auto-asigna puestos en la creacion, design non-goal): representa una
     * candidata en espera de la cola de PUESTO independiente de la de PLAZA.
     */
    private long insertWaitlistedDeskRequest(long employeeId, LocalDate date) {
        jdbcTemplate.update(
                "INSERT INTO dbo.requests (employee_id, requested_date, status, resource_type, "
                        + "waitlisted, created_at) VALUES (?, ?, 'PENDING', 'DESK', 1, ?)",
                employeeId, Date.valueOf(date), Timestamp.from(Instant.now()));
        Long id = jdbcTemplate.queryForObject(
                "SELECT TOP 1 id FROM dbo.requests WHERE employee_id = ? AND requested_date = ? "
                        + "AND resource_type = 'DESK' ORDER BY id DESC",
                Long.class, employeeId, Date.valueOf(date));
        return id == null ? 0L : id;
    }

    private long insertEmployee(String login) {
        jdbcTemplate.update(
                "INSERT INTO dbo.employees "
                        + "(first_name, last_name, login, email, password_hash, password_must_change, "
                        + "is_corporate, auth_origin, role, enabled, active) "
                        + "VALUES ('IT', 'User', ?, ?, ?, 0, 0, 'LOCAL', 'EMPLOYEE', 1, 1)",
                login, login + "@aleatica.com", passwordEncoder.encode(EMP_PASSWORD));
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

    private long idOfEmployee(String login) {
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM dbo.employees WHERE login = ?", Long.class, login);
        return id == null ? 0L : id;
    }

    private String statusOf(long employeeId, LocalDate date, String resourceType) {
        return jdbcTemplate.queryForObject(
                "SELECT TOP 1 status FROM dbo.requests WHERE employee_id = ? AND requested_date = ? "
                        + "AND resource_type = ? ORDER BY id DESC",
                String.class, employeeId, Date.valueOf(date), resourceType);
    }

    private String statusOf(long requestId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM dbo.requests WHERE id = ?", String.class, requestId);
    }

    private boolean waitlistedOf(long employeeId, LocalDate date, String resourceType) {
        Boolean waitlisted = jdbcTemplate.queryForObject(
                "SELECT TOP 1 waitlisted FROM dbo.requests WHERE employee_id = ? AND requested_date = ? "
                        + "AND resource_type = ? ORDER BY id DESC",
                Boolean.class, employeeId, Date.valueOf(date), resourceType);
        return Boolean.TRUE.equals(waitlisted);
    }

    private long resourceIdOf(long employeeId, LocalDate date, String resourceType) {
        Long resourceId = jdbcTemplate.queryForObject(
                "SELECT TOP 1 resource_id FROM dbo.requests WHERE employee_id = ? AND requested_date = ? "
                        + "AND resource_type = ? ORDER BY id DESC",
                Long.class, employeeId, Date.valueOf(date), resourceType);
        return resourceId == null ? 0L : resourceId;
    }

    private int countRequests(long employeeId) {
        Integer value = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.requests WHERE employee_id = ?", Integer.class, employeeId);
        return value == null ? 0 : value;
    }
}
