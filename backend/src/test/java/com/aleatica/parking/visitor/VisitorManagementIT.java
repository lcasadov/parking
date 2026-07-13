package com.aleatica.parking.visitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aleatica.parking.support.BaseIntegrationTest;
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
 * Tests de integracion de fichas de visitante contra un SQL Server real (Testcontainers):
 * alta con unicidad de {@code nationalId} garantizada por el INDICE UNICO real
 * ({@code UX_visitors_national_id}), edicion que afecta solo a futuras reservas (la reserva
 * referencia al visitante por id, sin denormalizar), busqueda por filtros y RBAC
 * ({@code ADMIN} 200 / {@code EMPLOYEE} 403 / sin sesion 401).
 *
 * <p>Todas las aserciones de listados son independientes del orden: filtran por campo
 * unico o usan {@code everyItem}, nunca asumen que la propia fila sea la primera. La
 * limpieza FK-safe la realiza {@link BaseIntegrationTest#resetDomainState}.</p>
 */
class VisitorManagementIT extends BaseIntegrationTest {

    private static final String BASE_URL = "/api/v1/visitors";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String SESSION_COOKIE = "parking_SESSION";

    private static final String ADMIN_LOGIN = "admin";
    private static final String ADMIN_PASSWORD = "Admin#Parking2026";

    private static final String EMP_LOGIN = "ittest.vis.emp";
    private static final String EMP_PASSWORD = "Visitor#Pass1word";

    private static final String NATIONAL_ID = "X1234567Z";
    private static final LocalDate TODAY = LocalDate.now(ZoneOffset.UTC);
    private static final LocalDate FUTURE = TODAY.plusDays(3);
    private static final LocalDate PAST = TODAY.minusDays(3);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Cookie adminSession;
    private Cookie empSession;

    @BeforeEach
    void seed() throws Exception {
        insertEmployee(EMP_LOGIN);
        adminSession = login(ADMIN_LOGIN, ADMIN_PASSWORD);
        empSession = login(EMP_LOGIN, EMP_PASSWORD);
    }

    // ---- Alta + unicidad ----

    @Test
    void shouldCreateVisitor_whenNationalIdIsUnique() throws Exception {
        // Act / Assert: 201 con la ficha y createdById = admin
        createVisitor(adminSession, "Ada", "Lovelace", NATIONAL_ID)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nationalId").value(NATIONAL_ID))
                .andExpect(jsonPath("$.firstName").value("Ada"))
                .andExpect(jsonPath("$.createdById").value((int) idOfEmployee(ADMIN_LOGIN)));
        assertThat(countVisitors(NATIONAL_ID)).isEqualTo(1);
    }

    @Test
    void shouldReturn409_whenCreatingVisitorWithDuplicateNationalId() throws Exception {
        // Arrange: ya existe una ficha con NATIONAL_ID
        createVisitor(adminSession, "Ada", "Lovelace", NATIONAL_ID).andExpect(status().isCreated());

        // Act / Assert: la segunda alta viola el indice unico -> 409
        createVisitor(adminSession, "Grace", "Hopper", NATIONAL_ID)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fields.nationalId").exists());
        assertThat(countVisitors(NATIONAL_ID)).isEqualTo(1);
    }

    @Test
    void shouldReturn400_whenCreatingVisitorWithoutRequiredFields() throws Exception {
        // Act / Assert: nationalId ausente -> 400
        String body = "{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\"}";
        mockMvc.perform(post(BASE_URL).cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.nationalId").exists());
    }

    // ---- Edicion: afecta solo a futuras reservas ----

    @Test
    void shouldAffectOnlyFutureReservations_whenUpdatingVisitorCard() throws Exception {
        // Arrange: visitante con una reserva pasada y una futura (sembradas)
        long visitorId = insertVisitor("Ada", "Lovelace", NATIONAL_ID, "OLD1234");
        long spaceA = insertSpace("P-VIS-A");
        long spaceB = insertSpace("P-VIS-B");
        long pastReservationId = insertReservation(visitorId, spaceA, PAST, "reserva pasada");
        long futureReservationId = insertReservation(visitorId, spaceB, FUTURE, "reserva futura");

        // Act: se cambia la matricula de la ficha
        String body = "{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\",\"nationalId\":\"" + NATIONAL_ID
                + "\",\"licensePlate\":\"NEW5678\"}";
        mockMvc.perform(put(BASE_URL + "/" + visitorId).cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.licensePlate").value("NEW5678"));

        // Assert: la ficha se actualiza; las reservas (pasada y futura) NO cambian sus
        // propios datos (referencian al visitante por id, sin denormalizar sus campos).
        assertThat(licensePlateOf(visitorId)).isEqualTo("NEW5678");
        assertThat(reservationDateOf(pastReservationId)).isEqualTo(PAST);
        assertThat(reservationDateOf(futureReservationId)).isEqualTo(FUTURE);
        assertThat(reservationNotesOf(pastReservationId)).isEqualTo("reserva pasada");
        assertThat(reservationNotesOf(futureReservationId)).isEqualTo("reserva futura");
    }

    // ---- Busqueda por filtros ----

    @Test
    void shouldFilterByNationalId_whenSearchingVisitors() throws Exception {
        // Arrange: dos fichas distintas
        insertVisitor("Ada", "Lovelace", NATIONAL_ID, "AAA111");
        insertVisitor("Grace", "Hopper", "Y7654321X", "BBB222");

        // Act / Assert: filtro por nationalId devuelve solo la coincidente (orden-independiente)
        mockMvc.perform(get(BASE_URL).cookie(adminSession).param("q", NATIONAL_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[*].nationalId", everyItem(equalTo(NATIONAL_ID))));
    }

    // ---- RBAC ----

    @Test
    void shouldReturn403_whenEmployeeAccessesVisitorsEndpoints() throws Exception {
        mockMvc.perform(get(BASE_URL).cookie(empSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void shouldReturn401_whenRequestHasNoSession() throws Exception {
        mockMvc.perform(get(BASE_URL)).andExpect(status().isUnauthorized());
    }

    // ---- Helpers ----

    private ResultActions createVisitor(Cookie session, String first, String last, String nationalId)
            throws Exception {
        String body = "{\"firstName\":\"" + first + "\",\"lastName\":\"" + last
                + "\",\"nationalId\":\"" + nationalId + "\"}";
        return mockMvc.perform(post(BASE_URL).cookie(session)
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

    private long insertVisitor(String first, String last, String nationalId, String plate) {
        jdbcTemplate.update(
                "INSERT INTO dbo.visitors (first_name, last_name, national_id, license_plate, "
                        + "created_by_id) VALUES (?, ?, ?, ?, ?)",
                first, last, nationalId, plate, idOfEmployee(ADMIN_LOGIN));
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM dbo.visitors WHERE national_id = ?", Long.class, nationalId);
        return id == null ? 0L : id;
    }

    private long insertReservation(long visitorId, long spaceId, LocalDate date, String notes) {
        jdbcTemplate.update(
                "INSERT INTO dbo.visitor_reservations (visitor_id, parking_space_id, reservation_date, "
                        + "notes, created_by_id, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                visitorId, spaceId, Date.valueOf(date), notes, idOfEmployee(ADMIN_LOGIN),
                Timestamp.from(Instant.now()));
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM dbo.visitor_reservations WHERE parking_space_id = ? AND reservation_date = ?",
                Long.class, spaceId, Date.valueOf(date));
        return id == null ? 0L : id;
    }

    private long insertSpace(String label) {
        jdbcTemplate.update("INSERT INTO dbo.parking_spaces (number, label, active) VALUES ((SELECT ISNULL(MAX(number),1000)+1 FROM dbo.parking_spaces), ?, 1)", label);
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

    private int countVisitors(String nationalId) {
        Integer value = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.visitors WHERE national_id = ?", Integer.class, nationalId);
        return value == null ? 0 : value;
    }

    private String licensePlateOf(long visitorId) {
        return jdbcTemplate.queryForObject(
                "SELECT license_plate FROM dbo.visitors WHERE id = ?", String.class, visitorId);
    }

    private LocalDate reservationDateOf(long reservationId) {
        Date date = jdbcTemplate.queryForObject(
                "SELECT reservation_date FROM dbo.visitor_reservations WHERE id = ?",
                Date.class, reservationId);
        return date == null ? null : date.toLocalDate();
    }

    private String reservationNotesOf(long reservationId) {
        return jdbcTemplate.queryForObject(
                "SELECT notes FROM dbo.visitor_reservations WHERE id = ?", String.class, reservationId);
    }
}
