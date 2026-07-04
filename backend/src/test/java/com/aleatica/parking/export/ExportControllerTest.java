package com.aleatica.parking.export;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aleatica.parking.auth.domain.ClockPort;
import com.aleatica.parking.config.SecurityConfig;
import com.aleatica.parking.export.application.ExportService;
import java.time.Instant;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Tests del adaptador web de exportaciones (@WebMvcTest): contrato HTTP, RBAC (401 sin sesion,
 * 403 por rol), validacion de formato (400), limite de tasa (429) y cabeceras del fichero. La
 * construccion de la tabla se mockea; la serializacion la ejercen los writers reales importados,
 * y el limite de tasa usa un reloj fijo (ventana determinista, sin sleep, S2925).
 */
@WebMvcTest(ExportController.class)
@Import({SecurityConfig.class, CsvExportWriter.class, XlsxExportWriter.class,
        ExportWriters.class, ExportRateLimiter.class, ExportControllerTest.FixedClockConfig.class})
class ExportControllerTest {

    private static final String EMPLOYEES_EXPORT = "/api/v1/employees/export";
    private static final String MY_DATA_EXPORT = "/api/v1/employees/me/export";
    private static final String REQUESTS_EXPORT = "/api/v1/requests/export";
    private static final String MY_REQUESTS_EXPORT = "/api/v1/requests/mine/export";
    private static final String AUDIT_EXPORT = "/api/v1/audit/export";

    private static final String ADMIN = "admin";
    private static final String EMP = "empleado";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_EMPLOYEE = "EMPLOYEE";
    private static final String XLSX_MIME =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String DISPOSITION = "Content-Disposition";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExportService exportService;

    @Autowired
    private ExportRateLimiter rateLimiter;

    @BeforeEach
    void resetRateLimiter() {
        // El limitador es un singleton del contexto cacheado y el reloj es fijo: se resetea antes
        // de cada test para que el conteo de uno no filtre a otro segun el orden (aislamiento).
        rateLimiter.reset();
    }

    private ExportTable table(String baseName) {
        return new ExportTable(baseName, List.of("id", "login"), List.of(List.of("1", "jperez")));
    }

    // ---- RBAC ----

    @Test
    void shouldReturn401_whenUnauthenticatedExportsMyData() throws Exception {
        // Act / Assert (1.6)
        mockMvc.perform(get(MY_DATA_EXPORT)).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403_whenEmployeeExportsRequestsHistory() throws Exception {
        // Act / Assert (1.3)
        mockMvc.perform(get(REQUESTS_EXPORT).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void shouldReturn403_whenEmployeeExportsEmployees() throws Exception {
        // Act / Assert
        mockMvc.perform(get(EMPLOYEES_EXPORT).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403_whenEmployeeExportsAuditLog() throws Exception {
        // Act / Assert
        mockMvc.perform(get(AUDIT_EXPORT).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isForbidden());
    }

    // ---- Formato ----

    @Test
    void shouldReturn400_whenFormatIsUnsupported() throws Exception {
        // Act / Assert (1.7)
        mockMvc.perform(get(EMPLOYEES_EXPORT).param("format", "pdf")
                        .with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.format").exists());
    }

    // ---- Descargas (async StreamingResponseBody) ----

    @Test
    void shouldDefaultToXlsx_whenFormatOmitted() throws Exception {
        // Arrange (1.8)
        BDDMockito.given(exportService.exportEmployees()).willReturn(table("employees"));

        // Act / Assert: sin ?format => xlsx, nombre de fichero con timestamp
        dispatch(get(EMPLOYEES_EXPORT).with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(XLSX_MIME))
                .andExpect(header().string(DISPOSITION, Matchers.containsString("employees-")))
                .andExpect(header().string(DISPOSITION, Matchers.containsString(".xlsx")));
    }

    @Test
    void shouldExportAuditCsv_whenAdminRequestsCsv() throws Exception {
        // Arrange (1.2)
        BDDMockito.given(exportService.exportAuditLog()).willReturn(table("audit-log"));

        // Act / Assert
        dispatch(get(AUDIT_EXPORT).param("format", "csv").with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string(DISPOSITION, Matchers.containsString(".csv")));
    }

    @Test
    void shouldExportOwnData_whenEmployeeRequestsMyData() throws Exception {
        // Arrange (1.4)
        BDDMockito.given(exportService.exportMyData(EMP)).willReturn(table("my-data"));

        // Act / Assert: cualquier usuario autenticado puede pedir sus propios datos
        dispatch(get(MY_DATA_EXPORT).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(XLSX_MIME));
    }

    @Test
    void shouldExportOwnRequests_whenEmployeeRequestsMyRequests() throws Exception {
        // Arrange (1.5)
        BDDMockito.given(exportService.exportMyRequests(EMP))
                .willReturn(table("my-requests"));

        // Act / Assert
        dispatch(get(MY_REQUESTS_EXPORT).param("format", "csv").with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"));
    }

    // ---- Limite de tasa ----

    @Test
    void shouldReturn429_whenSixthExportWithinOneMinute() throws Exception {
        // Arrange (1.9): reloj fijo => las 6 peticiones caen en la misma ventana
        BDDMockito.given(exportService.exportEmployees()).willReturn(table("employees"));

        // Act: 5 exportaciones permitidas
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get(EMPLOYEES_EXPORT).with(user(ADMIN).roles(ROLE_ADMIN)))
                    .andExpect(status().isOk());
        }

        // Assert: la sexta en la misma ventana se rechaza con 429
        mockMvc.perform(get(EMPLOYEES_EXPORT).with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("RATE_LIMIT_EXCEEDED"));
    }

    private ResultActions dispatch(MockHttpServletRequestBuilder builder) throws Exception {
        return mockMvc.perform(builder);
    }

    /** Reloj fijo: mantiene las peticiones del limite de tasa en una unica ventana (S2925). */
    @TestConfiguration
    static class FixedClockConfig {

        /**
         * @return un {@link ClockPort} que siempre devuelve el mismo instante
         */
        @Bean
        ClockPort clockPort() {
            Instant fixed = Instant.parse("2026-07-04T10:00:00Z");
            return () -> fixed;
        }
    }
}
