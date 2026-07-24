package com.aleatica.parking.availability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aleatica.parking.support.BaseIntegrationTest;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.http.Cookie;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Tests de integracion de la capability {@code availability-calendar} contra un SQL Server
 * real (Testcontainers): la tabla de verdad de disponibilidad end-to-end (libre, liberada,
 * aprobada, reserva de visitante, inactiva, asignada sin liberar), RBAC del calendario admin
 * (403 EMPLOYEE), privacidad de "Mi Semana" (sin nombres ajenos, 401 sin sesion) y una
 * asercion de ausencia de N+1 (numero de consultas constante frente al numero de plazas).
 *
 * <p>Las aserciones son independientes del orden: consultan por {@code parkingSpaceId} con
 * filtros JSONPath, nunca por indice de un dataset compartido. La limpieza FK-safe la realiza
 * {@link BaseIntegrationTest#resetDomainState}. La propiedad {@code hibernate.generate_statistics}
 * habilita el conteo de sentencias para la prueba de N+1.</p>
 */
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class AvailabilityCalendarIT extends BaseIntegrationTest {

    private static final String AVAILABILITY_URL = "/api/v1/availability";
    private static final String ADMIN_CALENDAR_URL = "/api/v1/calendar/admin";
    private static final String MY_WEEK_URL = "/api/v1/calendar/my-week";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String SESSION_COOKIE = "parking_SESSION";

    private static final String ADMIN_LOGIN = "admin";
    private static final String ADMIN_PASSWORD = "Admin#Parking2026";

    private static final String EMP_A_LOGIN = "ittest.avail.a";
    private static final String EMP_B_LOGIN = "ittest.avail.b";
    private static final String EMP_PASSWORD = "Avail#Pass1word";
    private static final String EMP_A_FIRST = "Ada";
    private static final String EMP_A_LAST = "Lovelace";
    private static final String EMP_B_FIRST = "Grace";
    private static final String EMP_B_LAST = "Hopper";

    private static final LocalDate MONDAY = LocalDate.of(2026, 7, 6);
    private static final LocalDate WEDNESDAY = MONDAY.plusDays(2);
    private static final int WEDNESDAY_DOW = WEDNESDAY.getDayOfWeek().getValue();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Cookie adminSession;
    private Cookie empASession;

    @BeforeEach
    void seed() throws Exception {
        insertEmployee(EMP_A_LOGIN, EMP_A_FIRST, EMP_A_LAST);
        insertEmployee(EMP_B_LOGIN, EMP_B_FIRST, EMP_B_LAST);
        adminSession = login(ADMIN_LOGIN, ADMIN_PASSWORD);
        empASession = login(EMP_A_LOGIN, EMP_PASSWORD);
    }

    // ---- Tabla de verdad de disponibilidad ----

    @Test
    void shouldIncludeSpace_whenFreeForDate() throws Exception {
        long space = insertSpace("P-AV-FREE", true);
        availability(adminSession, WEDNESDAY)
                .andExpect(status().isOk())
                .andExpect(jsonPath(itemFilter(space)).exists());
    }

    @Test
    void shouldIncludeSpace_whenFixedAssignmentReleasedForDate() throws Exception {
        long space = insertSpace("P-AV-REL", true);
        insertFixedAssignment(space, EMP_A_LOGIN, WEDNESDAY_DOW);
        insertRelease(space, EMP_A_LOGIN, WEDNESDAY);
        availability(adminSession, WEDNESDAY)
                .andExpect(status().isOk())
                .andExpect(jsonPath(itemFilter(space)).exists());
    }

    @Test
    void shouldExcludeSpace_whenRequestApprovedForDate() throws Exception {
        long space = insertSpace("P-AV-APR", true);
        insertApprovedRequest(space, EMP_A_LOGIN, WEDNESDAY);
        availability(adminSession, WEDNESDAY)
                .andExpect(status().isOk())
                .andExpect(jsonPath(itemFilter(space)).doesNotExist());
    }

    @Test
    void shouldExcludeSpace_whenVisitorReservationForDate() throws Exception {
        long space = insertSpace("P-AV-VIS", true);
        long visitor = insertVisitor("Alan", "Turing", "T0000001A");
        insertReservation(visitor, space, WEDNESDAY);
        availability(adminSession, WEDNESDAY)
                .andExpect(status().isOk())
                .andExpect(jsonPath(itemFilter(space)).doesNotExist());
    }

    @Test
    void shouldExcludeSpace_whenInactive() throws Exception {
        long space = insertSpace("P-AV-OFF", false);
        availability(adminSession, WEDNESDAY)
                .andExpect(status().isOk())
                .andExpect(jsonPath(itemFilter(space)).doesNotExist());
    }

    @Test
    void shouldExcludeSpace_whenFixedAssignmentActiveWithoutRelease() throws Exception {
        long space = insertSpace("P-AV-FIX", true);
        insertFixedAssignment(space, EMP_A_LOGIN, WEDNESDAY_DOW);
        availability(adminSession, WEDNESDAY)
                .andExpect(status().isOk())
                .andExpect(jsonPath(itemFilter(space)).doesNotExist());
    }

    @Test
    void shouldReturnBadRequest_whenDateMissing() throws Exception {
        mockMvc.perform(get(AVAILABILITY_URL).cookie(adminSession))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.date").exists());
    }

    @Test
    void shouldReturn401_whenAvailabilityWithoutSession() throws Exception {
        mockMvc.perform(get(AVAILABILITY_URL).param("date", WEDNESDAY.toString()))
                .andExpect(status().isUnauthorized());
    }

    // ---- Calendario admin: RBAC y estados ----

    @Test
    void shouldReturnAdminCalendar_whenCallerIsAdmin() throws Exception {
        long space = insertSpace("P-AV-CAL", true);
        insertFixedAssignment(space, EMP_A_LOGIN, WEDNESDAY_DOW);

        mockMvc.perform(get(ADMIN_CALENDAR_URL).param("weekStart", MONDAY.toString()).cookie(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekStart").value(MONDAY.toString()))
                .andExpect(jsonPath(rowFilter(space)).exists())
                .andExpect(jsonPath("$.rows[?(@.parkingSpaceId == " + space
                        + ")].cells[?(@.state == 'ASSIGNED' && @.employeeName == '"
                        + EMP_A_FIRST + " " + EMP_A_LAST + "')]").exists());
    }

    @Test
    void shouldReturnForbidden_whenEmployeeCallsAdminCalendar() throws Exception {
        mockMvc.perform(get(ADMIN_CALENDAR_URL).param("weekStart", MONDAY.toString()).cookie(empASession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void shouldReturnBadRequest_whenWeekStartInvalid() throws Exception {
        mockMvc.perform(get(ADMIN_CALENDAR_URL).param("weekStart", "nope").cookie(adminSession))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.weekStart").exists());
    }

    // ---- Mi Semana: privacidad ----

    @Test
    void shouldReturnOwnWeekWithoutOtherNames_whenEmployeeCallsMyWeek() throws Exception {
        // Arrange: A y B con asignacion fija el mismo dia en plazas distintas
        long spaceA = insertSpace("P-AV-MWA", true);
        long spaceB = insertSpace("P-AV-MWB", true);
        insertFixedAssignment(spaceA, EMP_A_LOGIN, WEDNESDAY_DOW);
        insertFixedAssignment(spaceB, EMP_B_LOGIN, WEDNESDAY_DOW);

        // Act: A consulta su semana
        String body = mockMvc.perform(get(MY_WEEK_URL).param("weekStart", MONDAY.toString())
                        .cookie(empASession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekStart").value(MONDAY.toString()))
                .andExpect(jsonPath("$.days[?(@.state == 'ASSIGNED' && @.parkingSpaceLabel == 'P-AV-MWA')]")
                        .exists())
                .andReturn().getResponse().getContentAsString();

        // Assert: no aparece el nombre del tercero (B) ni su plaza
        assertThat(body).doesNotContain(EMP_B_FIRST).doesNotContain(EMP_B_LAST).doesNotContain("P-AV-MWB");
        assertThat(body).doesNotContain("employeeName");
    }

    @Test
    void shouldReturn401_whenMyWeekWithoutSession() throws Exception {
        mockMvc.perform(get(MY_WEEK_URL).param("weekStart", MONDAY.toString()))
                .andExpect(status().isUnauthorized());
    }

    // ---- Ausencia de N+1: numero de consultas constante frente al numero de plazas ----

    @Test
    void shouldNotScaleQueries_whenAvailabilityHasManySpaces() throws Exception {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();

        // Medida con pocas plazas
        insertActiveSpaces("P-AV-SMALL-", 3);
        long few = countQueries(statistics, WEDNESDAY);

        // Medida con muchas plazas (mismo calculo, mas filas)
        insertActiveSpaces("P-AV-LARGE-", 15);
        long many = countQueries(statistics, WEDNESDAY);

        // Assert: el numero de sentencias NO crece con el numero de plazas (sin N+1)
        assertThat(many).isEqualTo(few);
    }

    private long countQueries(Statistics statistics, LocalDate date) throws Exception {
        statistics.clear();
        availability(adminSession, date).andExpect(status().isOk());
        return statistics.getPrepareStatementCount();
    }

    // ---- Helpers HTTP ----

    private ResultActions availability(Cookie session, LocalDate date) throws Exception {
        return mockMvc.perform(get(AVAILABILITY_URL).param("date", date.toString()).cookie(session));
    }

    private static String itemFilter(long space) {
        return "$.availableResources[?(@.parkingSpaceId == " + space + ")]";
    }

    private static String rowFilter(long space) {
        return "$.rows[?(@.parkingSpaceId == " + space + ")]";
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

    // ---- Helpers de siembra ----

    private void insertActiveSpaces(String prefix, int count) {
        for (int i = 0; i < count; i++) {
            insertSpace(prefix + i, true);
        }
    }

    private long insertSpace(String label, boolean active) {
        jdbcTemplate.update("INSERT INTO dbo.parking_spaces (number, label, active) VALUES ((SELECT ISNULL(MAX(number),1000)+1 FROM dbo.parking_spaces), ?, ?)",
                label, active ? 1 : 0);
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM dbo.parking_spaces WHERE label = ?", Long.class, label);
        return id == null ? 0L : id;
    }

    private void insertEmployee(String login, String first, String last) {
        jdbcTemplate.update(
                "INSERT INTO dbo.employees "
                        + "(first_name, last_name, login, email, password_hash, password_must_change, "
                        + "is_corporate, auth_origin, role, enabled, active) "
                        + "VALUES (?, ?, ?, ?, ?, 0, 0, 'LOCAL', 'EMPLOYEE', 1, 1)",
                first, last, login, login + "@aleatica.com", passwordEncoder.encode(EMP_PASSWORD));
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

    private void insertFixedAssignment(long space, String employeeLogin, int dayOfWeek) {
        jdbcTemplate.update(
                "INSERT INTO dbo.fixed_assignments (resource_id, employee_id, day_of_week, "
                        + "active, created_by_id, created_at) VALUES (?, ?, ?, 1, ?, ?)",
                space, idOfEmployee(employeeLogin), dayOfWeek, idOfEmployee(ADMIN_LOGIN),
                Timestamp.from(Instant.now()));
    }

    private void insertRelease(long space, String employeeLogin, LocalDate date) {
        jdbcTemplate.update(
                "INSERT INTO dbo.releases (resource_id, employee_id, release_date, type, "
                        + "released_by_id, created_at) VALUES (?, ?, ?, 'VOLUNTARY', ?, ?)",
                space, idOfEmployee(employeeLogin), Date.valueOf(date), idOfEmployee(employeeLogin),
                Timestamp.from(Instant.now()));
    }

    private void insertApprovedRequest(long space, String employeeLogin, LocalDate date) {
        jdbcTemplate.update(
                "INSERT INTO dbo.requests (employee_id, requested_date, status, resource_id, "
                        + "resolved_by_id, resolved_at, created_at) VALUES (?, ?, 'APPROVED', ?, ?, ?, ?)",
                idOfEmployee(employeeLogin), Date.valueOf(date), space, idOfEmployee(ADMIN_LOGIN),
                Timestamp.from(Instant.now()), Timestamp.from(Instant.now()));
    }

    private void insertReservation(long visitor, long space, LocalDate date) {
        jdbcTemplate.update(
                "INSERT INTO dbo.visitor_reservations (visitor_id, resource_type, resource_id, reservation_date, "
                        + "created_by_id, created_at) VALUES (?, 'PARKING', ?, ?, ?, ?)",
                visitor, space, Date.valueOf(date), idOfEmployee(ADMIN_LOGIN),
                Timestamp.from(Instant.now()));
    }

    private long idOfEmployee(String login) {
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM dbo.employees WHERE login = ?", Long.class, login);
        return id == null ? 0L : id;
    }
}
