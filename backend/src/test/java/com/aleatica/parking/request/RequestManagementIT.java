package com.aleatica.parking.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Tests de integracion de solicitudes contra un SQL Server real (Testcontainers):
 * ventana temporal, unicidad {@code PENDING} y disponibilidad {@code APPROVED}
 * garantizadas por los INDICES UNICOS FILTRADOS reales de la BD (incluida la doble
 * aprobacion concurrente de dos administradores sobre la misma plaza/fecha), maquina
 * de estados, verificacion de pertenencia (BOLA) y orden FIFO.
 *
 * <p>Todas las aserciones de listados son independientes del orden salvo la de FIFO,
 * que es el objetivo del propio test y opera sobre datos sembrados de forma
 * determinista tras la limpieza FK-safe de {@link BaseIntegrationTest}.</p>
 */
class RequestManagementIT extends BaseIntegrationTest {

    private static final String BASE_URL = "/api/v1/requests";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String SESSION_COOKIE = "parking_SESSION";

    private static final String ADMIN_LOGIN = "admin";
    private static final String ADMIN_PASSWORD = "Admin#Parking2026";

    private static final String EMP_A_LOGIN = "ittest.req.empa";
    private static final String EMP_B_LOGIN = "ittest.req.empb";
    private static final String EMP_PASSWORD = "Request#Pass1word";

    private static final LocalDate TODAY = LocalDate.now(ZoneOffset.UTC);
    private static final LocalDate WITHIN = TODAY.plusDays(3);
    private static final LocalDate OUTSIDE_PAST = TODAY.minusDays(1);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Cookie adminSession;
    private Cookie empASession;
    private long empAId;
    private long empBId;
    private long spaceX;

    @BeforeEach
    void seed() throws Exception {
        // La limpieza FK-safe de la BD compartida la realiza BaseIntegrationTest#resetDomainState;
        // aqui solo se siembran los datos propios del test.
        empAId = insertEmployee(EMP_A_LOGIN);
        empBId = insertEmployee(EMP_B_LOGIN);
        adminSession = login(ADMIN_LOGIN, ADMIN_PASSWORD);
        empASession = login(EMP_A_LOGIN, EMP_PASSWORD);
        spaceX = insertSpace("P-REQ-01");
    }

    // ---- Creacion: ventana + unicidad ----

    @Test
    void shouldCreatePendingRequest_whenDateWithinWindow() throws Exception {
        // Act / Assert: 201 PENDING, sin plaza
        createRequest(empASession, WITHIN)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.parkingSpaceId").doesNotExist());
        assertThat(statusOf(empAId, WITHIN)).isEqualTo("PENDING");
    }

    @Test
    void shouldReturn400_whenDateBeforeToday() throws Exception {
        // Act / Assert
        createRequest(empASession, OUTSIDE_PAST)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("OUTSIDE_REQUEST_WINDOW"))
                .andExpect(jsonPath("$.fields.requestedDate").exists());
        assertThat(countRequests(empAId)).isZero();
    }

    @Test
    void shouldReturn409_whenDuplicatePendingForSameDate() throws Exception {
        // Arrange
        createRequest(empASession, WITHIN).andExpect(status().isCreated());

        // Act / Assert: segunda PENDING misma fecha -> 409 (indice filtrado)
        createRequest(empASession, WITHIN)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("REQUEST_ALREADY_PENDING"));
        assertThat(countPending(empAId, WITHIN)).isEqualTo(1);
    }

    @Test
    void shouldAllowNewPending_whenPreviousCancelledForSameDate() throws Exception {
        // Arrange: una solicitud CANCELLED no bloquea (el indice solo aplica a PENDING)
        insertRequest(empAId, WITHIN, "CANCELLED", Instant.now());

        // Act / Assert
        createRequest(empASession, WITHIN).andExpect(status().isCreated());
        assertThat(countPending(empAId, WITHIN)).isEqualTo(1);
    }

    // ---- Listados: BOLA + FIFO ----

    @Test
    void shouldListOnlyOwnRequests_whenEmployeeListsMine() throws Exception {
        // Arrange: dos propias de A y una de B
        insertRequest(empAId, WITHIN, "PENDING", Instant.now());
        insertRequest(empAId, TODAY.plusDays(5), "PENDING", Instant.now());
        insertRequest(empBId, WITHIN, "PENDING", Instant.now());

        // Act / Assert: solo las de A (independiente del orden)
        mockMvc.perform(get(BASE_URL + "/mine").cookie(empASession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].employeeId", everyItem(equalTo((int) empAId))));
    }

    @Test
    void shouldListPendingInFifoOrder_whenAdminListsPending() throws Exception {
        // Arrange: A creada antes que B (created_at controlado)
        Instant base = Instant.parse("2026-07-04T10:00:00Z");
        insertRequest(empAId, WITHIN, "PENDING", base);
        insertRequest(empBId, WITHIN, "PENDING", base.plusSeconds(60));

        // Act / Assert: orden FIFO por created_at ASC (A antes que B)
        mockMvc.perform(get(BASE_URL + "/pending").cookie(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].employeeId").value((int) empAId))
                .andExpect(jsonPath("$.content[1].employeeId").value((int) empBId));
    }

    @Test
    void shouldReturn403_whenEmployeeListsPending() throws Exception {
        // Act / Assert (RBAC): un EMPLOYEE no accede a la cola de pendientes
        mockMvc.perform(get(BASE_URL + "/pending").cookie(empASession))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn404_whenGettingUnknownRequest() throws Exception {
        // Act / Assert
        mockMvc.perform(get(BASE_URL + "/999999").cookie(adminSession))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // ---- Cancelacion: BOLA + estado ----

    @Test
    void shouldCancelRequest_whenOwnerCancelsPending() throws Exception {
        // Arrange
        long id = insertRequest(empAId, WITHIN, "PENDING", Instant.now());

        // Act / Assert
        mockMvc.perform(post(BASE_URL + "/" + id + "/cancel").cookie(empASession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(statusOf(empAId, WITHIN)).isEqualTo("CANCELLED");
    }

    @Test
    void shouldReturn409_whenCancellingResolvedRequest() throws Exception {
        // Arrange: solicitud ya aprobada
        long id = insertApproved(empAId, WITHIN, spaceX);

        // Act / Assert
        mockMvc.perform(post(BASE_URL + "/" + id + "/cancel").cookie(empASession))
                .andExpect(status().isConflict());
        assertThat(statusOf(empAId, WITHIN)).isEqualTo("APPROVED");
    }

    @Test
    void shouldReturn403_whenCancellingOtherEmployeeRequest() throws Exception {
        // Arrange: solicitud de B; A intenta cancelarla
        long id = insertRequest(empBId, WITHIN, "PENDING", Instant.now());

        // Act / Assert (BOLA)
        mockMvc.perform(post(BASE_URL + "/" + id + "/cancel").cookie(empASession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        assertThat(statusOf(empBId, WITHIN)).isEqualTo("PENDING");
    }

    // ---- Aprobacion: disponibilidad + concurrencia ----

    @Test
    void shouldApproveRequest_whenSpaceAvailable() throws Exception {
        // Arrange
        long id = insertRequest(empAId, WITHIN, "PENDING", Instant.now());

        // Act / Assert
        approve(adminSession, id, spaceX, "Bienvenido")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.parkingSpaceId").value((int) spaceX))
                .andExpect(jsonPath("$.resolvedById").value((int) idOfEmployee(ADMIN_LOGIN)))
                .andExpect(jsonPath("$.approvalNote").value("Bienvenido"));
    }

    @Test
    void shouldReturn409_whenApprovingSpaceWithActiveFixedAssignment() throws Exception {
        // Arrange: la plaza tiene una asignacion fija activa ese dia de la semana
        long id = insertRequest(empAId, WITHIN, "PENDING", Instant.now());
        insertFixedAssignment(spaceX, empBId, WITHIN.getDayOfWeek().getValue());

        // Act / Assert
        approve(adminSession, id, spaceX, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("SPACE_NOT_AVAILABLE"));
        assertThat(statusOf(empAId, WITHIN)).isEqualTo("PENDING");
    }

    @Test
    void shouldReturn409_whenApprovingRequestForSpaceReservedByVisitor() throws Exception {
        // Arrange: la plaza ya tiene una reserva de visitante para esa fecha; una solicitud
        // pendiente pide la MISMA plaza/fecha. Aprobarla crearia una doble reserva (integridad).
        long visitorId = insertVisitor("11111111H");
        insertVisitorReservation(visitorId, spaceX, WITHIN);
        long id = insertRequest(empAId, WITHIN, "PENDING", Instant.now());

        // Act / Assert: 409 SPACE_NOT_AVAILABLE, sin doble reserva
        approve(adminSession, id, spaceX, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("SPACE_NOT_AVAILABLE"));
        assertThat(statusOf(empAId, WITHIN)).isEqualTo("PENDING");
        assertThat(approvedRowsForSpaceDate(spaceX, WITHIN)).isZero();
        assertThat(visitorReservationRows(spaceX, WITHIN)).isEqualTo(1);
    }

    @Test
    void shouldApproveRequest_whenFixedAssignmentIsReleasedForThatDate() throws Exception {
        // Arrange: la plaza tiene asignacion fija ese dia PERO liberada para WITHIN -> disponible
        insertFixedAssignment(spaceX, empBId, WITHIN.getDayOfWeek().getValue());
        insertRelease(spaceX, empBId, WITHIN);
        long id = insertRequest(empAId, WITHIN, "PENDING", Instant.now());

        // Act / Assert: la liberacion la vuelve aprobable
        approve(adminSession, id, spaceX, "Bienvenido")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.parkingSpaceId").value((int) spaceX));
        assertThat(statusOf(empAId, WITHIN)).isEqualTo("APPROVED");
    }

    @Test
    void shouldReturn409_whenTwoAdminsApproveSameSpaceSameDate() throws Exception {
        // Arrange: dos solicitudes PENDING (A y B) para la MISMA fecha; ambas se aprueban
        // con la MISMA plaza de forma concurrente.
        long idA = insertRequest(empAId, WITHIN, "PENDING", Instant.now());
        long idB = insertRequest(empBId, WITHIN, "PENDING", Instant.now());

        int threads = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch fire = new CountDownLatch(1);
        List<Integer> statuses = new CopyOnWriteArrayList<>();
        try {
            pool.submit(() -> raceApprove(ready, fire, idA, statuses));
            pool.submit(() -> raceApprove(ready, fire, idB, statuses));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            fire.countDown();
            pool.shutdown();
            assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }

        // Assert: exactamente un 200 y un 409; una sola fila APPROVED para (plaza, fecha)
        assertThat(Collections.frequency(statuses, 200)).isEqualTo(1);
        assertThat(Collections.frequency(statuses, 409)).isEqualTo(1);
        assertThat(approvedRowsForSpaceDate(spaceX, WITHIN)).isEqualTo(1);
    }

    // ---- Rechazo: catalogo + validacion OTHER ----

    @Test
    void shouldRejectRequest_whenReasonCodeFromCatalog() throws Exception {
        // Arrange
        long id = insertRequest(empAId, WITHIN, "PENDING", Instant.now());

        // Act / Assert
        reject(adminSession, id, "{\"reasonCode\":\"NO_AVAILABILITY\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReasonCode").value("NO_AVAILABILITY"))
                .andExpect(jsonPath("$.resolvedById").value((int) idOfEmployee(ADMIN_LOGIN)));
    }

    @Test
    void shouldReturn400_whenRejectingOtherWithoutFreeText() throws Exception {
        // Arrange
        long id = insertRequest(empAId, WITHIN, "PENDING", Instant.now());

        // Act / Assert
        reject(adminSession, id, "{\"reasonCode\":\"OTHER\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.rejectionReason").exists());
        assertThat(statusOf(empAId, WITHIN)).isEqualTo("PENDING");
    }

    // ---- Helpers ----

    private void raceApprove(CountDownLatch ready, CountDownLatch fire, long id, List<Integer> statuses) {
        try {
            ready.countDown();
            assertThat(fire.await(10, TimeUnit.SECONDS)).isTrue();
            int statusCode = approve(adminSession, id, spaceX, null)
                    .andReturn().getResponse().getStatus();
            statuses.add(statusCode);
        } catch (Exception ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Fallo en la aprobacion concurrente", ex);
        }
    }

    private ResultActions createRequest(Cookie session, LocalDate date) throws Exception {
        String body = "{\"requestedDate\":\"" + date + "\"}";
        return mockMvc.perform(post(BASE_URL).cookie(session)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions approve(Cookie session, long id, long spaceId, String note) throws Exception {
        String noteJson = note == null ? "" : ",\"approvalNote\":\"" + note + "\"";
        String body = "{\"parkingSpaceId\":" + spaceId + noteJson + "}";
        return mockMvc.perform(post(BASE_URL + "/" + id + "/approve").cookie(session)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions reject(Cookie session, long id, String body) throws Exception {
        return mockMvc.perform(post(BASE_URL + "/" + id + "/reject").cookie(session)
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

    private long insertRequest(long employeeId, LocalDate date, String status, Instant createdAt) {
        jdbcTemplate.update(
                "INSERT INTO dbo.requests (employee_id, requested_date, status, created_at) "
                        + "VALUES (?, ?, ?, ?)",
                employeeId, Date.valueOf(date), status, Timestamp.from(createdAt));
        Long id = jdbcTemplate.queryForObject(
                "SELECT TOP 1 id FROM dbo.requests WHERE employee_id = ? AND requested_date = ? "
                        + "AND status = ? ORDER BY id DESC",
                Long.class, employeeId, Date.valueOf(date), status);
        return id == null ? 0L : id;
    }

    private long insertApproved(long employeeId, LocalDate date, long spaceId) {
        jdbcTemplate.update(
                "INSERT INTO dbo.requests (employee_id, requested_date, status, resource_id, "
                        + "resolved_by_id, resolved_at, created_at) VALUES (?, ?, 'APPROVED', ?, ?, ?, ?)",
                employeeId, Date.valueOf(date), spaceId, idOfEmployee(ADMIN_LOGIN),
                Timestamp.from(Instant.now()), Timestamp.from(Instant.now()));
        Long id = jdbcTemplate.queryForObject(
                "SELECT TOP 1 id FROM dbo.requests WHERE employee_id = ? AND requested_date = ? "
                        + "AND status = 'APPROVED' ORDER BY id DESC",
                Long.class, employeeId, Date.valueOf(date));
        return id == null ? 0L : id;
    }

    private long insertVisitor(String nationalId) {
        jdbcTemplate.update(
                "INSERT INTO dbo.visitors (first_name, last_name, national_id, created_by_id, created_at) "
                        + "VALUES ('Visita', 'Test', ?, ?, ?)",
                nationalId, idOfEmployee(ADMIN_LOGIN), Timestamp.from(Instant.now()));
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM dbo.visitors WHERE national_id = ?", Long.class, nationalId);
        return id == null ? 0L : id;
    }

    private void insertVisitorReservation(long visitorId, long spaceId, LocalDate date) {
        jdbcTemplate.update(
                "INSERT INTO dbo.visitor_reservations (visitor_id, parking_space_id, reservation_date, "
                        + "created_by_id, created_at) VALUES (?, ?, ?, ?, ?)",
                visitorId, spaceId, Date.valueOf(date), idOfEmployee(ADMIN_LOGIN),
                Timestamp.from(Instant.now()));
    }

    private void insertRelease(long spaceId, long employeeId, LocalDate date) {
        jdbcTemplate.update(
                "INSERT INTO dbo.releases (resource_id, employee_id, release_date, type, "
                        + "released_by_id, created_at) VALUES (?, ?, ?, 'VOLUNTARY', ?, ?)",
                spaceId, employeeId, Date.valueOf(date), employeeId, Timestamp.from(Instant.now()));
    }

    private void insertFixedAssignment(long spaceId, long employeeId, int dayOfWeek) {
        jdbcTemplate.update(
                "INSERT INTO dbo.fixed_assignments (resource_id, employee_id, day_of_week, "
                        + "active, created_by_id, created_at) VALUES (?, ?, ?, 1, ?, ?)",
                spaceId, employeeId, dayOfWeek, idOfEmployee(ADMIN_LOGIN), Timestamp.from(Instant.now()));
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
        jdbcTemplate.update("INSERT INTO dbo.parking_spaces (number, label, active) VALUES ((SELECT ISNULL(MAX(number),1000)+1 FROM dbo.parking_spaces), ?, 1)", label);
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM dbo.parking_spaces WHERE label = ?", Long.class, label);
        return id == null ? 0L : id;
    }

    private long idOfEmployee(String login) {
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM dbo.employees WHERE login = ?", Long.class, login);
        return id == null ? 0L : id;
    }

    private String statusOf(long employeeId, LocalDate date) {
        return jdbcTemplate.queryForObject(
                "SELECT TOP 1 status FROM dbo.requests WHERE employee_id = ? AND requested_date = ? "
                        + "ORDER BY id DESC",
                String.class, employeeId, Date.valueOf(date));
    }

    private int countRequests(long employeeId) {
        return count("SELECT COUNT(*) FROM dbo.requests WHERE employee_id = ?", employeeId);
    }

    private int countPending(long employeeId, LocalDate date) {
        return count("SELECT COUNT(*) FROM dbo.requests WHERE employee_id = ? AND requested_date = ? "
                + "AND status = 'PENDING'", employeeId, Date.valueOf(date));
    }

    private int approvedRowsForSpaceDate(long spaceId, LocalDate date) {
        return count("SELECT COUNT(*) FROM dbo.requests WHERE resource_id = ? AND requested_date = ? "
                + "AND status = 'APPROVED'", spaceId, Date.valueOf(date));
    }

    private int visitorReservationRows(long spaceId, LocalDate date) {
        return count("SELECT COUNT(*) FROM dbo.visitor_reservations WHERE parking_space_id = ? "
                + "AND reservation_date = ?", spaceId, Date.valueOf(date));
    }

    private int count(String sql, Object... args) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }
}
