package com.aleatica.parking.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aleatica.parking.support.BaseIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Tests de integracion del pipeline de exportacion contra un SQL Server real (Testcontainers):
 * verifica de extremo a extremo la exclusion de credenciales en el XLSX (1.1), la exportacion de
 * auditoria en CSV (1.2), la comprobacion de objeto en "mis datos"/"mis solicitudes" (1.4/1.5),
 * el fichero de solo cabecera cuando no hay filas (1.10), la sanitizacion de formulas end-to-end
 * (1.11), el registro del evento en {@code audit_log} y el limite de tasa 429 (1.9). El
 * aislamiento entre ITs (BD + reset del limitador) lo garantiza {@link BaseIntegrationTest}.
 */
class ExportIT extends BaseIntegrationTest {

    private static final String EMPLOYEES_EXPORT = "/api/v1/employees/export";
    private static final String MY_DATA_EXPORT = "/api/v1/employees/me/export";
    private static final String MY_REQUESTS_EXPORT = "/api/v1/requests/mine/export";
    private static final String AUDIT_EXPORT = "/api/v1/audit/export";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String SESSION_COOKIE = "parking_SESSION";

    private static final String ADMIN_LOGIN = "admin";
    private static final String ADMIN_PASSWORD = "Admin#Parking2026";
    private static final String EMP_LOGIN = "ittest.export.emp";
    private static final String EMP_PASSWORD = "Export#Pass1word";
    private static final String OTHER_LOGIN = "ittest.export.other";
    private static final String XLSX_MIME =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private long empId;
    private long otherId;

    @BeforeEach
    void seed() {
        empId = insertEmployee(EMP_LOGIN, "EMPLOYEE", "Juan", null);
        otherId = insertEmployee(OTHER_LOGIN, "EMPLOYEE", "Ana", null);
    }

    @Test
    void should_export_employees_xlsx_when_admin_requests() throws Exception {
        // Arrange
        Cookie admin = login(ADMIN_LOGIN, ADMIN_PASSWORD);

        // Act
        byte[] body = perform(get(EMPLOYEES_EXPORT).param("format", "xlsx").cookie(admin),
                XLSX_MIME, ".xlsx");

        // Assert (1.1): cabeceras sin campos de credenciales; el empleado sembrado esta presente
        List<String> headers = xlsxHeaders(body);
        assertThat(headers)
                .doesNotContain("passwordHash", "failedLoginAttempts", "lockedUntil")
                .contains("login", "email");
        assertThat(xlsxColumn(body, headers.indexOf("login"))).contains(EMP_LOGIN, OTHER_LOGIN);
    }

    @Test
    void should_export_audit_csv_when_admin_requests() throws Exception {
        // Arrange: una entrada de auditoria previa
        insertAudit(empId, "CREATE_REQUEST");
        Cookie admin = login(ADMIN_LOGIN, ADMIN_PASSWORD);

        // Act
        byte[] body = perform(get(AUDIT_EXPORT).param("format", "csv").cookie(admin),
                "text/csv", ".csv");

        // Assert (1.2): CSV con cabecera de auditoria
        assertThat(new String(body, StandardCharsets.UTF_8)).contains("action").contains("occurredAt");
    }

    @Test
    void should_export_only_own_personal_data_when_employee_requests_my_data() throws Exception {
        // Arrange
        Cookie emp = login(EMP_LOGIN, EMP_PASSWORD);

        // Act
        byte[] body = perform(get(MY_DATA_EXPORT).param("format", "csv").cookie(emp),
                "text/csv", ".csv");

        // Assert (1.4): contiene el propio login y NO el de otro empleado; sin campos "Solo admins"
        String csv = new String(body, StandardCharsets.UTF_8);
        assertThat(csv).contains(EMP_LOGIN).doesNotContain(OTHER_LOGIN);
        assertThat(csv).doesNotContain("mobilePhone").doesNotContain("licensePlate");
    }

    @Test
    void should_export_only_own_requests_when_employee_requests_my_requests() throws Exception {
        // Arrange: dos solicitudes del empleado y una de otro
        insertRequest(empId, LocalDate.parse("2026-07-10"));
        insertRequest(empId, LocalDate.parse("2026-07-11"));
        insertRequest(otherId, LocalDate.parse("2026-07-12"));
        Cookie emp = login(EMP_LOGIN, EMP_PASSWORD);

        // Act
        byte[] body = perform(get(MY_REQUESTS_EXPORT).param("format", "csv").cookie(emp),
                "text/csv", ".csv");

        // Assert (1.5): solo las solicitudes propias (BOLA). Se comprueba por las fechas
        // solicitadas, unicas por solicitud, para no confundir con digitos de ids en timestamps.
        String csv = new String(body, StandardCharsets.UTF_8);
        long dataRows = csv.lines().count() - 1; // menos la cabecera
        assertThat(dataRows).isEqualTo(2);
        assertThat(csv)
                .contains("2026-07-10", "2026-07-11")
                .doesNotContain("2026-07-12");
    }

    @Test
    void should_emit_header_only_file_when_dataset_is_empty() throws Exception {
        // Arrange: empleado sin solicitudes
        Cookie emp = login(EMP_LOGIN, EMP_PASSWORD);

        // Act (1.10)
        byte[] body = perform(get(MY_REQUESTS_EXPORT).param("format", "csv").cookie(emp),
                "text/csv", ".csv");

        // Assert: solo la cabecera, ninguna fila de datos
        String csv = new String(body, StandardCharsets.UTF_8);
        assertThat(csv.lines().count()).isEqualTo(1);
        assertThat(csv).contains("employeeId");
    }

    @Test
    void should_sanitize_formula_prefixes_when_writing_csv() throws Exception {
        // Arrange: un empleado cuyo nombre empieza por '=' (inyeccion de formula)
        insertEmployee("ittest.evil", "EMPLOYEE", "=SUM(1+1)", null);
        Cookie admin = login(ADMIN_LOGIN, ADMIN_PASSWORD);

        // Act (1.11)
        byte[] body = perform(get(EMPLOYEES_EXPORT).param("format", "csv").cookie(admin),
                "text/csv", ".csv");

        // Assert: el valor peligroso se emite prefijado con apostrofo, nunca como formula cruda
        String csv = new String(body, StandardCharsets.UTF_8);
        assertThat(csv).contains("'=SUM(1+1)").doesNotContain(",\"=SUM(1+1)\"");
    }

    @Test
    void should_record_audit_event_when_admin_exports() throws Exception {
        // Arrange
        Cookie admin = login(ADMIN_LOGIN, ADMIN_PASSWORD);

        // Act
        perform(get(EMPLOYEES_EXPORT).cookie(admin), XLSX_MIME, ".xlsx");

        // Assert: la exportacion queda registrada en audit_log con su accion
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.audit_log WHERE action = 'EXPORT_EMPLOYEES'", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void should_return_429_when_sixth_export_within_one_minute() throws Exception {
        // Arrange
        Cookie admin = login(ADMIN_LOGIN, ADMIN_PASSWORD);

        // Act: cinco exportaciones permitidas
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get(EMPLOYEES_EXPORT).cookie(admin)).andExpect(status().isOk());
        }

        // Assert (1.9): la sexta dentro de la misma ventana se rechaza con 429
        mockMvc.perform(get(EMPLOYEES_EXPORT).cookie(admin))
                .andExpect(status().isTooManyRequests());
    }

    // ---- helpers ----

    private byte[] perform(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder,
            String expectedContentType, String expectedExtension) throws Exception {
        MvcResult result = mockMvc.perform(builder)
                .andExpect(status().isOk())
                .andReturn();
        assertThat(result.getResponse().getContentType()).contains(expectedContentType);
        assertThat(result.getResponse().getHeader("Content-Disposition"))
                .contains("attachment").contains(expectedExtension);
        return result.getResponse().getContentAsByteArray();
    }

    private static List<String> xlsxHeaders(byte[] bytes) throws Exception {
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Row header = wb.getSheetAt(0).getRow(0);
            List<String> headers = new ArrayList<>();
            header.forEach(cell -> headers.add(cell.getStringCellValue()));
            return headers;
        }
    }

    private static List<String> xlsxColumn(byte[] bytes, int columnIndex) throws Exception {
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheetAt(0);
            List<String> values = new ArrayList<>();
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                values.add(sheet.getRow(r).getCell(columnIndex).getStringCellValue());
            }
            return values;
        }
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

    private long insertEmployee(String login, String role, String firstName, String plate) {
        jdbcTemplate.update(
                "INSERT INTO dbo.employees "
                        + "(first_name, last_name, login, email, password_hash, password_must_change, "
                        + "is_corporate, auth_origin, role, enabled, active, mobile_phone, license_plate) "
                        + "VALUES (?, 'User', ?, ?, ?, 0, 0, 'LOCAL', ?, 1, 1, '600100200', ?)",
                firstName, login, login + "@aleatica.com", passwordEncoder.encode(EMP_PASSWORD),
                role, plate);
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM dbo.employees WHERE login = ?", Long.class, login);
        return id == null ? 0L : id;
    }

    private void insertRequest(long employeeId, LocalDate date) {
        jdbcTemplate.update(
                "INSERT INTO dbo.requests (employee_id, requested_date, status, created_at) "
                        + "VALUES (?, ?, 'PENDING', ?)",
                employeeId, date, Timestamp.from(Instant.now()));
    }

    private void insertAudit(long actorId, String action) {
        jdbcTemplate.update(
                "INSERT INTO dbo.audit_log (actor_employee_id, action, entity_type, occurred_at) "
                        + "VALUES (?, ?, 'Request', ?)",
                actorId, action, Timestamp.from(Instant.now()));
    }
}
