package com.aleatica.parking.release;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
 * Tests de integracion de liberaciones contra un SQL Server real (Testcontainers):
 * ventana temporal, resolucion de la plaza fija, unicidad recurso+fecha garantizada por
 * el INDICE UNICO real de la BD (incluida la carrera de dos liberaciones concurrentes del
 * mismo recurso/fecha), verificacion de pertenencia (BOLA) en listado y cancelacion, y
 * cancelacion (borrado fisico) de una liberacion futura propia.
 *
 * <p>Todas las aserciones de listados son independientes del orden: filtran por campo
 * unico o usan {@code everyItem}, nunca asumen que la propia fila sea la primera. La
 * limpieza FK-safe (incluida la tabla {@code releases}) la realiza
 * {@link BaseIntegrationTest#resetDomainState}.</p>
 */
class ReleaseManagementIT extends BaseIntegrationTest {

    private static final String BASE_URL = "/api/v1/releases";
    private static final String MINE_URL = BASE_URL + "/mine";
    private static final String ADMIN_URL = BASE_URL + "/administrative";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String SESSION_COOKIE = "parking_SESSION";

    private static final String ADMIN_LOGIN = "admin";
    private static final String ADMIN_PASSWORD = "Admin#Parking2026";

    private static final String EMP_A_LOGIN = "ittest.rel.empa";
    private static final String EMP_B_LOGIN = "ittest.rel.empb";
    private static final String EMP_PASSWORD = "Release#Pass1word";

    private static final LocalDate TODAY = LocalDate.now(ZoneOffset.UTC);
    private static final LocalDate FUTURE = TODAY.plusDays(3);
    private static final LocalDate PAST = TODAY.minusDays(1);
    private static final int FUTURE_DOW = FUTURE.getDayOfWeek().getValue();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Cookie adminSession;
    private Cookie empASession;
    private long empAId;
    private long empBId;
    private long spaceX;
    private long spaceY;

    @BeforeEach
    void seed() throws Exception {
        // La limpieza FK-safe de la BD compartida la realiza BaseIntegrationTest#resetDomainState;
        // aqui solo se siembran los datos propios del test.
        empAId = insertEmployee(EMP_A_LOGIN);
        empBId = insertEmployee(EMP_B_LOGIN);
        adminSession = login(ADMIN_LOGIN, ADMIN_PASSWORD);
        empASession = login(EMP_A_LOGIN, EMP_PASSWORD);
        spaceX = insertSpace("P-REL-01");
        spaceY = insertSpace("P-REL-02");
    }

    // ---- Liberacion voluntaria: ventana + resolucion de plaza ----

    @Test
    void shouldCreateVoluntaryRelease_whenOwnerAndFutureDate() throws Exception {
        // Arrange: asignacion fija de A sobre spaceX el dia de FUTURE
        insertFixedAssignment(spaceX, empAId, FUTURE_DOW);

        // Act / Assert: 201 VOLUNTARY, plaza resuelta implicitamente, titular = ejecutor
        createRelease(empASession, FUTURE, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("VOLUNTARY"))
                .andExpect(jsonPath("$.parkingSpaceId").value((int) spaceX))
                .andExpect(jsonPath("$.employeeId").value((int) empAId))
                .andExpect(jsonPath("$.releasedById").value((int) empAId))
                .andExpect(jsonPath("$.reason").doesNotExist());
        assertThat(releasesForSpaceDate(spaceX, FUTURE)).isEqualTo(1);
    }

    @Test
    void shouldReturn400_whenReleaseDateInPast() throws Exception {
        // Arrange
        insertFixedAssignment(spaceX, empAId, PAST.getDayOfWeek().getValue());

        // Act / Assert
        createRelease(empASession, PAST, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("RELEASE_DATE_IN_PAST"))
                .andExpect(jsonPath("$.fields.releaseDate").exists());
        assertThat(countReleases(empAId)).isZero();
    }

    @Test
    void shouldReturn409_whenNoFixedAssignmentForThatDay() throws Exception {
        // Arrange: A no tiene asignacion fija ese dia de la semana

        // Act / Assert
        createRelease(empASession, FUTURE, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("NO_FIXED_ASSIGNMENT"));
        assertThat(countReleases(empAId)).isZero();
    }

    // ---- Listado propio: BOLA ----

    @Test
    void shouldListOnlyOwnReleases_whenEmployeeRequestsMine() throws Exception {
        // Arrange: dos de A y una de B
        insertRelease(spaceX, empAId, FUTURE, "VOLUNTARY", null, empAId);
        insertRelease(spaceY, empAId, FUTURE.plusDays(1), "VOLUNTARY", null, empAId);
        insertRelease(spaceX, empBId, FUTURE.plusDays(2), "VOLUNTARY", null, empBId);

        // Act / Assert: solo las de A (independiente del orden)
        mockMvc.perform(get(MINE_URL).cookie(empASession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].employeeId", everyItem(equalTo((int) empAId))));
    }

    // ---- Cancelacion: BOLA + fecha ----

    @Test
    void shouldCancelRelease_whenFutureAndOwn() throws Exception {
        // Arrange
        long id = insertRelease(spaceX, empAId, FUTURE, "VOLUNTARY", null, empAId);

        // Act / Assert: 204 y borrado fisico
        mockMvc.perform(delete(BASE_URL + "/" + id).cookie(empASession))
                .andExpect(status().isNoContent());
        assertThat(releaseExists(id)).isFalse();
    }

    @Test
    void shouldReturn403_whenCancellingReleaseOfAnotherEmployee() throws Exception {
        // Arrange: liberacion de B; A intenta cancelarla (BOLA)
        long id = insertRelease(spaceX, empBId, FUTURE, "VOLUNTARY", null, empBId);

        // Act / Assert
        mockMvc.perform(delete(BASE_URL + "/" + id).cookie(empASession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        assertThat(releaseExists(id)).isTrue();
    }

    @Test
    void shouldReturn409_whenCancellingPastRelease() throws Exception {
        // Arrange: liberacion propia de fecha pasada (sembrada directamente)
        long id = insertRelease(spaceX, empAId, PAST, "VOLUNTARY", null, empAId);

        // Act / Assert
        mockMvc.perform(delete(BASE_URL + "/" + id).cookie(empASession))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("RELEASE_NOT_CANCELLABLE"));
        assertThat(releaseExists(id)).isTrue();
    }

    @Test
    void shouldReturn404_whenCancellingUnknownRelease() throws Exception {
        // Act / Assert
        mockMvc.perform(delete(BASE_URL + "/999999").cookie(empASession))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // ---- Liberacion administrativa ----

    @Test
    void shouldCreateAdministrativeRelease_whenAdminAndReasonPresent() throws Exception {
        // Arrange: asignacion fija de B sobre spaceX el dia de FUTURE
        insertFixedAssignment(spaceX, empBId, FUTURE_DOW);

        // Act / Assert: 201 ADMINISTRATIVE, titular = B, ejecutor = admin, reason presente
        administrativeRelease(adminSession, empBId, spaceX, FUTURE, "Ausencia justificada")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("ADMINISTRATIVE"))
                .andExpect(jsonPath("$.employeeId").value((int) empBId))
                .andExpect(jsonPath("$.releasedById").value((int) idOfEmployee(ADMIN_LOGIN)))
                .andExpect(jsonPath("$.reason").value("Ausencia justificada"));
        assertThat(releasesForSpaceDate(spaceX, FUTURE)).isEqualTo(1);
    }

    @Test
    void shouldReturn400_whenAdministrativeReleaseMissingReason() throws Exception {
        // Arrange
        insertFixedAssignment(spaceX, empBId, FUTURE_DOW);

        // Act / Assert: reason ausente -> 400 con fields.reason
        String body = "{\"employeeId\":" + empBId + ",\"parkingSpaceId\":" + spaceX
                + ",\"releaseDate\":\"" + FUTURE + "\"}";
        mockMvc.perform(post(ADMIN_URL).cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.reason").exists());
        assertThat(releasesForSpaceDate(spaceX, FUTURE)).isZero();
    }

    @Test
    void shouldReturn403_whenNonAdminCreatesAdministrativeRelease() throws Exception {
        // Arrange
        insertFixedAssignment(spaceX, empAId, FUTURE_DOW);

        // Act / Assert (RBAC): un EMPLOYEE no accede a la liberacion administrativa
        administrativeRelease(empASession, empAId, spaceX, FUTURE, "Motivo")
                .andExpect(status().isForbidden());
        assertThat(releasesForSpaceDate(spaceX, FUTURE)).isZero();
    }

    // ---- Unicidad + concurrencia ----

    @Test
    void shouldReturn409_whenResourceAlreadyReleasedForDate() throws Exception {
        // Arrange: A ya libero spaceX para FUTURE; se intenta una segunda liberacion
        insertFixedAssignment(spaceX, empAId, FUTURE_DOW);
        createRelease(empASession, FUTURE, null).andExpect(status().isCreated());

        // Act / Assert: segunda liberacion del mismo recurso/fecha -> 409 (indice unico)
        createRelease(empASession, FUTURE, spaceX)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("RESOURCE_ALREADY_RELEASED"));
        assertThat(releasesForSpaceDate(spaceX, FUTURE)).isEqualTo(1);
    }

    @Test
    void shouldAllowOnlyOneRelease_whenTwoConcurrentForSameResourceAndDate() throws Exception {
        // Arrange: A tiene asignacion fija sobre spaceX el dia de FUTURE; dos peticiones
        // simultaneas liberan el MISMO recurso/fecha.
        insertFixedAssignment(spaceX, empAId, FUTURE_DOW);

        int threads = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch fire = new CountDownLatch(1);
        List<Integer> statuses = new CopyOnWriteArrayList<>();
        try {
            pool.submit(() -> raceRelease(ready, fire, statuses));
            pool.submit(() -> raceRelease(ready, fire, statuses));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            fire.countDown();
            pool.shutdown();
            assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }

        // Assert: exactamente un 201 y un 409; una sola fila para (recurso, fecha)
        assertThat(Collections.frequency(statuses, 201)).isEqualTo(1);
        assertThat(Collections.frequency(statuses, 409)).isEqualTo(1);
        assertThat(releasesForSpaceDate(spaceX, FUTURE)).isEqualTo(1);
    }

    // ---- Helpers ----

    private void raceRelease(CountDownLatch ready, CountDownLatch fire, List<Integer> statuses) {
        try {
            ready.countDown();
            assertThat(fire.await(10, TimeUnit.SECONDS)).isTrue();
            int statusCode = createRelease(empASession, FUTURE, spaceX)
                    .andReturn().getResponse().getStatus();
            statuses.add(statusCode);
        } catch (Exception ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Fallo en la liberacion concurrente", ex);
        }
    }

    private ResultActions createRelease(Cookie session, LocalDate date, Long spaceId) throws Exception {
        String spaceJson = spaceId == null ? "" : ",\"parkingSpaceId\":" + spaceId;
        String body = "{\"releaseDate\":\"" + date + "\"" + spaceJson + "}";
        return mockMvc.perform(post(BASE_URL).cookie(session)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions administrativeRelease(
            Cookie session, long employeeId, long spaceId, LocalDate date, String reason) throws Exception {
        String body = "{\"employeeId\":" + employeeId + ",\"parkingSpaceId\":" + spaceId
                + ",\"releaseDate\":\"" + date + "\",\"reason\":\"" + reason + "\"}";
        return mockMvc.perform(post(ADMIN_URL).cookie(session)
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

    private long insertRelease(
            long spaceId, long employeeId, LocalDate date, String type, String reason, long releasedById) {
        jdbcTemplate.update(
                "INSERT INTO dbo.releases (resource_id, employee_id, release_date, type, reason, "
                        + "released_by_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                spaceId, employeeId, Date.valueOf(date), type, reason, releasedById,
                Timestamp.from(Instant.now()));
        Long id = jdbcTemplate.queryForObject(
                "SELECT TOP 1 id FROM dbo.releases WHERE resource_id = ? AND release_date = ? "
                        + "ORDER BY id DESC",
                Long.class, spaceId, Date.valueOf(date));
        return id == null ? 0L : id;
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
        jdbcTemplate.update("INSERT INTO dbo.parking_spaces (label, active) VALUES (?, 1)", label);
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM dbo.parking_spaces WHERE label = ?", Long.class, label);
        return id == null ? 0L : id;
    }

    private long idOfEmployee(String login) {
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM dbo.employees WHERE login = ?", Long.class, login);
        return id == null ? 0L : id;
    }

    private boolean releaseExists(long id) {
        return count("SELECT COUNT(*) FROM dbo.releases WHERE id = ?", id) > 0;
    }

    private int countReleases(long employeeId) {
        return count("SELECT COUNT(*) FROM dbo.releases WHERE employee_id = ?", employeeId);
    }

    private int releasesForSpaceDate(long spaceId, LocalDate date) {
        return count("SELECT COUNT(*) FROM dbo.releases WHERE resource_id = ? AND release_date = ?",
                spaceId, Date.valueOf(date));
    }

    private int count(String sql, Object... args) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }
}
