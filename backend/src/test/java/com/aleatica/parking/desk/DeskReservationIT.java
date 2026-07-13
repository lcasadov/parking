package com.aleatica.parking.desk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aleatica.parking.support.BaseIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.sql.Date;
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
 * Tests de integracion del ciclo de reserva de puestos ({@code DESK}) contra un SQL Server
 * real (Testcontainers): el valor central de la capability init-desks. Verifican que los
 * puestos fluyen por asignacion fija, solicitud, liberacion y disponibilidad reutilizando
 * el modelo de recurso generico:
 * <ul>
 *   <li>1.7 una asignacion fija {@code DESK} coexiste con una {@code PARKING} del mismo
 *       empleado y dia (indices unicos por {@code resource_type});</li>
 *   <li>1.8 dos empleados no pueden compartir el mismo puesto el mismo dia (409);</li>
 *   <li>1.9 solicitud de puesto disponible en la ventana (201, tipo DESK);</li>
 *   <li>1.10 un puesto EXECUTIVE liberado vuelve a estar disponible;</li>
 *   <li>1.11 aprobar una segunda solicitud del mismo puesto ya aprobado da 409.</li>
 * </ul>
 *
 * <p>Las lecturas por rango de disponibilidad se filtran por {@code resource_type}, de modo
 * que un puesto con el mismo {@code resource_id} que una plaza no interfiera. La limpieza
 * FK-safe (incluida {@code desks}) la realiza {@link BaseIntegrationTest#resetDomainState}.</p>
 */
class DeskReservationIT extends BaseIntegrationTest {

    private static final String FIXED_URL = "/api/v1/fixed-assignments/employee/";
    private static final String REQUESTS_URL = "/api/v1/requests";
    private static final String RELEASES_URL = "/api/v1/releases";
    private static final String AVAILABILITY_URL = "/api/v1/availability";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String SESSION_COOKIE = "parking_SESSION";

    private static final String ADMIN_LOGIN = "admin";
    private static final String ADMIN_PASSWORD = "Admin#Parking2026";
    private static final String EMP_A_LOGIN = "ittest.deskres.a";
    private static final String EMP_B_LOGIN = "ittest.deskres.b";
    private static final String EMP_PASSWORD = "DeskRes#Pass1word";

    private static final LocalDate WITHIN = LocalDate.now(ZoneOffset.UTC).plusDays(3);
    private static final int WITHIN_DOW = WITHIN.getDayOfWeek().getValue();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Cookie adminSession;
    private Cookie empASession;
    private Cookie empBSession;
    private long empAId;
    private long empBId;

    @BeforeEach
    void cleanAndSeed() throws Exception {
        adminSession = login(ADMIN_LOGIN, ADMIN_PASSWORD);
        empAId = insertEmployee(EMP_A_LOGIN);
        empBId = insertEmployee(EMP_B_LOGIN);
        empASession = login(EMP_A_LOGIN, EMP_PASSWORD);
        empBSession = login(EMP_B_LOGIN, EMP_PASSWORD);
    }

    @Test
    void shouldCreateDeskFixedAssignment_whenEmployeeHasParkingAssignment() throws Exception {
        // Arrange: empleado A con asignacion fija de PLAZA para el dia WITHIN_DOW
        long spaceId = insertSpace("P-DK-01");
        setFixed(empAId, spaceId, WITHIN_DOW, "PARKING").andExpect(status().isOk());
        long deskId = insertDesk(3, "STANDARD");

        // Act: se le asigna ademas un PUESTO fijo el mismo dia -> recurso distinto, sin conflicto
        setFixed(empAId, deskId, WITHIN_DOW, "DESK")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.resourceType == 'DESK')]").exists());

        // Assert: coexisten una asignacion PARKING y una DESK activas para el empleado y dia
        assertThat(activeFixed(empAId, "PARKING", WITHIN_DOW)).isEqualTo(1);
        assertThat(activeFixed(empAId, "DESK", WITHIN_DOW)).isEqualTo(1);
    }

    @Test
    void shouldReturn409_whenAssigningSameDeskToTwoEmployeesSameDay() throws Exception {
        // Arrange: el puesto 5 asignado fijamente al empleado A el martes (dow=2)
        long deskId = insertDesk(5, "STANDARD");
        setFixed(empAId, deskId, 2, "DESK").andExpect(status().isOk());

        // Act / Assert: el mismo puesto, mismo dia, a otro empleado -> 409 (indice recurso/dia)
        setFixed(empBId, deskId, 2, "DESK")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fields.parkingSpaceId").exists());
        assertThat(activeFixed(empBId, "DESK", 2)).isZero();
    }

    @Test
    void shouldCreatePendingRequest_whenDeskAvailableInWindow() throws Exception {
        // Act: el empleado A solicita un PUESTO para una fecha dentro de la ventana
        createRequest(empASession, WITHIN, "DESK")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.resourceType").value("DESK"));

        // Assert: existe una solicitud DESK pendiente del empleado A para la fecha
        assertThat(pendingDeskRequests(empAId, WITHIN)).isEqualTo(1);
    }

    @Test
    void shouldMakeDeskAvailable_whenExecutiveReleasesIt() throws Exception {
        // Arrange: puesto EXECUTIVE con asignacion fija del directivo (A) el dia WITHIN_DOW
        long deskId = insertDesk(7, "EXECUTIVE");
        setFixed(empAId, deskId, WITHIN_DOW, "DESK").andExpect(status().isOk());
        // Antes de liberar, el puesto NO esta disponible esa fecha (asignacion fija vigente)
        deskAvailability(adminSession, WITHIN)
                .andExpect(status().isOk())
                .andExpect(jsonPath(deskFilter(deskId)).doesNotExist());

        // Act: el directivo libera voluntariamente su puesto para esa fecha
        createRelease(empASession, WITHIN, deskId, "DESK").andExpect(status().isCreated());

        // Assert: el puesto liberado vuelve a estar disponible; otro empleado puede pedirlo
        deskAvailability(adminSession, WITHIN)
                .andExpect(status().isOk())
                .andExpect(jsonPath(deskFilter(deskId)).exists());
        createRequest(empBSession, WITHIN, "DESK").andExpect(status().isCreated());
    }

    @Test
    void shouldReturn409_whenApprovingSecondRequestForAlreadyApprovedDesk() throws Exception {
        // Arrange: puesto con una solicitud DESK APPROVED para la fecha (empleado A)
        long deskId = insertDesk(9, "STANDARD");
        insertApprovedDeskRequest(empAId, deskId, WITHIN);
        // El empleado B crea una segunda solicitud DESK pendiente para la misma fecha
        long requestBId = createRequestId(empBSession, WITHIN, "DESK");

        // Act / Assert: aprobar la segunda sobre el mismo puesto/fecha -> 409 (no disponible)
        mockMvc.perform(post(REQUESTS_URL + "/" + requestBId + "/approve").cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parkingSpaceId\":" + deskId + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("SPACE_NOT_AVAILABLE"));
    }

    // ---- Helpers HTTP ----

    private ResultActions setFixed(long employeeId, long resourceId, int dow, String resourceType)
            throws Exception {
        String body = "{\"parkingSpaceId\":" + resourceId + ",\"daysOfWeek\":[" + dow
                + "],\"resourceType\":\"" + resourceType + "\"}";
        return mockMvc.perform(put(FIXED_URL + employeeId).cookie(adminSession)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions createRequest(Cookie session, LocalDate date, String resourceType)
            throws Exception {
        String body = "{\"requestedDate\":\"" + date + "\",\"resourceType\":\"" + resourceType + "\"}";
        return mockMvc.perform(post(REQUESTS_URL).cookie(session)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private long createRequestId(Cookie session, LocalDate date, String resourceType) throws Exception {
        String content = createRequest(session, date, resourceType)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(content);
        return node.get("id").asLong();
    }

    private ResultActions createRelease(Cookie session, LocalDate date, long resourceId, String resourceType)
            throws Exception {
        String body = "{\"releaseDate\":\"" + date + "\",\"parkingSpaceId\":" + resourceId
                + ",\"resourceType\":\"" + resourceType + "\"}";
        return mockMvc.perform(post(RELEASES_URL).cookie(session)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions deskAvailability(Cookie session, LocalDate date) throws Exception {
        return mockMvc.perform(get(AVAILABILITY_URL)
                .param("date", date.toString()).param("resourceType", "DESK").cookie(session));
    }

    private static String deskFilter(long deskId) {
        return "$.availableResources[?(@.parkingSpaceId == " + deskId + ")]";
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

    // ---- Helpers de siembra (jdbc) ----

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

    private long insertSpace(String label) {
        jdbcTemplate.update("INSERT INTO dbo.parking_spaces (number, label, active) VALUES ((SELECT ISNULL(MAX(number),1000)+1 FROM dbo.parking_spaces), ?, 1)", label);
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM dbo.parking_spaces WHERE label = ?", Long.class, label);
        return id == null ? 0L : id;
    }

    private long insertDesk(int number, String category) {
        jdbcTemplate.update(
                "INSERT INTO dbo.desks (number, category, coord_x, coord_y, active) "
                        + "VALUES (?, ?, 50, 50, 1)", number, category);
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM dbo.desks WHERE number = ?", Long.class, number);
        return id == null ? 0L : id;
    }

    private void insertApprovedDeskRequest(long employeeId, long deskId, LocalDate date) {
        jdbcTemplate.update(
                "INSERT INTO dbo.requests (employee_id, requested_date, status, resource_id, "
                        + "resource_type, resolved_by_id, resolved_at, created_at) "
                        + "VALUES (?, ?, 'APPROVED', ?, 'DESK', ?, ?, ?)",
                employeeId, Date.valueOf(date), deskId, idOfEmployee(ADMIN_LOGIN),
                java.sql.Timestamp.from(Instant.now()), java.sql.Timestamp.from(Instant.now()));
    }

    private long idOfEmployee(String login) {
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM dbo.employees WHERE login = ?", Long.class, login);
        return id == null ? 0L : id;
    }

    private int activeFixed(long employeeId, String resourceType, int dow) {
        Integer value = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.fixed_assignments WHERE employee_id = ? "
                        + "AND resource_type = ? AND day_of_week = ? AND active = 1",
                Integer.class, employeeId, resourceType, dow);
        return value == null ? 0 : value;
    }

    private int pendingDeskRequests(long employeeId, LocalDate date) {
        Integer value = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.requests WHERE employee_id = ? AND requested_date = ? "
                        + "AND resource_type = 'DESK' AND status = 'PENDING'",
                Integer.class, employeeId, Date.valueOf(date));
        return value == null ? 0 : value;
    }
}
