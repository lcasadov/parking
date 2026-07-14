package com.aleatica.parking.floorplan;

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
 * Tests de integracion del plano interactivo de puestos (capability {@code floor-plan}) contra
 * un SQL Server real (Testcontainers). Cubren cada escenario BDD de
 * {@code openspec/changes/init-floor-plan/specs/floor-plan/spec.md}: proyeccion de estados por
 * fecha, ventana de solicitud, privacidad (MINE vs ASSIGNED sin fuga de titulares), solicitud
 * directa desde el plano (libre / no libre / duplicada / concurrencia), liberacion (RELEASED) y
 * edicion de posiciones (persistencia / validacion / autorizacion).
 *
 * <p>Los tests son ORDER-INDEPENDENT: {@link BaseIntegrationTest#resetDomainState} limpia el
 * estado (incluidos los 65 puestos del seed dev) antes de cada test, y cada test siembra e
 * identifica sus propios puestos por {@code deskId}/{@code deskNumber} (nunca asume el primer
 * elemento). Deben pasar en orden natural y reverse-alphabetical.</p>
 */
class FloorPlanIT extends BaseIntegrationTest {

    private static final String FLOOR_PLAN_URL = "/api/v1/floor-plan";
    private static final String FIXED_URL = "/api/v1/fixed-assignments/employee/";
    private static final String RELEASES_URL = "/api/v1/releases";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String SESSION_COOKIE = "parking_SESSION";
    private static final String STATE = "state";

    private static final String ADMIN_LOGIN = "admin";
    private static final String ADMIN_PASSWORD = "Admin#Parking2026";
    private static final String EMP_A_LOGIN = "ittest.floorplan.a";
    private static final String EMP_B_LOGIN = "ittest.floorplan.b";
    private static final String EMP_PASSWORD = "FloorPlan#Pass1word";

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

    // ---- Requirement 1: vista del plano por fecha ----

    @Test
    void shouldReturnDeskStatesAndPositions_whenDateIsValid() throws Exception {
        // Arrange: un puesto libre (STANDARD) y uno EXECUTIVE asignado fijamente a A el dia WITHIN
        long freeDesk = insertDesk(10, "STANDARD", 10, 20);
        long execDesk = insertDesk(11, "EXECUTIVE", 30, 40);
        setFixed(empAId, execDesk, WITHIN_DOW).andExpect(status().isOk());

        // Act
        String body = getFloorPlan(empASession, WITHIN)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert: cada puesto trae posicion, categoria y estado
        JsonNode free = deskNode(body, freeDesk);
        assertThat(free.get(STATE).asText()).isEqualTo("FREE");
        assertThat(free.get("coordX").asDouble()).isEqualTo(10.0);
        assertThat(free.get("coordY").asDouble()).isEqualTo(20.0);
        assertThat(free.get("deskNumber").asInt()).isEqualTo(10);
        JsonNode exec = deskNode(body, execDesk);
        assertThat(exec.get(STATE).asText()).isEqualTo("MINE");
        assertThat(exec.get("category").asText()).isEqualTo("EXECUTIVE");
    }

    @Test
    void shouldReturn400OutsideRequestWindow_whenDateIsPastOrBeyond14d() throws Exception {
        // Fecha pasada
        getFloorPlan(empASession, LocalDate.now(ZoneOffset.UTC).minusDays(1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("OUTSIDE_REQUEST_WINDOW"));
        // Fecha posterior a hoy+14
        getFloorPlan(empASession, LocalDate.now(ZoneOffset.UTC).plusDays(15))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("OUTSIDE_REQUEST_WINDOW"));
    }

    @Test
    void shouldNotRevealOtherHolders_whenMarkingOwnDeskAsMine() throws Exception {
        // Arrange: puesto de A (fijo) y puesto de B (solicitud aprobada) para la fecha
        long deskOfA = insertDesk(20, "STANDARD", 15, 15);
        long deskOfB = insertDesk(21, "STANDARD", 25, 25);
        setFixed(empAId, deskOfA, WITHIN_DOW).andExpect(status().isOk());
        insertApprovedDeskRequest(empBId, deskOfB, WITHIN);

        // Act: A consulta el plano
        String body = getFloorPlan(empASession, WITHIN)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert: su puesto es MINE; el de B es ASSIGNED sin ningun dato del titular
        assertThat(deskNode(body, deskOfA).get(STATE).asText()).isEqualTo("MINE");
        JsonNode assigned = deskNode(body, deskOfB);
        assertThat(assigned.get(STATE).asText()).isEqualTo("ASSIGNED");
        assertThat(assigned.has("employeeId")).isFalse();
        assertThat(assigned.has("holderName")).isFalse();
        assertThat(assigned.has("employeeName")).isFalse();
    }

    @Test
    void shouldShowReleasedAndAllowRequest_whenFixedDeskIsReleased() throws Exception {
        // Arrange: puesto EXECUTIVE fijo de A el dia WITHIN, liberado por A esa fecha
        long deskId = insertDesk(30, "EXECUTIVE", 60, 60);
        setFixed(empAId, deskId, WITHIN_DOW).andExpect(status().isOk());
        createRelease(empASession, deskId, WITHIN).andExpect(status().isCreated());

        // Act / Assert: el plano lo pinta RELEASED y otro empleado puede solicitarlo
        String body = getFloorPlan(empBSession, WITHIN)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(deskNode(body, deskId).get(STATE).asText()).isEqualTo("RELEASED");
        requestDesk(empBSession, deskId, WITHIN).andExpect(status().isCreated());
    }

    // ---- Requirement 2: solicitud directa desde el plano ----

    @Test
    void shouldCreateRequest_whenClickingAFreeDesk() throws Exception {
        // Arrange
        long deskId = insertDesk(40, "STANDARD", 5, 5);

        // Act
        requestDesk(empASession, deskId, WITHIN)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requestId").isNumber())
                .andExpect(jsonPath("$.deskId").value(deskId))
                .andExpect(jsonPath("$.state").value("MINE"));

        // Assert: existe una solicitud DESK pendiente puesto-especifica de A para la fecha
        assertThat(pendingDeskRequests(deskId, WITHIN)).isEqualTo(1);
        // El plano ahora lo marca MINE para A
        String body = getFloorPlan(empASession, WITHIN).andReturn().getResponse().getContentAsString();
        assertThat(deskNode(body, deskId).get(STATE).asText()).isEqualTo("MINE");
    }

    @Test
    void shouldReturn409_whenRequestingANonFreeDesk() throws Exception {
        // Arrange: puesto fijo de A el dia WITHIN (no libre para B)
        long deskId = insertDesk(41, "STANDARD", 5, 5);
        setFixed(empAId, deskId, WITHIN_DOW).andExpect(status().isOk());

        // Act / Assert: B intenta solicitarlo -> 409 disponibilidad
        requestDesk(empBSession, deskId, WITHIN)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("SPACE_NOT_AVAILABLE"));
        assertThat(pendingDeskRequests(deskId, WITHIN)).isZero();
    }

    @Test
    void shouldReturn409RequestAlreadyPending_whenDuplicatePendingForSameDate() throws Exception {
        // Arrange: A ya tiene una solicitud de puesto pendiente para la fecha
        long firstDesk = insertDesk(42, "STANDARD", 5, 5);
        long secondDesk = insertDesk(43, "STANDARD", 6, 6);
        requestDesk(empASession, firstDesk, WITHIN).andExpect(status().isCreated());

        // Act / Assert: solicitar otro puesto la misma fecha -> 409 (una pendiente por empleado/fecha)
        requestDesk(empASession, secondDesk, WITHIN)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("REQUEST_ALREADY_PENDING"));
        assertThat(pendingDeskRequests(secondDesk, WITHIN)).isZero();
    }

    @Test
    void shouldReturn409_whenTwoEmployeesRequestSameFreeDeskConcurrently() throws Exception {
        // Arrange: un puesto libre; A lo solicita primero (gana)
        long deskId = insertDesk(44, "STANDARD", 5, 5);
        requestDesk(empASession, deskId, WITHIN).andExpect(status().isCreated());

        // Act / Assert: B pincha el mismo puesto -> 409 (ya no esta libre, REQUESTED)
        requestDesk(empBSession, deskId, WITHIN)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("SPACE_NOT_AVAILABLE"));
        // Solo la solicitud de A (una) sobrevive para ese puesto/fecha
        assertThat(pendingDeskRequests(deskId, WITHIN)).isEqualTo(1);
        // El plano marca el puesto REQUESTED para B (tercero, sin titular)
        String body = getFloorPlan(empBSession, WITHIN).andReturn().getResponse().getContentAsString();
        assertThat(deskNode(body, deskId).get(STATE).asText()).isEqualTo("REQUESTED");
    }

    @Test
    void shouldReturn404_whenRequestingUnknownDesk() throws Exception {
        requestDesk(empASession, 999999L, WITHIN)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void shouldCreatePendingRequest_whenClickingAFreeDeskInManualMode() throws Exception {
        // Arrange: modo por defecto MANUAL (garantizado por resetDomainState)
        long deskId = insertDesk(45, "STANDARD", 7, 7);

        // Act / Assert: la solicitud nace PENDING (status en el cuerpo)
        requestDesk(empASession, deskId, WITHIN)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.state").value("MINE"))
                .andExpect(jsonPath("$.status").value("PENDING"));
        assertThat(approvedDeskRequests(deskId, WITHIN)).isZero();
    }

    @Test
    void shouldAutoApproveDesk_whenClickingAFreeDeskInAutomaticMode() throws Exception {
        // Arrange: conmutar el modo global a AUTOMATIC
        setApprovalMode("AUTOMATIC");
        long deskId = insertDesk(46, "STANDARD", 8, 8);

        // Act / Assert: el puesto pinchado se auto-aprueba (nace APPROVED con el puesto asignado)
        requestDesk(empASession, deskId, WITHIN)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deskId").value(deskId))
                .andExpect(jsonPath("$.state").value("MINE"))
                .andExpect(jsonPath("$.status").value("APPROVED"));
        // Existe una unica solicitud DESK APPROVED de A para ese puesto/fecha, sin resolutor humano
        assertThat(approvedDeskRequests(deskId, WITHIN)).isEqualTo(1);
        assertThat(pendingDeskRequests(deskId, WITHIN)).isZero();
        // El plano lo marca MINE para A (lo tiene asignado)
        String body = getFloorPlan(empASession, WITHIN).andReturn().getResponse().getContentAsString();
        assertThat(deskNode(body, deskId).get(STATE).asText()).isEqualTo("MINE");
    }

    @Test
    void shouldReturn409ForSecondAutoApproval_whenTwoEmployeesClickSameDeskInAutomaticMode() throws Exception {
        // Arrange: modo AUTOMATIC; A auto-aprueba primero el puesto libre (gana)
        setApprovalMode("AUTOMATIC");
        long deskId = insertDesk(47, "STANDARD", 9, 9);
        requestDesk(empASession, deskId, WITHIN)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // Act / Assert: B pincha el mismo puesto -> 409 (ya asignado; la 2a auto-aprobacion no procede)
        requestDesk(empBSession, deskId, WITHIN)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("SPACE_NOT_AVAILABLE"));
        // Solo la solicitud de A sobrevive APPROVED para ese puesto/fecha (indice unico filtrado)
        assertThat(approvedDeskRequests(deskId, WITHIN)).isEqualTo(1);
    }

    // ---- Requirement 3: edicion de posiciones ----

    @Test
    void shouldPersistCoordinates_whenAdminUpdatesDeskPosition() throws Exception {
        // Arrange
        long deskId = insertDesk(50, "STANDARD", 10, 10);

        // Act
        putPosition(adminSession, deskId, "72.5", "18.0").andExpect(status().isNoContent());

        // Assert: las coordenadas quedan persistidas
        assertThat(coordX(deskId)).isEqualByComparingTo("72.50");
        assertThat(coordY(deskId)).isEqualByComparingTo("18.00");
    }

    @Test
    void shouldReturn400_whenCoordinatesOutOfRange() throws Exception {
        // Arrange
        long deskId = insertDesk(51, "STANDARD", 10, 10);

        // Act / Assert: coordX fuera de 0-100 -> 400 sin modificar el puesto
        putPosition(adminSession, deskId, "150", "18.0")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.coordX").exists());
        assertThat(coordX(deskId)).isEqualByComparingTo("10.00");
    }

    @Test
    void shouldReturn403_whenEmployeeUpdatesDeskPosition() throws Exception {
        // Arrange
        long deskId = insertDesk(52, "STANDARD", 10, 10);

        // Act / Assert: un EMPLOYEE no puede editar posiciones -> 403, puesto intacto
        putPosition(empASession, deskId, "72.5", "18.0")
                .andExpect(status().isForbidden());
        assertThat(coordX(deskId)).isEqualByComparingTo("10.00");
    }

    // ---- Helpers HTTP ----

    private ResultActions getFloorPlan(Cookie session, LocalDate date) throws Exception {
        return mockMvc.perform(get(FLOOR_PLAN_URL).param("date", date.toString()).cookie(session));
    }

    private ResultActions requestDesk(Cookie session, long deskId, LocalDate date) throws Exception {
        String body = "{\"date\":\"" + date + "\"}";
        return mockMvc.perform(post(FLOOR_PLAN_URL + "/desks/" + deskId + "/request").cookie(session)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions putPosition(Cookie session, long deskId, String coordX, String coordY)
            throws Exception {
        String body = "{\"coordX\":" + coordX + ",\"coordY\":" + coordY + "}";
        return mockMvc.perform(put(FLOOR_PLAN_URL + "/desks/" + deskId + "/position").cookie(session)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions setFixed(long employeeId, long deskId, int dow) throws Exception {
        String body = "{\"parkingSpaceId\":" + deskId + ",\"daysOfWeek\":[" + dow
                + "],\"resourceType\":\"DESK\"}";
        return mockMvc.perform(put(FIXED_URL + employeeId).cookie(adminSession)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions createRelease(Cookie session, long deskId, LocalDate date) throws Exception {
        String body = "{\"releaseDate\":\"" + date + "\",\"parkingSpaceId\":" + deskId
                + ",\"resourceType\":\"DESK\"}";
        return mockMvc.perform(post(RELEASES_URL).cookie(session)
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

    // ---- Helpers de lectura de la respuesta (order-independent) ----

    private JsonNode deskNode(String content, long deskId) throws Exception {
        JsonNode desks = objectMapper.readTree(content).get("desks");
        for (JsonNode node : desks) {
            if (node.get("deskId").asLong() == deskId) {
                return node;
            }
        }
        throw new AssertionError("Puesto no encontrado en el plano: " + deskId);
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

    private long insertDesk(int number, String category, int coordX, int coordY) {
        jdbcTemplate.update(
                "INSERT INTO dbo.desks (number, category, coord_x, coord_y, active) "
                        + "VALUES (?, ?, ?, ?, 1)", number, category, coordX, coordY);
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

    private int pendingDeskRequests(long deskId, LocalDate date) {
        Integer value = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.requests WHERE resource_id = ? AND requested_date = ? "
                        + "AND resource_type = 'DESK' AND status = 'PENDING'",
                Integer.class, deskId, Date.valueOf(date));
        return value == null ? 0 : value;
    }

    private int approvedDeskRequests(long deskId, LocalDate date) {
        Integer value = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.requests WHERE resource_id = ? AND requested_date = ? "
                        + "AND resource_type = 'DESK' AND status = 'APPROVED' AND resolved_by_id IS NULL",
                Integer.class, deskId, Date.valueOf(date));
        return value == null ? 0 : value;
    }

    private void setApprovalMode(String mode) {
        jdbcTemplate.update(
                "UPDATE dbo.system_settings SET approval_mode = ? WHERE id = 1", mode);
    }

    private java.math.BigDecimal coordX(long deskId) {
        return jdbcTemplate.queryForObject(
                "SELECT coord_x FROM dbo.desks WHERE id = ?", java.math.BigDecimal.class, deskId);
    }

    private java.math.BigDecimal coordY(long deskId) {
        return jdbcTemplate.queryForObject(
                "SELECT coord_y FROM dbo.desks WHERE id = ?", java.math.BigDecimal.class, deskId);
    }
}
