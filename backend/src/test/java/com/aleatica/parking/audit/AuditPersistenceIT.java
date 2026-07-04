package com.aleatica.parking.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aleatica.parking.support.BaseIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Tests de integracion de la persistencia y consulta de auditoria contra un SQL Server real
 * (Testcontainers). Verifica lo que solo se observa con AOP + BD reales: el aspecto inserta
 * una entrada enriquecida con actor cuando hay usuario autenticado (spec Req 3), y con actor
 * nulo en una accion del sistema (spec Req 3, actor del sistema); y las consultas
 * {@code GET /audit} / {@code GET /login-logs} aplican los filtros y el RBAC sobre la BD real.
 * El aislamiento entre ITs lo garantiza {@link BaseIntegrationTest#resetDomainState()}
 * (limpia {@code audit_log} y {@code login_log}).
 */
@Import(AuditPersistenceIT.AuditableSampleConfig.class)
class AuditPersistenceIT extends BaseIntegrationTest {

    private static final String AUDIT_URL = "/api/v1/audit";
    private static final String LOGIN_LOGS_URL = "/api/v1/login-logs";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String SESSION_COOKIE = "parking_SESSION";
    private static final String ADMIN_LOGIN = "admin";
    private static final String ADMIN_PASSWORD = "Admin#Parking2026";
    private static final String EMP_LOGIN = "ittest.audit.emp";
    private static final String EMP_PASSWORD = "Audit#Pass1word";
    private static final String SAMPLE_ACTION = "SAMPLE_ACTION";
    private static final String SAMPLE_ENTITY = "Sample";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditableSampleService sampleService;

    private long empId;
    private long adminId;

    @BeforeEach
    void seed() {
        empId = insertEmployee(EMP_LOGIN, "EMPLOYEE");
        adminId = idOfEmployee(ADMIN_LOGIN);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ---- 1.7 / 1.8: AOP persiste en audit_log ----

    @Test
    void should_insert_audit_entry_when_auditable_use_case_succeeds() {
        // Arrange: hay un usuario autenticado (actor conocido)
        authenticateAs(ADMIN_LOGIN);

        // Act: se invoca un caso de uso anotado @Auditable
        sampleService.perform();

        // Assert: se persistio una entrada con el actor, la accion y el entityId derivado
        Map<String, Object> row = latestAuditRow();
        assertThat(row.get("action")).isEqualTo(SAMPLE_ACTION);
        assertThat(row.get("entity_type")).isEqualTo(SAMPLE_ENTITY);
        assertThat(((Number) row.get("actor_employee_id")).longValue()).isEqualTo(adminId);
        assertThat(((Number) row.get("entity_id")).longValue()).isEqualTo(99L);
        assertThat((String) row.get("details")).contains("\"actorLogin\":\"" + ADMIN_LOGIN + "\"");
    }

    @Test
    void should_insert_audit_entry_with_null_actor_when_system_action() {
        // Arrange: sin usuario autenticado (accion del sistema)
        SecurityContextHolder.clearContext();

        // Act
        sampleService.perform();

        // Assert: la entrada se crea con actor nulo
        Map<String, Object> row = latestAuditRow();
        assertThat(row.get("action")).isEqualTo(SAMPLE_ACTION);
        assertThat(row.get("actor_employee_id")).isNull();
    }

    // ---- Consulta /audit sobre BD real ----

    @Test
    void should_return_filtered_audit_page_when_admin_queries_with_valid_filters() throws Exception {
        // Arrange: dos entradas de distinto actor/accion
        insertAudit(adminId, "APPROVE_REQUEST", "Request", 15L);
        insertAudit(empId, "CREATE_REQUEST", "Request", 16L);
        Cookie admin = login(ADMIN_LOGIN, ADMIN_PASSWORD);

        // Act / Assert: filtra por actor + accion
        mockMvc.perform(get(AUDIT_URL)
                        .param("actorEmployeeId", String.valueOf(adminId))
                        .param("action", "APPROVE_REQUEST")
                        .cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].action").value("APPROVE_REQUEST"));
    }

    @Test
    void should_return_403_when_employee_queries_audit_log() throws Exception {
        // Arrange
        Cookie emp = login(EMP_LOGIN, EMP_PASSWORD);

        // Act / Assert
        mockMvc.perform(get(AUDIT_URL).cookie(emp))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void should_return_400_when_audit_window_from_after_to() throws Exception {
        // Arrange
        Cookie admin = login(ADMIN_LOGIN, ADMIN_PASSWORD);

        // Act / Assert
        mockMvc.perform(get(AUDIT_URL)
                        .param("from", "2026-06-01T00:00:00Z")
                        .param("to", "2026-01-01T00:00:00Z")
                        .cookie(admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.from").exists());
    }

    // ---- Consulta /login-logs sobre BD real ----

    @Test
    void should_return_only_invalid_credentials_when_login_log_filtered_by_result() throws Exception {
        // Arrange: un intento OK y otro fallido
        insertLoginLog("jperez", "OK");
        insertLoginLog("jperez", "INVALID_CREDENTIALS");
        Cookie admin = login(ADMIN_LOGIN, ADMIN_PASSWORD);

        // Act / Assert: solo devuelve los INVALID_CREDENTIALS del filtro (mas los reales del login)
        mockMvc.perform(get(LOGIN_LOGS_URL)
                        .param("result", "INVALID_CREDENTIALS")
                        .cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.result != 'INVALID_CREDENTIALS')]").isEmpty());
    }

    @Test
    void should_return_400_when_login_log_result_outside_enum() throws Exception {
        // Arrange
        Cookie admin = login(ADMIN_LOGIN, ADMIN_PASSWORD);

        // Act / Assert
        mockMvc.perform(get(LOGIN_LOGS_URL).param("result", "UNKNOWN").cookie(admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.result").exists());
    }

    @Test
    void should_return_403_when_employee_queries_login_log() throws Exception {
        // Arrange
        Cookie emp = login(EMP_LOGIN, EMP_PASSWORD);

        // Act / Assert
        mockMvc.perform(get(LOGIN_LOGS_URL).cookie(emp))
                .andExpect(status().isForbidden());
    }

    // ---- helpers ----

    private void authenticateAs(String login) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(login, "n/a", java.util.List.of()));
    }

    private Map<String, Object> latestAuditRow() {
        return jdbcTemplate.queryForMap(
                "SELECT TOP 1 actor_employee_id, action, entity_type, entity_id, details "
                        + "FROM dbo.audit_log ORDER BY id DESC");
    }

    private void insertAudit(Long actorId, String action, String entityType, Long entityId) {
        jdbcTemplate.update(
                "INSERT INTO dbo.audit_log (actor_employee_id, action, entity_type, entity_id, "
                        + "occurred_at) VALUES (?, ?, ?, ?, ?)",
                actorId, action, entityType, entityId, Timestamp.from(Instant.now()));
    }

    private void insertLoginLog(String loginAttempted, String result) {
        jdbcTemplate.update(
                "INSERT INTO dbo.login_log (login_attempted, result, phase, occurred_at) "
                        + "VALUES (?, ?, 'PHASE_1', ?)",
                loginAttempted, result, Timestamp.from(Instant.now()));
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

    private long insertEmployee(String login, String role) {
        jdbcTemplate.update(
                "INSERT INTO dbo.employees "
                        + "(first_name, last_name, login, email, password_hash, password_must_change, "
                        + "is_corporate, auth_origin, role, enabled, active) "
                        + "VALUES ('IT', 'User', ?, ?, ?, 0, 0, 'LOCAL', ?, 1, 1)",
                login, login + "@aleatica.com", passwordEncoder.encode(EMP_PASSWORD), role);
        return idOfEmployee(login);
    }

    private long idOfEmployee(String login) {
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM dbo.employees WHERE login = ?", Long.class, login);
        return id == null ? 0L : id;
    }

    /**
     * Servicio de prueba con un metodo anotado {@link Auditable} para ejercitar el aspecto
     * de auditoria de extremo a extremo (proxy Spring AOP sobre un bean real).
     */
    public static class AuditableSampleService {

        /**
         * @return un resultado con {@code getId()} para verificar la derivacion de {@code entityId}
         */
        @Auditable(action = SAMPLE_ACTION, entityType = SAMPLE_ENTITY)
        public SampleResult perform() {
            return new SampleResult(99L);
        }
    }

    /** Resultado de prueba que expone {@code getId()} (deriva {@code entity_id} en el aspecto). */
    public record SampleResult(Long value) {

        /**
         * @return el id de la entidad afectada (derivado por el aspecto)
         */
        public Long getId() {
            return value;
        }
    }

    /** Registra el bean de prueba anotado {@link Auditable} en el contexto del IT. */
    @TestConfiguration
    static class AuditableSampleConfig {

        /**
         * @return el bean de prueba auditable, proxyado por el aspecto de auditoria
         */
        @Bean
        AuditableSampleService auditableSampleService() {
            return new AuditableSampleService();
        }
    }
}
