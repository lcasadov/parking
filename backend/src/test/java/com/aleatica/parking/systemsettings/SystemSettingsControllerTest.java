package com.aleatica.parking.systemsettings;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aleatica.parking.config.SecurityConfig;
import com.aleatica.parking.systemsettings.application.SystemSettingsService;
import com.aleatica.parking.systemsettings.domain.ApprovalMode;
import com.aleatica.parking.systemsettings.dto.SystemSettingsResponse;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests del adaptador web del ajuste global (@WebMvcTest): RBAC (401 sin sesion, 403 para
 * {@code EMPLOYEE}, 200 para {@code ADMIN}) y validacion del cuerpo (400 con modo invalido). La
 * logica se mockea; aqui solo se verifica el contrato HTTP.
 */
@WebMvcTest(SystemSettingsController.class)
@Import(SecurityConfig.class)
class SystemSettingsControllerTest {

    private static final String URL = "/api/v1/admin/settings";
    private static final String ADMIN = "admin";
    private static final String EMP = "empleado";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_EMPLOYEE = "EMPLOYEE";
    private static final String VALID_BODY = "{\"approvalMode\":\"AUTOMATIC\"}";
    private static final String INVALID_BODY = "{\"approvalMode\":\"SOMETIMES\"}";
    private static final String ERROR_PATH = "$.error";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SystemSettingsService systemSettingsService;

    // ---- 401 sin sesion ----

    @Test
    void shouldReturn401_whenGettingWithoutSession() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401_whenUpdatingWithoutSession() throws Exception {
        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    // ---- 403 por rol ----

    @Test
    void shouldReturn403_whenEmployeeGetsSettings() throws Exception {
        mockMvc.perform(get(URL).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath(ERROR_PATH).value("FORBIDDEN"));
    }

    @Test
    void shouldReturn403_whenEmployeeUpdatesSettings() throws Exception {
        mockMvc.perform(put(URL).with(user(EMP).roles(ROLE_EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden());
    }

    // ---- 200 admin ----

    @Test
    void shouldReturnSettings_whenAdminGets() throws Exception {
        given(systemSettingsService.current())
                .willReturn(new SystemSettingsResponse(ApprovalMode.MANUAL, null, null));

        mockMvc.perform(get(URL).with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalMode").value("MANUAL"));
    }

    @Test
    void shouldUpdateSettings_whenAdminUpdatesWithValidMode() throws Exception {
        given(systemSettingsService.updateApprovalMode(any(ApprovalMode.class), anyString()))
                .willReturn(new SystemSettingsResponse(ApprovalMode.AUTOMATIC, 1L, Instant.now()));

        mockMvc.perform(put(URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalMode").value("AUTOMATIC"));
    }

    // ---- 400 valor invalido ----

    @Test
    void shouldReturn400_whenAdminUpdatesWithInvalidMode() throws Exception {
        mockMvc.perform(put(URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(INVALID_BODY))
                .andExpect(status().isBadRequest());
    }
}
