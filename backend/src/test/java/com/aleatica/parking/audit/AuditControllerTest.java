package com.aleatica.parking.audit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aleatica.parking.audit.application.AuditQueryService;
import com.aleatica.parking.audit.application.InvalidDateRangeException;
import com.aleatica.parking.audit.application.LoginLogQueryService;
import com.aleatica.parking.audit.dto.AuditLogEntryResponse;
import com.aleatica.parking.audit.dto.LoginLogEntryResponse;
import com.aleatica.parking.auth.domain.LoginPhase;
import com.aleatica.parking.auth.domain.LoginResult;
import com.aleatica.parking.config.SecurityConfig;
import com.aleatica.parking.employee.dto.PageResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests del adaptador web de auditoria (@WebMvcTest): RBAC (401 sin sesion, 403 para
 * EMPLOYEE), 200 con filtros para ADMIN, y validacion (400 por ventana invalida y por
 * {@code result} fuera del enum). La logica se mockea; aqui solo se verifica el contrato HTTP
 * y la autorizacion.
 */
@WebMvcTest(AuditController.class)
@Import(SecurityConfig.class)
class AuditControllerTest {

    private static final String AUDIT_URL = "/api/v1/audit";
    private static final String LOGIN_LOGS_URL = "/api/v1/login-logs";
    private static final String ADMIN = "admin";
    private static final String EMP = "empleado";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_EMPLOYEE = "EMPLOYEE";
    private static final Instant WHEN = Instant.parse("2026-03-01T10:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditQueryService auditQueryService;

    @MockBean
    private LoginLogQueryService loginLogQueryService;

    // ---- 1.1 / 1.2 / 1.3: /audit ----

    @Test
    void should_return_filtered_audit_page_when_admin_queries_with_valid_filters() throws Exception {
        // Arrange
        given(auditQueryService.list(eq(7L), eq("APPROVE_REQUEST"), any(), any(), any()))
                .willReturn(new PageResponse<>(List.of(auditEntry()), 1, 1, 20, 0, true, true));

        // Act / Assert
        mockMvc.perform(get(AUDIT_URL)
                        .param("actorEmployeeId", "7")
                        .param("action", "APPROVE_REQUEST")
                        .param("from", "2026-01-01T00:00:00Z")
                        .param("to", "2026-06-01T00:00:00Z")
                        .with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("APPROVE_REQUEST"))
                .andExpect(jsonPath("$.content[0].entityType").value("Request"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void should_return_401_when_audit_queried_without_session() throws Exception {
        mockMvc.perform(get(AUDIT_URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void should_return_403_when_employee_queries_audit_log() throws Exception {
        mockMvc.perform(get(AUDIT_URL).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void should_return_400_when_audit_window_from_after_to() throws Exception {
        // Arrange: el servicio rechaza la ventana invalida
        willThrow(new InvalidDateRangeException("La fecha 'from' no puede ser posterior a 'to'"))
                .given(auditQueryService).list(any(), any(), any(), any(), any());

        // Act / Assert
        mockMvc.perform(get(AUDIT_URL)
                        .param("from", "2026-06-01T00:00:00Z")
                        .param("to", "2026-01-01T00:00:00Z")
                        .with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.from").exists());
    }

    // ---- 1.4 / 1.5 / 1.6: /login-logs ----

    @Test
    void should_return_only_invalid_credentials_when_login_log_filtered_by_result() throws Exception {
        // Arrange
        given(loginLogQueryService.list(eq(LoginResult.INVALID_CREDENTIALS), any(), any(), any()))
                .willReturn(new PageResponse<>(List.of(loginEntry()), 1, 1, 20, 0, true, true));

        // Act / Assert
        mockMvc.perform(get(LOGIN_LOGS_URL)
                        .param("result", "INVALID_CREDENTIALS")
                        .with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].result").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.content[0].loginAttempted").value("jperez"));
    }

    @Test
    void should_return_400_when_login_log_result_outside_enum() throws Exception {
        // Act / Assert: 'UNKNOWN' no pertenece al enum LoginResult -> binding falla -> 400
        mockMvc.perform(get(LOGIN_LOGS_URL)
                        .param("result", "UNKNOWN")
                        .with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.result").exists());
    }

    @Test
    void should_return_403_when_employee_queries_login_log() throws Exception {
        mockMvc.perform(get(LOGIN_LOGS_URL).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void should_return_401_when_login_log_queried_without_session() throws Exception {
        mockMvc.perform(get(LOGIN_LOGS_URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void should_return_empty_filters_page_when_admin_queries_without_params() throws Exception {
        // Arrange: sin filtros, el servicio recibe nulls
        given(auditQueryService.list(isNull(), isNull(), isNull(), isNull(), any()))
                .willReturn(new PageResponse<>(List.of(), 0, 0, 20, 0, true, true));

        // Act / Assert
        mockMvc.perform(get(AUDIT_URL).with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    private static AuditLogEntryResponse auditEntry() {
        return new AuditLogEntryResponse(
                1L, 7L, "APPROVE_REQUEST", "Request", 15L, "{\"actorLogin\":\"admin\"}", WHEN);
    }

    private static LoginLogEntryResponse loginEntry() {
        return new LoginLogEntryResponse(
                1L, "jperez", null, LoginResult.INVALID_CREDENTIALS, LoginPhase.PHASE_1,
                "10.0.0.5", "JUnit", WHEN);
    }
}
