package com.aleatica.parking.fixedassignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aleatica.parking.support.BaseIntegrationTest;
import jakarta.servlet.http.Cookie;
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
 * Tests de integracion de asignaciones fijas contra un SQL Server real
 * (Testcontainers): reemplazo del conjunto de dias, revocacion logica que preserva
 * historico, verificacion de pertenencia (BOLA) y, sobre todo, que los indices
 * unicos FILTRADOS ({@code WHERE active = 1}) de la BD real fuerzan el {@code 409} en
 * colision plaza/dia y empleado/dia, incluida la doble insercion concurrente.
 */
class FixedAssignmentManagementIT extends BaseIntegrationTest {

    private static final String BASE_URL = "/api/v1/fixed-assignments";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String SESSION_COOKIE = "parking_SESSION";

    private static final String ADMIN_LOGIN = "admin";
    private static final String ADMIN_PASSWORD = "Admin#Parking2026";

    private static final String EMP_A_LOGIN = "ittest.fa.empa";
    private static final String EMP_B_LOGIN = "ittest.fa.empb";
    private static final String EMP_PASSWORD = "Fixed#Pass1word";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Cookie adminSession;
    private long empAId;
    private long empBId;
    private long spaceX;
    private long spaceY;

    @BeforeEach
    void cleanAndSeed() throws Exception {
        jdbcTemplate.update("DELETE FROM dbo.fixed_assignments");
        jdbcTemplate.update("DELETE FROM dbo.login_log");
        jdbcTemplate.update("DELETE FROM dbo.employees WHERE login LIKE 'ittest.fa.%'");
        jdbcTemplate.update("DELETE FROM dbo.parking_spaces");
        adminSession = login(ADMIN_LOGIN, ADMIN_PASSWORD);
        empAId = insertEmployee(EMP_A_LOGIN);
        empBId = insertEmployee(EMP_B_LOGIN);
        spaceX = insertSpace("P-01");
        spaceY = insertSpace("P-02");
    }

    @Test
    void shouldCreateAssignments_whenAdminSetsValidDays() throws Exception {
        // Act
        setAssignments(adminSession, empAId, spaceX, "[1,2,3]")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].active").value(true));

        // Assert: 3 filas activas del empleado A, con created_by = admin
        assertThat(activeCount(empAId)).isEqualTo(3);
        assertThat(createdByOf(empAId)).isEqualTo(adminId());
    }

    @Test
    void shouldReturn400_whenDayOfWeekOutOfRange() throws Exception {
        // Act / Assert: dia 8 fuera de rango
        setAssignments(adminSession, empAId, spaceX, "[1,8]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.daysOfWeek").exists());
        assertThat(activeCount(empAId)).isZero();
    }

    @Test
    void shouldReturn400_whenDaysEmpty() throws Exception {
        // Act / Assert: lista vacia
        setAssignments(adminSession, empAId, spaceX, "[]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.daysOfWeek").exists());
    }

    @Test
    void shouldReturn409_whenSpaceAlreadyAssignedToOtherEmployeeSameDay() throws Exception {
        // Arrange: plaza X asignada al empleado A el dia 1
        setAssignments(adminSession, empAId, spaceX, "[1]").andExpect(status().isOk());

        // Act / Assert: misma plaza X, dia 1, al empleado B -> 409 (indice plaza/dia)
        setAssignments(adminSession, empBId, spaceX, "[1]")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fields.parkingSpaceId").exists());
        assertThat(activeCount(empBId)).isZero();
    }

    @Test
    void shouldReturn409_whenEmployeeAlreadyHasResourceSameDay() throws Exception {
        // Arrange: empleado A con plaza X el dia 2
        setAssignments(adminSession, empAId, spaceX, "[2]").andExpect(status().isOk());

        // Act / Assert: otra plaza Y el mismo dia 2, sin revocar -> 409 (indice empleado/dia)
        setAssignments(adminSession, empAId, spaceY, "[2]")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fields.employeeId").exists());
    }

    @Test
    void shouldKeepSingleActiveRow_whenConcurrentPutsTargetSameSpaceAndDay() throws Exception {
        // Arrange: dos PUT concurrentes asignan la MISMA plaza el dia 1 a A y a B
        int threads = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch fire = new CountDownLatch(1);
        List<Integer> statuses = new CopyOnWriteArrayList<>();
        try {
            pool.submit(() -> race(ready, fire, empAId, statuses));
            pool.submit(() -> race(ready, fire, empBId, statuses));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            fire.countDown();
            pool.shutdown();
            assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }

        // Assert: exactamente un 200 y un 409; una sola fila activa para (X, dia 1)
        assertThat(Collections.frequency(statuses, 200)).isEqualTo(1);
        assertThat(Collections.frequency(statuses, 409)).isEqualTo(1);
        assertThat(activeRowsForSpaceDay(spaceX, 1)).isEqualTo(1);
    }

    @Test
    void shouldLogicallyRevoke_whenAdminDeletesActiveAssignment() throws Exception {
        // Arrange: empleado A con dias 1,2 en la plaza X
        setAssignments(adminSession, empAId, spaceX, "[1,2]").andExpect(status().isOk());

        // Act: revocacion
        mockMvc.perform(delete(BASE_URL + "/employee/" + empAId).cookie(adminSession))
                .andExpect(status().isNoContent());

        // Assert: no quedan filas activas, pero el historico permanece (2 filas revocadas)
        assertThat(activeCount(empAId)).isZero();
        assertThat(totalCount(empAId)).isEqualTo(2);
        assertThat(revokedByOf(empAId)).isEqualTo(adminId());
    }

    @Test
    void shouldReturn404_whenRevokingEmployeeWithoutActiveAssignment() throws Exception {
        // Act / Assert: empleado sin asignacion activa
        mockMvc.perform(delete(BASE_URL + "/employee/" + empAId).cookie(adminSession))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void shouldReturnOwnAssignments_whenEmployeeQueriesSelf() throws Exception {
        // Arrange
        setAssignments(adminSession, empAId, spaceX, "[1]").andExpect(status().isOk());
        Cookie empSession = login(EMP_A_LOGIN, EMP_PASSWORD);

        // Act / Assert
        mockMvc.perform(get(BASE_URL + "/employee/" + empAId).cookie(empSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].employeeId").value(empAId));
    }

    @Test
    void shouldListAllActiveAssignments_whenAdminLists() throws Exception {
        // Arrange: dos empleados con asignaciones activas
        setAssignments(adminSession, empAId, spaceX, "[1]").andExpect(status().isOk());
        setAssignments(adminSession, empBId, spaceY, "[2]").andExpect(status().isOk());

        // Act / Assert: el ADMIN ve la pagina con ambas
        mockMvc.perform(get(BASE_URL).cookie(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void shouldReturnEmployeeAssignments_whenAdminQueriesAnyEmployee() throws Exception {
        // Arrange
        setAssignments(adminSession, empAId, spaceX, "[1,2]").andExpect(status().isOk());

        // Act / Assert: el ADMIN consulta las de otro empleado sin restriccion de pertenencia
        mockMvc.perform(get(BASE_URL + "/employee/" + empAId).cookie(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].employeeId").value(empAId));
    }

    @Test
    void shouldReturn403_whenEmployeeQueriesOtherEmployeeAssignments() throws Exception {
        // Arrange
        Cookie empSession = login(EMP_A_LOGIN, EMP_PASSWORD);

        // Act / Assert (BOLA): A intenta ver las de B
        mockMvc.perform(get(BASE_URL + "/employee/" + empBId).cookie(empSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void shouldReturn403_whenEmployeeListsAllAssignments() throws Exception {
        // Arrange
        Cookie empSession = login(EMP_A_LOGIN, EMP_PASSWORD);

        // Act / Assert
        mockMvc.perform(get(BASE_URL).cookie(empSession))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldBeIdempotent_whenPutRepeatsSameSpaceAndDays() throws Exception {
        // Arrange
        setAssignments(adminSession, empAId, spaceX, "[1,2]").andExpect(status().isOk());

        // Act: mismo conjunto de nuevo
        setAssignments(adminSession, empAId, spaceX, "[1,2]").andExpect(status().isOk());

        // Assert: siguen siendo 2 filas activas, sin duplicados
        assertThat(activeCount(empAId)).isEqualTo(2);
        assertThat(totalCount(empAId)).isEqualTo(2);
    }

    @Test
    void shouldReviveAfterRevoke_whenRevokedRowCoexistsWithNewActiveRow() throws Exception {
        // Arrange: asigna, revoca y vuelve a asignar la misma plaza/dia
        setAssignments(adminSession, empAId, spaceX, "[1]").andExpect(status().isOk());
        mockMvc.perform(delete(BASE_URL + "/employee/" + empAId).cookie(adminSession))
                .andExpect(status().isNoContent());

        // Act: reasignar (X, dia 1) tras la revocacion -> el indice filtrado lo permite
        setAssignments(adminSession, empAId, spaceX, "[1]").andExpect(status().isOk());

        // Assert: 1 fila activa + 1 revocada coexisten para (X, dia 1)
        assertThat(activeRowsForSpaceDay(spaceX, 1)).isEqualTo(1);
        assertThat(totalRowsForSpaceDay(spaceX, 1)).isEqualTo(2);
    }

    private void race(CountDownLatch ready, CountDownLatch fire, long employeeId, List<Integer> statuses) {
        try {
            ready.countDown();
            assertThat(fire.await(10, TimeUnit.SECONDS)).isTrue();
            int statusCode = setAssignments(adminSession, employeeId, spaceX, "[1]")
                    .andReturn().getResponse().getStatus();
            statuses.add(statusCode);
        } catch (Exception ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Fallo en el alta concurrente", ex);
        }
    }

    private ResultActions setAssignments(Cookie session, long employeeId, long spaceId, String days)
            throws Exception {
        String body = "{\"parkingSpaceId\":" + spaceId + ",\"daysOfWeek\":" + days + "}";
        return mockMvc.perform(put(BASE_URL + "/employee/" + employeeId).cookie(session)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private Cookie login(String login, String password) throws Exception {
        Cookie cookie = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post(LOGIN_URL)
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

    private long adminId() {
        return idOfEmployee(ADMIN_LOGIN);
    }

    private int activeCount(long employeeId) {
        return count("SELECT COUNT(*) FROM dbo.fixed_assignments WHERE employee_id = ? AND active = 1",
                employeeId);
    }

    private int totalCount(long employeeId) {
        return count("SELECT COUNT(*) FROM dbo.fixed_assignments WHERE employee_id = ?", employeeId);
    }

    private int activeRowsForSpaceDay(long spaceId, int day) {
        return count("SELECT COUNT(*) FROM dbo.fixed_assignments "
                + "WHERE parking_space_id = ? AND day_of_week = ? AND active = 1", spaceId, day);
    }

    private int totalRowsForSpaceDay(long spaceId, int day) {
        return count("SELECT COUNT(*) FROM dbo.fixed_assignments "
                + "WHERE parking_space_id = ? AND day_of_week = ?", spaceId, day);
    }

    private long createdByOf(long employeeId) {
        Long value = jdbcTemplate.queryForObject(
                "SELECT TOP 1 created_by_id FROM dbo.fixed_assignments WHERE employee_id = ? AND active = 1",
                Long.class, employeeId);
        return value == null ? 0L : value;
    }

    private long revokedByOf(long employeeId) {
        Long value = jdbcTemplate.queryForObject(
                "SELECT TOP 1 revoked_by_id FROM dbo.fixed_assignments WHERE employee_id = ? AND active = 0",
                Long.class, employeeId);
        return value == null ? 0L : value;
    }

    private int count(String sql, Object... args) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }
}
