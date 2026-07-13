package com.aleatica.parking.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aleatica.parking.support.BaseIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Tests de integracion de gestion de empleados contra un SQL Server real
 * (Testcontainers): verifica el CRUD end-to-end, el reset de contrasena, la
 * autorizacion por rol y, sobre todo, que el indice unico {@code UX_employees_login}
 * / {@code UX_employees_email} de la BD real fuerza el {@code 409} en colision.
 */
class EmployeeManagementIT extends BaseIntegrationTest {

    private static final String BASE_URL = "/api/v1/employees";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String SESSION_COOKIE = "parking_SESSION";

    private static final String ADMIN_LOGIN = "admin";
    private static final String ADMIN_PASSWORD = "Admin#Parking2026";

    private static final String TEST_LOGIN = "ittest.employee";
    private static final String TEST_EMAIL = "ittest.employee@aleatica.com";
    private static final String EMP_LOGIN = "ittest.rbac";
    private static final String EMP_EMAIL = "ittest.rbac@aleatica.com";
    private static final String EMP_PASSWORD = "Rbac#Pass1word";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Cookie adminSession;

    @BeforeEach
    void login() throws Exception {
        // La limpieza FK-safe de la BD compartida la realiza BaseIntegrationTest#resetDomainState.
        adminSession = login(ADMIN_LOGIN, ADMIN_PASSWORD);
    }

    @Test
    void shouldCreateEmployee_whenLoginAndEmailAreUnique() throws Exception {
        // Act
        createEmployee(TEST_LOGIN, TEST_EMAIL).andExpect(status().isCreated());

        // Assert: aparece en el listado con busqueda por texto, sin asumir su posicion
        // (order-independent: la fila propia debe estar presente sea cual sea el resto del contenido)
        mockMvc.perform(get(BASE_URL).param("q", "ittest").cookie(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].login", hasItem(TEST_LOGIN)))
                .andExpect(jsonPath(
                        "$.content[?(@.login=='" + TEST_LOGIN + "')].active", hasItem(true)));
    }

    @Test
    void shouldReturn409_whenCreatingEmployeeWithExistingLogin() throws Exception {
        // Arrange: primer alta
        createEmployee(TEST_LOGIN, TEST_EMAIL).andExpect(status().isCreated());

        // Act / Assert: mismo login, otro email -> el indice unico UX_employees_login fuerza 409
        createEmployee(TEST_LOGIN, "otro." + TEST_EMAIL)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fields.login").exists());
    }

    @Test
    void shouldReturn409_whenCreatingEmployeeWithExistingEmail() throws Exception {
        // Arrange
        createEmployee(TEST_LOGIN, TEST_EMAIL).andExpect(status().isCreated());

        // Act / Assert: mismo email, otro login -> UX_employees_email fuerza 409
        createEmployee("otro." + TEST_LOGIN, TEST_EMAIL)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fields.email").exists());
    }

    @Test
    void shouldUpdateDeactivateAndReactivate_whenAdminOperates() throws Exception {
        // Arrange
        long id = createAndGetId(TEST_LOGIN, TEST_EMAIL);

        // Act / Assert: edicion
        mockMvc.perform(put(BASE_URL + "/" + id).cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("Editado", TEST_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Editado"));

        // Baja logica
        mockMvc.perform(delete(BASE_URL + "/" + id).cookie(adminSession))
                .andExpect(status().isNoContent());
        assertThat(activeFlag(id)).isFalse();

        // Reactivacion
        mockMvc.perform(post(BASE_URL + "/" + id + "/reactivate").cookie(adminSession))
                .andExpect(status().isNoContent());
        assertThat(activeFlag(id)).isTrue();
    }

    @Test
    void shouldReturnTemporaryPassword_whenResettingInPhase1() throws Exception {
        // Arrange
        long id = createAndGetId(TEST_LOGIN, TEST_EMAIL);

        // Act / Assert: Fase 1 devuelve la temporal y marca password_must_change
        mockMvc.perform(post(BASE_URL + "/" + id + "/reset-password").cookie(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temporaryPassword").isNotEmpty())
                .andExpect(jsonPath("$.mustChange").value(true));
        assertThat(mustChangeFlag(id)).isTrue();
    }

    @Test
    void shouldRoundTripCorporateFlag_underContractKeyIsCorporate() throws Exception {
        // Arrange: alta con isCorporate=true (clave contractual, bug #21)
        mockMvc.perform(post(BASE_URL).cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBodyCorporate(TEST_LOGIN, TEST_EMAIL, true)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isCorporate").value(true));

        // Assert: se persistio (columna is_corporate) y la lectura lo devuelve bajo isCorporate
        assertThat(corporateFlag(TEST_LOGIN)).isTrue();
        mockMvc.perform(get(BASE_URL).param("q", "ittest").cookie(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.content[?(@.login=='" + TEST_LOGIN + "')].isCorporate", hasItem(true)));

        // Act / Assert: la edicion que lo desactiva tambien viaja bajo isCorporate
        long id = jdbcTemplate.queryForObject(
                "SELECT id FROM dbo.employees WHERE login = ?", Long.class, TEST_LOGIN);
        mockMvc.perform(put(BASE_URL + "/" + id).cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBodyCorporate(TEST_EMAIL, false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isCorporate").value(false));
        assertThat(corporateFlag(TEST_LOGIN)).isFalse();
    }

    @Test
    void shouldReturn403_whenEmployeeRoleListsEmployees() throws Exception {
        // Arrange: empleado con rol EMPLOYEE y su sesion
        insertEmployee(EMP_LOGIN, EMP_EMAIL, passwordEncoder.encode(EMP_PASSWORD));
        Cookie empSession = login(EMP_LOGIN, EMP_PASSWORD);

        // Act / Assert
        mockMvc.perform(get(BASE_URL).cookie(empSession))
                .andExpect(status().isForbidden());
    }

    private org.springframework.test.web.servlet.ResultActions createEmployee(
            String login, String email) throws Exception {
        return mockMvc.perform(post(BASE_URL).cookie(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody(login, email)));
    }

    private long createAndGetId(String login, String email) throws Exception {
        createEmployee(login, email).andExpect(status().isCreated());
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM dbo.employees WHERE login = ?", Long.class, login);
        return id == null ? 0L : id;
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

    private void insertEmployee(String login, String email, String hash) {
        jdbcTemplate.update(
                "INSERT INTO dbo.employees "
                        + "(first_name, last_name, login, email, password_hash, password_must_change, "
                        + "is_corporate, auth_origin, role, enabled, active) "
                        + "VALUES ('IT', 'User', ?, ?, ?, 0, 0, 'LOCAL', 'EMPLOYEE', 1, 1)",
                login, email, hash);
    }

    private boolean activeFlag(long id) {
        Boolean value = jdbcTemplate.queryForObject(
                "SELECT active FROM dbo.employees WHERE id = ?", Boolean.class, id);
        return Boolean.TRUE.equals(value);
    }

    private boolean corporateFlag(String login) {
        Boolean value = jdbcTemplate.queryForObject(
                "SELECT is_corporate FROM dbo.employees WHERE login = ?", Boolean.class, login);
        return Boolean.TRUE.equals(value);
    }

    private boolean mustChangeFlag(long id) {
        Boolean value = jdbcTemplate.queryForObject(
                "SELECT password_must_change FROM dbo.employees WHERE id = ?", Boolean.class, id);
        return Boolean.TRUE.equals(value);
    }

    private static String createBody(String login, String email) {
        return "{\"firstName\":\"Juan\",\"lastName\":\"Perez\",\"login\":\"" + login
                + "\",\"email\":\"" + email + "\",\"role\":\"EMPLOYEE\",\"category\":\"EMPLEADO\"}";
    }

    private static String updateBody(String firstName, String email) {
        return "{\"firstName\":\"" + firstName + "\",\"lastName\":\"Perez\",\"email\":\""
                + email + "\",\"role\":\"EMPLOYEE\",\"category\":\"EMPLEADO\"}";
    }

    private static String createBodyCorporate(String login, String email, boolean corporate) {
        return "{\"firstName\":\"Juan\",\"lastName\":\"Perez\",\"login\":\"" + login
                + "\",\"email\":\"" + email + "\",\"isCorporate\":" + corporate
                + ",\"role\":\"EMPLOYEE\",\"category\":\"EMPLEADO\"}";
    }

    private static String updateBodyCorporate(String email, boolean corporate) {
        return "{\"firstName\":\"Juan\",\"lastName\":\"Perez\",\"email\":\"" + email
                + "\",\"isCorporate\":" + corporate + ",\"role\":\"EMPLOYEE\",\"category\":\"EMPLEADO\"}";
    }
}
