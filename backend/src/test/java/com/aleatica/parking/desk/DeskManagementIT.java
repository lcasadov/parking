package com.aleatica.parking.desk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aleatica.parking.support.BaseIntegrationTest;
import jakarta.servlet.http.Cookie;
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
 * Tests de integracion del CRUD de puestos contra un SQL Server real (Testcontainers):
 * alta con numero unico 1-65, unicidad (409), rango fuera de 1-65 (400), RBAC (403 para
 * EMPLOYEE en las mutaciones), cambio de categoria y, sobre todo, que un puesto
 * desactivado desaparece de la disponibilidad de tipo {@code DESK} (spec init-desks, Reqs
 * "Alta de puesto numerado" y "Edicion y activacion/desactivacion de puesto").
 *
 * <p>Las aserciones son independientes del orden: consultan por {@code number} o
 * {@code parkingSpaceId} con filtros JSONPath. La limpieza FK-safe (incluida la tabla
 * {@code desks}) la realiza {@link BaseIntegrationTest#resetDomainState}.</p>
 */
class DeskManagementIT extends BaseIntegrationTest {

    private static final String BASE_URL = "/api/v1/desks";
    private static final String AVAILABILITY_URL = "/api/v1/availability";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String SESSION_COOKIE = "parking_SESSION";

    private static final String ADMIN_LOGIN = "admin";
    private static final String ADMIN_PASSWORD = "Admin#Parking2026";
    private static final String EMP_LOGIN = "ittest.desk.emp";
    private static final String EMP_PASSWORD = "Desk#Pass1word";

    private static final LocalDate DATE = LocalDate.now(ZoneOffset.UTC).plusDays(1);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Cookie adminSession;
    private Cookie empSession;

    @BeforeEach
    void cleanAndSeed() throws Exception {
        adminSession = login(ADMIN_LOGIN, ADMIN_PASSWORD);
        insertEmployee(EMP_LOGIN);
        empSession = login(EMP_LOGIN, EMP_PASSWORD);
    }

    @Test
    void shouldCreateDesk_whenNumberInRangeAndUnique() throws Exception {
        // Act
        createDesk(adminSession, 12, "STANDARD")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.number").value(12))
                .andExpect(jsonPath("$.label").value("D-12"))
                .andExpect(jsonPath("$.active").value(true));

        // Assert
        assertThat(deskCount(12)).isEqualTo(1);
    }

    @Test
    void shouldReturn409_whenCreatingDeskWithDuplicateNumber() throws Exception {
        // Arrange
        createDesk(adminSession, 12, "STANDARD").andExpect(status().isCreated());

        // Act / Assert
        createDesk(adminSession, 12, "EXECUTIVE")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fields.number").exists());
        assertThat(deskCount(12)).isEqualTo(1);
    }

    @Test
    void shouldReturn400_whenDeskNumberOutOfRange() throws Exception {
        // Act / Assert: numero 70 fuera del rango 1-65
        createDesk(adminSession, 70, "STANDARD")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.number").exists());
        assertThat(deskCount(70)).isZero();
    }

    @Test
    void shouldReturn403_whenEmployeeCreatesDesk() throws Exception {
        // Act / Assert
        createDesk(empSession, 20, "STANDARD").andExpect(status().isForbidden());
        assertThat(deskCount(20)).isZero();
    }

    @Test
    void shouldUpdateCategory_whenAdminSetsExecutive() throws Exception {
        // Arrange
        createDesk(adminSession, 8, "STANDARD").andExpect(status().isCreated());
        long deskId = deskId(8);

        // Act
        mockMvc.perform(put(BASE_URL + "/" + deskId).cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"category\":\"EXECUTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("EXECUTIVE"));

        // Assert
        assertThat(categoryOf(deskId)).isEqualTo("EXECUTIVE");
    }

    @Test
    void shouldReturn400_whenCoordinatesOutOfRange() throws Exception {
        // Arrange
        createDesk(adminSession, 9, "STANDARD").andExpect(status().isCreated());
        long deskId = deskId(9);

        // Act / Assert: coordenada 150 fuera de 0-100
        mockMvc.perform(put(BASE_URL + "/" + deskId).cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"STANDARD\",\"coordX\":150}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.coordX").exists());
    }

    @Test
    void shouldRemoveDeskFromAvailability_whenDeactivated() throws Exception {
        // Arrange: un puesto activo aparece en la disponibilidad de tipo DESK para la fecha
        createDesk(adminSession, 30, "STANDARD").andExpect(status().isCreated());
        long deskId = deskId(30);
        deskAvailability(adminSession, DATE)
                .andExpect(status().isOk())
                .andExpect(jsonPath(deskFilter(deskId)).exists());

        // Act: se desactiva el puesto
        mockMvc.perform(patch(BASE_URL + "/" + deskId + "/activation").cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        // Assert: el puesto inactivo ya no aparece como disponible
        deskAvailability(adminSession, DATE)
                .andExpect(status().isOk())
                .andExpect(jsonPath(deskFilter(deskId)).doesNotExist());
    }

    @Test
    void shouldReturn404_whenActivatingUnknownDesk() throws Exception {
        // Act / Assert
        mockMvc.perform(patch(BASE_URL + "/999999/activation").cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // ---- Helpers HTTP ----

    private ResultActions createDesk(Cookie session, int number, String category) throws Exception {
        String body = "{\"number\":" + number + ",\"category\":\"" + category
                + "\",\"coordX\":30.5,\"coordY\":47.0}";
        return mockMvc.perform(post(BASE_URL).cookie(session)
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

    private void insertEmployee(String login) {
        jdbcTemplate.update(
                "INSERT INTO dbo.employees "
                        + "(first_name, last_name, login, email, password_hash, password_must_change, "
                        + "is_corporate, auth_origin, role, enabled, active) "
                        + "VALUES ('IT', 'User', ?, ?, ?, 0, 0, 'LOCAL', 'EMPLOYEE', 1, 1)",
                login, login + "@aleatica.com", passwordEncoder.encode(EMP_PASSWORD));
    }

    private long deskId(int number) {
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM dbo.desks WHERE number = ?", Long.class, number);
        return id == null ? 0L : id;
    }

    private String categoryOf(long deskId) {
        return jdbcTemplate.queryForObject(
                "SELECT category FROM dbo.desks WHERE id = ?", String.class, deskId);
    }

    private int deskCount(int number) {
        Integer value = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.desks WHERE number = ?", Integer.class, number);
        return value == null ? 0 : value;
    }
}
