package com.aleatica.parking.visitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
 * Tests de integracion de reservas de visitante contra un SQL Server real
 * (Testcontainers): control de disponibilidad transaccional (plaza inactiva, asignacion
 * fija no liberada, solicitud aprobada u otra reserva), unicidad plaza+fecha garantizada
 * por el INDICE UNICO real ({@code UX_visitor_reservations_space_date}, incluida la carrera
 * de dos reservas concurrentes), anulacion de reservas solo futuras y RBAC.
 *
 * <p>Las aserciones cuentan filas por (plaza, fecha) o por id, nunca asumen orden. La
 * limpieza FK-safe la realiza {@link BaseIntegrationTest#resetDomainState}.</p>
 */
class VisitorReservationIT extends BaseIntegrationTest {

    private static final String BASE_URL = "/api/v1/visitor-reservations";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String SESSION_COOKIE = "parking_SESSION";

    private static final String ADMIN_LOGIN = "admin";
    private static final String ADMIN_PASSWORD = "Admin#Parking2026";

    private static final String EMP_LOGIN = "ittest.visres.emp";
    private static final String EMP_PASSWORD = "Visitor#Pass1word";

    private static final LocalDate TODAY = LocalDate.now(ZoneOffset.UTC);
    private static final LocalDate FUTURE = TODAY.plusDays(3);
    private static final LocalDate PAST = TODAY.minusDays(3);
    private static final int FUTURE_DOW = FUTURE.getDayOfWeek().getValue();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Cookie adminSession;
    private Cookie empSession;
    private long visitorId;
    private long spaceId;

    @BeforeEach
    void seed() throws Exception {
        insertEmployee(EMP_LOGIN);
        adminSession = login(ADMIN_LOGIN, ADMIN_PASSWORD);
        empSession = login(EMP_LOGIN, EMP_PASSWORD);
        visitorId = insertVisitor("Ada", "Lovelace", "X1234567Z");
        spaceId = insertSpace("P-VRES-01", true);
    }

    // ---- Creacion: disponibilidad ----

    @Test
    void shouldCreateReservation_whenSpaceIsAvailableForDate() throws Exception {
        // Act / Assert: 201 y la plaza pasa a estar ocupada esa fecha
        createReservation(adminSession, visitorId, spaceId, FUTURE)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.parkingSpaceId").value((int) spaceId))
                .andExpect(jsonPath("$.visitorId").value((int) visitorId));
        assertThat(reservationsForSpaceDate(spaceId, FUTURE)).isEqualTo(1);
    }

    @Test
    void shouldReturn409_whenCreatingReservationOnAlreadyReservedSpace() throws Exception {
        // Arrange: ya hay una reserva de visitante para spaceId/FUTURE
        createReservation(adminSession, visitorId, spaceId, FUTURE).andExpect(status().isCreated());

        // Act / Assert: otra reserva sobre la misma plaza/fecha -> 409
        long other = insertVisitor("Grace", "Hopper", "Y7654321X");
        createReservation(adminSession, other, spaceId, FUTURE)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("SPACE_NOT_AVAILABLE"));
        assertThat(reservationsForSpaceDate(spaceId, FUTURE)).isEqualTo(1);
    }

    @Test
    void shouldReturn409_whenCreatingReservationOnApprovedRequestSpace() throws Exception {
        // Arrange: hay una solicitud APPROVED sobre spaceId/FUTURE
        insertApprovedRequest(spaceId, FUTURE);

        // Act / Assert
        createReservation(adminSession, visitorId, spaceId, FUTURE)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("SPACE_NOT_AVAILABLE"));
        assertThat(reservationsForSpaceDate(spaceId, FUTURE)).isZero();
    }

    @Test
    void shouldReturn409_whenCreatingReservationOnFixedAssignedSpaceNotReleased() throws Exception {
        // Arrange: asignacion fija activa sobre spaceId el dia de la semana de FUTURE (no liberada)
        insertFixedAssignment(spaceId, FUTURE_DOW);

        // Act / Assert
        createReservation(adminSession, visitorId, spaceId, FUTURE)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("SPACE_NOT_AVAILABLE"));
        assertThat(reservationsForSpaceDate(spaceId, FUTURE)).isZero();
    }

    @Test
    void shouldCreateReservation_whenFixedAssignedSpaceIsReleasedForDate() throws Exception {
        // Arrange: asignacion fija ese dia PERO liberada para FUTURE -> disponible
        insertFixedAssignment(spaceId, FUTURE_DOW);
        insertRelease(spaceId, FUTURE);

        // Act / Assert
        createReservation(adminSession, visitorId, spaceId, FUTURE)
                .andExpect(status().isCreated());
        assertThat(reservationsForSpaceDate(spaceId, FUTURE)).isEqualTo(1);
    }

    @Test
    void shouldReturn409_whenCreatingReservationOnInactiveSpace() throws Exception {
        // Arrange: plaza inactiva
        long inactive = insertSpace("P-VRES-OFF", false);

        // Act / Assert
        createReservation(adminSession, visitorId, inactive, FUTURE)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("SPACE_NOT_AVAILABLE"));
        assertThat(reservationsForSpaceDate(inactive, FUTURE)).isZero();
    }

    @Test
    void shouldReturn400_whenCreatingReservationWithoutRequiredFields() throws Exception {
        // Act / Assert: parkingSpaceId ausente -> 400
        String body = "{\"visitorId\":" + visitorId + ",\"reservationDate\":\"" + FUTURE + "\"}";
        mockMvc.perform(post(BASE_URL).cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.parkingSpaceId").exists());
    }

    @Test
    void shouldReturn404_whenCreatingReservationForUnknownVisitor() throws Exception {
        // Act / Assert
        createReservation(adminSession, 999999L, spaceId, FUTURE)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // ---- Anulacion ----

    @Test
    void shouldCancelReservation_whenReservationIsInFuture() throws Exception {
        // Arrange
        long id = insertReservation(visitorId, spaceId, FUTURE);

        // Act / Assert: 204 y borrado fisico (la plaza vuelve a estar disponible)
        mockMvc.perform(delete(BASE_URL + "/" + id).cookie(adminSession))
                .andExpect(status().isNoContent());
        assertThat(reservationExists(id)).isFalse();
    }

    @Test
    void shouldReturn400_whenCancelingPastReservation() throws Exception {
        // Arrange: reserva de fecha pasada (sembrada)
        long id = insertReservation(visitorId, spaceId, PAST);

        // Act / Assert
        mockMvc.perform(delete(BASE_URL + "/" + id).cookie(adminSession))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VISITOR_RESERVATION_NOT_CANCELLABLE"));
        assertThat(reservationExists(id)).isTrue();
    }

    @Test
    void shouldReturn404_whenCancelingUnknownReservation() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/999999").cookie(adminSession))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // ---- RBAC ----

    @Test
    void shouldReturn403_whenEmployeeCreatesReservation() throws Exception {
        createReservation(empSession, visitorId, spaceId, FUTURE)
                .andExpect(status().isForbidden());
        assertThat(reservationsForSpaceDate(spaceId, FUTURE)).isZero();
    }

    @Test
    void shouldReturn401_whenRequestHasNoSession() throws Exception {
        createReservation(null, visitorId, spaceId, FUTURE).andExpect(status().isUnauthorized());
    }

    // ---- Concurrencia ----

    @Test
    void shouldAllowOnlyOneReservation_whenTwoConcurrentForSameSpaceAndDate() throws Exception {
        // Arrange: dos peticiones simultaneas reservan la MISMA plaza/fecha
        long visitorB = insertVisitor("Grace", "Hopper", "Y7654321X");

        int threads = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch fire = new CountDownLatch(1);
        List<Integer> statuses = new CopyOnWriteArrayList<>();
        try {
            pool.submit(() -> raceReservation(ready, fire, visitorId, statuses));
            pool.submit(() -> raceReservation(ready, fire, visitorB, statuses));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            fire.countDown();
            pool.shutdown();
            assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }

        // Assert: exactamente un 201 y un 409; una sola fila para (plaza, fecha)
        assertThat(Collections.frequency(statuses, 201)).isEqualTo(1);
        assertThat(Collections.frequency(statuses, 409)).isEqualTo(1);
        assertThat(reservationsForSpaceDate(spaceId, FUTURE)).isEqualTo(1);
    }

    // ---- Helpers ----

    private void raceReservation(
            CountDownLatch ready, CountDownLatch fire, long visitor, List<Integer> statuses) {
        try {
            ready.countDown();
            assertThat(fire.await(10, TimeUnit.SECONDS)).isTrue();
            int statusCode = createReservation(adminSession, visitor, spaceId, FUTURE)
                    .andReturn().getResponse().getStatus();
            statuses.add(statusCode);
        } catch (Exception ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Fallo en la reserva concurrente", ex);
        }
    }

    private ResultActions createReservation(Cookie session, long visitor, long space, LocalDate date)
            throws Exception {
        String body = "{\"visitorId\":" + visitor + ",\"parkingSpaceId\":" + space
                + ",\"reservationDate\":\"" + date + "\"}";
        var request = post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(body);
        if (session != null) {
            request = request.cookie(session);
        }
        return mockMvc.perform(request);
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

    private long insertVisitor(String first, String last, String nationalId) {
        jdbcTemplate.update(
                "INSERT INTO dbo.visitors (first_name, last_name, national_id, created_by_id) "
                        + "VALUES (?, ?, ?, ?)",
                first, last, nationalId, idOfEmployee(ADMIN_LOGIN));
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM dbo.visitors WHERE national_id = ?", Long.class, nationalId);
        return id == null ? 0L : id;
    }

    private long insertReservation(long visitor, long space, LocalDate date) {
        jdbcTemplate.update(
                "INSERT INTO dbo.visitor_reservations (visitor_id, parking_space_id, reservation_date, "
                        + "created_by_id, created_at) VALUES (?, ?, ?, ?, ?)",
                visitor, space, Date.valueOf(date), idOfEmployee(ADMIN_LOGIN),
                Timestamp.from(Instant.now()));
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM dbo.visitor_reservations WHERE parking_space_id = ? AND reservation_date = ?",
                Long.class, space, Date.valueOf(date));
        return id == null ? 0L : id;
    }

    private void insertApprovedRequest(long space, LocalDate date) {
        jdbcTemplate.update(
                "INSERT INTO dbo.requests (employee_id, requested_date, status, resource_id, "
                        + "resolved_by_id, resolved_at, created_at) VALUES (?, ?, 'APPROVED', ?, ?, ?, ?)",
                idOfEmployee(EMP_LOGIN), Date.valueOf(date), space, idOfEmployee(ADMIN_LOGIN),
                Timestamp.from(Instant.now()), Timestamp.from(Instant.now()));
    }

    private void insertFixedAssignment(long space, int dayOfWeek) {
        jdbcTemplate.update(
                "INSERT INTO dbo.fixed_assignments (resource_id, employee_id, day_of_week, "
                        + "active, created_by_id, created_at) VALUES (?, ?, ?, 1, ?, ?)",
                space, idOfEmployee(EMP_LOGIN), dayOfWeek, idOfEmployee(ADMIN_LOGIN),
                Timestamp.from(Instant.now()));
    }

    private void insertRelease(long space, LocalDate date) {
        jdbcTemplate.update(
                "INSERT INTO dbo.releases (resource_id, employee_id, release_date, type, "
                        + "released_by_id, created_at) VALUES (?, ?, ?, 'VOLUNTARY', ?, ?)",
                space, idOfEmployee(EMP_LOGIN), Date.valueOf(date), idOfEmployee(EMP_LOGIN),
                Timestamp.from(Instant.now()));
    }

    private long insertSpace(String label, boolean active) {
        jdbcTemplate.update("INSERT INTO dbo.parking_spaces (number, label, active) VALUES ((SELECT ISNULL(MAX(number),1000)+1 FROM dbo.parking_spaces), ?, ?)",
                label, active ? 1 : 0);
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM dbo.parking_spaces WHERE label = ?", Long.class, label);
        return id == null ? 0L : id;
    }

    private void insertEmployee(String login) {
        jdbcTemplate.update(
                "INSERT INTO dbo.employees "
                        + "(first_name, last_name, login, email, password_hash, password_must_change, "
                        + "is_corporate, auth_origin, role, enabled, active) "
                        + "VALUES ('IT', 'User', ?, ?, ?, 0, 0, 'LOCAL', 'EMPLOYEE', 1, 1)",
                login, login + "@aleatica.com", passwordEncoder.encode(EMP_PASSWORD));
    }

    private long idOfEmployee(String login) {
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM dbo.employees WHERE login = ?", Long.class, login);
        return id == null ? 0L : id;
    }

    private boolean reservationExists(long id) {
        Integer value = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.visitor_reservations WHERE id = ?", Integer.class, id);
        return value != null && value > 0;
    }

    private int reservationsForSpaceDate(long space, LocalDate date) {
        Integer value = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.visitor_reservations WHERE parking_space_id = ? "
                        + "AND reservation_date = ?",
                Integer.class, space, Date.valueOf(date));
        return value == null ? 0 : value;
    }
}
