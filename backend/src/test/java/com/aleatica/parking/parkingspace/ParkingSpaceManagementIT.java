package com.aleatica.parking.parkingspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

/**
 * Tests de integracion de gestion de plazas contra un SQL Server real
 * (Testcontainers): CRUD end-to-end, configuracion masiva del total preservando
 * historico, autorizacion por rol y, sobre todo, que el indice unico
 * {@code UX_parking_spaces_label} de la BD real fuerza el {@code 409} en colision,
 * incluida el alta concurrente del mismo {@code label}.
 */
class ParkingSpaceManagementIT extends BaseIntegrationTest {

    private static final String BASE_URL = "/api/v1/parking-spaces";
    private static final String CONFIGURE_URL = BASE_URL + "/configure";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String SESSION_COOKIE = "parking_SESSION";

    private static final String ADMIN_LOGIN = "admin";
    private static final String ADMIN_PASSWORD = "Admin#Parking2026";

    private static final String EMP_LOGIN = "ittest.space.rbac";
    private static final String EMP_EMAIL = "ittest.space.rbac@aleatica.com";
    private static final String EMP_PASSWORD = "Rbac#Pass1word";

    private static final String LABEL = "P-08";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Cookie adminSession;

    @BeforeEach
    void cleanAndLogin() throws Exception {
        // fixed_assignments referencia parking_spaces y employees (FK): se limpia
        // primero por si otro IT del contenedor compartido dejo filas hijas.
        jdbcTemplate.update("DELETE FROM dbo.fixed_assignments");
        jdbcTemplate.update("DELETE FROM dbo.parking_spaces");
        jdbcTemplate.update("DELETE FROM dbo.login_log");
        jdbcTemplate.update("DELETE FROM dbo.employees WHERE login = ?", EMP_LOGIN);
        adminSession = login(ADMIN_LOGIN, ADMIN_PASSWORD);
    }

    @Test
    void shouldCreateSpace_whenLabelIsUnique() throws Exception {
        // Act
        createSpace(LABEL).andExpect(status().isCreated())
                .andExpect(jsonPath("$.label").value(LABEL))
                .andExpect(jsonPath("$.active").value(true));

        // Assert: aparece en el listado
        mockMvc.perform(get(BASE_URL).cookie(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].label").value(LABEL));
    }

    @Test
    void shouldReturn409_whenCreatingSpaceWithExistingLabel() throws Exception {
        // Arrange
        createSpace(LABEL).andExpect(status().isCreated());

        // Act / Assert: mismo label -> el indice unico UX_parking_spaces_label fuerza 409
        createSpace(LABEL)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fields.label").exists());
        assertThat(spaceCount(LABEL)).isEqualTo(1);
    }

    @Test
    void shouldReturn409_whenConcurrentInsertSameLabel() throws Exception {
        // Arrange: dos altas concurrentes del mismo label; la red dura es el indice unico
        int threads = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch fire = new CountDownLatch(1);
        List<Integer> statuses = new CopyOnWriteArrayList<>();
        try {
            for (int i = 0; i < threads; i++) {
                pool.submit(() -> race(ready, fire, statuses));
            }
            ready.await(10, TimeUnit.SECONDS);
            fire.countDown();
            pool.shutdown();
            assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }

        // Assert: exactamente un alta correcta (201) y una en conflicto (409); una sola fila
        assertThat(Collections.frequency(statuses, 201)).isEqualTo(1);
        assertThat(Collections.frequency(statuses, 409)).isEqualTo(1);
        assertThat(spaceCount(LABEL)).isEqualTo(1);
    }

    @Test
    void shouldUpdateAndDeactivate_whenAdminOperates() throws Exception {
        // Arrange
        long id = createAndGetId(LABEL);

        // Act / Assert: edicion del label
        updateSpace(id, "{\"label\":\"P-09\",\"active\":true}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("P-09"));

        // Desactivar la plaza la excluye de disponibilidad (active = false persistido)
        updateSpace(id, "{\"label\":\"P-09\",\"active\":false}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        assertThat(activeFlag(id)).isFalse();
    }

    @Test
    void shouldReturn409_whenUpdatingToLabelUsedByAnotherSpace() throws Exception {
        // Arrange: dos plazas
        createAndGetId("P-08");
        long second = createAndGetId("P-09");

        // Act / Assert: editar la segunda al label de la primera -> 409
        updateSpace(second, "{\"label\":\"P-08\",\"active\":true}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fields.label").exists());
    }

    @Test
    void shouldAdjustTotalPreservingHistory_whenAdminConfigures() throws Exception {
        // Act: configurar 5 plazas en un parque vacio
        mockMvc.perform(post(CONFIGURE_URL).cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"total\":5}"))
                .andExpect(status().isOk());
        assertThat(activeCount()).isEqualTo(5);

        // Act: reducir a 2 -> desactiva 3 sobrantes SIN borrar la fila (historico preservado)
        mockMvc.perform(post(CONFIGURE_URL).cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"total\":2}"))
                .andExpect(status().isOk());
        assertThat(activeCount()).isEqualTo(2);
        assertThat(totalCount()).isEqualTo(5);
    }

    @Test
    void shouldReturn403_whenEmployeeManagesSpaces() throws Exception {
        // Arrange: empleado con rol EMPLOYEE y su sesion
        insertEmployee(EMP_LOGIN, EMP_EMAIL, passwordEncoder.encode(EMP_PASSWORD));
        Cookie empSession = login(EMP_LOGIN, EMP_PASSWORD);

        // Act / Assert
        mockMvc.perform(post(BASE_URL).cookie(empSession)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"label\":\"P-99\"}"))
                .andExpect(status().isForbidden());
    }

    private void race(CountDownLatch ready, CountDownLatch fire, List<Integer> statuses) {
        try {
            ready.countDown();
            fire.await(10, TimeUnit.SECONDS);
            int statusCode = mockMvc.perform(post(BASE_URL).cookie(adminSession)
                            .contentType(MediaType.APPLICATION_JSON).content(createBody(LABEL)))
                    .andReturn().getResponse().getStatus();
            statuses.add(statusCode);
        } catch (Exception ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Fallo en el alta concurrente", ex);
        }
    }

    private org.springframework.test.web.servlet.ResultActions createSpace(String label)
            throws Exception {
        return mockMvc.perform(post(BASE_URL).cookie(adminSession)
                .contentType(MediaType.APPLICATION_JSON).content(createBody(label)));
    }

    private org.springframework.test.web.servlet.ResultActions updateSpace(long id, String body)
            throws Exception {
        return mockMvc.perform(put(BASE_URL + "/" + id).cookie(adminSession)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private long createAndGetId(String label) throws Exception {
        createSpace(label).andExpect(status().isCreated());
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM dbo.parking_spaces WHERE label = ?", Long.class, label);
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
                "SELECT active FROM dbo.parking_spaces WHERE id = ?", Boolean.class, id);
        return Boolean.TRUE.equals(value);
    }

    private int spaceCount(String label) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.parking_spaces WHERE label = ?", Integer.class, label);
        return count == null ? 0 : count;
    }

    private int activeCount() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.parking_spaces WHERE active = 1", Integer.class);
        return count == null ? 0 : count;
    }

    private int totalCount() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.parking_spaces", Integer.class);
        return count == null ? 0 : count;
    }

    private static String createBody(String label) {
        return "{\"label\":\"" + label + "\"}";
    }
}
