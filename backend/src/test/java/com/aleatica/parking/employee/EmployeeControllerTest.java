package com.aleatica.parking.employee;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aleatica.parking.config.SecurityConfig;
import com.aleatica.parking.employee.application.EmployeeConflictException;
import com.aleatica.parking.employee.application.EmployeeService;
import com.aleatica.parking.employee.dto.EmployeeResetPasswordResponse;
import com.aleatica.parking.employee.dto.EmployeeResponse;
import com.aleatica.parking.employee.dto.PageResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests del adaptador web de empleados (@WebMvcTest): RBAC (401 sin sesion, 403
 * para EMPLOYEE), codigos HTTP de CRUD/reset y mapeo de conflicto a 409. La
 * logica se mockea; aqui solo se verifica el contrato HTTP y la autorizacion.
 */
@WebMvcTest(EmployeeController.class)
@Import(SecurityConfig.class)
class EmployeeControllerTest {

    private static final String BASE_URL = "/api/v1/employees";
    private static final String RESET_URL = BASE_URL + "/5/reset-password";
    private static final String ADMIN = "admin";
    private static final String EMP = "empleado";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_EMPLOYEE = "EMPLOYEE";
    private static final String LOGIN = "jperez";
    private static final String EMAIL = "jperez@aleatica.com";

    private static final String CREATE_BODY = """
            {"firstName":"Juan","lastName":"Perez","login":"jperez",
             "email":"jperez@aleatica.com","role":"EMPLOYEE","category":"DIRECTOR_N1"}""";
    private static final String UPDATE_BODY = """
            {"firstName":"Juan","lastName":"Perez",
             "email":"jperez@aleatica.com","role":"ADMIN","category":"GERENTE"}""";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeService employeeService;

    @Test
    void shouldReturn401_whenListingWithoutSession() throws Exception {
        // Act / Assert
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403_whenEmployeeRoleListsEmployees() throws Exception {
        // Act / Assert
        mockMvc.perform(get(BASE_URL).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void shouldReturn403_whenEmployeeRoleResetsPassword() throws Exception {
        // Act / Assert
        mockMvc.perform(post(RESET_URL).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnPage_whenAdminListsEmployees() throws Exception {
        // Arrange
        given(employeeService.list(any(), any(), any()))
                .willReturn(new PageResponse<>(List.of(sample()), 1, 1, 20, 0, true, true));

        // Act / Assert
        mockMvc.perform(get(BASE_URL).with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].login").value(LOGIN))
                .andExpect(jsonPath("$.content[0].category").value("DIRECTOR_N1"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldReturn201_whenAdminCreatesEmployee() throws Exception {
        // Arrange
        given(employeeService.create(any())).willReturn(sample());

        // Act / Assert
        mockMvc.perform(post(BASE_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.login").value(LOGIN))
                .andExpect(jsonPath("$.category").value("DIRECTOR_N1"));
    }

    @Test
    void shouldReturn400_whenCreatingEmployeeWithoutCategory() throws Exception {
        // Arrange: cuerpo valido salvo por la ausencia de 'category' (@NotNull)
        String noCategory = "{\"firstName\":\"Juan\",\"lastName\":\"Perez\","
                + "\"login\":\"jperez\",\"email\":\"jperez@aleatica.com\",\"role\":\"EMPLOYEE\"}";

        // Act / Assert
        mockMvc.perform(post(BASE_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(noCategory))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.category").exists());
    }

    @Test
    void shouldReturn400_whenCreatingEmployeeWithInvalidCategory() throws Exception {
        // Arrange: 'category' fuera del dominio del enum
        String badCategory = "{\"firstName\":\"Juan\",\"lastName\":\"Perez\","
                + "\"login\":\"jperez\",\"email\":\"jperez@aleatica.com\","
                + "\"role\":\"EMPLOYEE\",\"category\":\"JEFAZO\"}";

        // Act / Assert
        mockMvc.perform(post(BASE_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(badCategory))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400_whenCreatingEmployeeWithInvalidFields() throws Exception {
        // Act / Assert: firstName en blanco y email mal formado
        String invalid = "{\"firstName\":\"\",\"lastName\":\"Perez\","
                + "\"login\":\"jperez\",\"email\":\"no-es-email\",\"role\":\"EMPLOYEE\"}";
        mockMvc.perform(post(BASE_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldReturn409_whenCreatingEmployeeWithExistingLogin() throws Exception {
        // Arrange
        willThrow(new EmployeeConflictException("login", "El login ya esta en uso"))
                .given(employeeService).create(any());

        // Act / Assert
        mockMvc.perform(post(BASE_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.fields.login").exists());
    }

    @Test
    void shouldReturn200_whenAdminUpdatesEmployee() throws Exception {
        // Arrange
        given(employeeService.update(eq(5L), any())).willReturn(sample());

        // Act / Assert
        mockMvc.perform(put(BASE_URL + "/5").with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value(LOGIN));
    }

    @Test
    void shouldReturn204_whenAdminDeactivatesEmployee() throws Exception {
        // Act / Assert
        mockMvc.perform(delete(BASE_URL + "/5").with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isNoContent());
        verify(employeeService).deactivate(5L);
    }

    @Test
    void shouldReturn204_whenAdminReactivatesEmployee() throws Exception {
        // Act / Assert
        mockMvc.perform(post(BASE_URL + "/5/reactivate").with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isNoContent());
        verify(employeeService).reactivate(5L);
    }

    @Test
    void shouldReturnTemporaryPassword_whenAdminResetsPasswordInPhase1() throws Exception {
        // Arrange
        given(employeeService.resetPassword(5L))
                .willReturn(EmployeeResetPasswordResponse.phase1("TempPass#12Word"));

        // Act / Assert
        mockMvc.perform(post(RESET_URL).with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temporaryPassword").value("TempPass#12Word"))
                .andExpect(jsonPath("$.mustChange").value(true));
    }

    private EmployeeResponse sample() {
        return new EmployeeResponse(
                5L, "Juan", "Perez", LOGIN, EMAIL, "IT", "600100200", "1234ABC",
                true, AuthOrigin.LOCAL, Role.EMPLOYEE, EmployeeCategory.DIRECTOR_N1,
                true, true, false, true, true,
                Instant.parse("2026-01-01T00:00:00Z"), null);
    }
}
