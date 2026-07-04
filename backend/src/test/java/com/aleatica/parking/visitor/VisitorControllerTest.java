package com.aleatica.parking.visitor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aleatica.parking.config.SecurityConfig;
import com.aleatica.parking.employee.dto.PageResponse;
import com.aleatica.parking.visitor.application.DuplicateNationalIdException;
import com.aleatica.parking.visitor.application.VisitorService;
import com.aleatica.parking.visitor.dto.VisitorResponse;
import jakarta.persistence.EntityNotFoundException;
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
 * Tests del adaptador web de fichas de visitante (@WebMvcTest) sobre los 4 endpoints:
 * RBAC (401 sin sesion, 403 a {@code EMPLOYEE}), codigos HTTP de cada operacion, y la
 * forma de error {@code { error, message, fields, timestamp }} para 400/404/409. La
 * logica se mockea; aqui solo se verifica el contrato HTTP.
 */
@WebMvcTest(VisitorController.class)
@Import(SecurityConfig.class)
class VisitorControllerTest {

    private static final String BASE_URL = "/api/v1/visitors";
    private static final String ID_URL = BASE_URL + "/42";

    private static final String ADMIN = "admin";
    private static final String EMP = "empleado";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_EMPLOYEE = "EMPLOYEE";

    private static final String CREATE_BODY =
            "{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\",\"nationalId\":\"X1234567Z\"}";
    private static final String ERROR_PATH = "$.error";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VisitorService visitorService;

    // ---- 401 sin sesion ----

    @Test
    void shouldReturn401_whenListingWithoutSession() throws Exception {
        mockMvc.perform(get(BASE_URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401_whenCreatingWithoutSession() throws Exception {
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isUnauthorized());
    }

    // ---- 403 a EMPLOYEE ----

    @Test
    void shouldReturn403_whenEmployeeListsVisitors() throws Exception {
        mockMvc.perform(get(BASE_URL).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath(ERROR_PATH).value("FORBIDDEN"));
        verify(visitorService, never()).list(any(), any());
    }

    @Test
    void shouldReturn403_whenEmployeeCreatesVisitor() throws Exception {
        mockMvc.perform(post(BASE_URL).with(user(EMP).roles(ROLE_EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isForbidden());
        verify(visitorService, never()).create(anyString(), any());
    }

    // ---- Caminos felices y codigos ----

    @Test
    void shouldReturn201_whenAdminCreatesVisitor() throws Exception {
        // Arrange
        given(visitorService.create(anyString(), any())).willReturn(sample());

        // Act / Assert
        mockMvc.perform(post(BASE_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nationalId").value("X1234567Z"));
    }

    @Test
    void shouldReturn200_whenAdminListsVisitors() throws Exception {
        // Arrange
        given(visitorService.list(any(), any()))
                .willReturn(new PageResponse<>(List.of(sample()), 1, 1, 20, 0, true, true));

        // Act / Assert
        mockMvc.perform(get(BASE_URL).with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(42))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldReturn200_whenAdminGetsVisitor() throws Exception {
        // Arrange
        given(visitorService.get(eq(42L))).willReturn(sample());

        // Act / Assert
        mockMvc.perform(get(ID_URL).with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42));
    }

    @Test
    void shouldReturn200_whenAdminUpdatesVisitor() throws Exception {
        // Arrange
        given(visitorService.update(eq(42L), any())).willReturn(sample());

        // Act / Assert
        mockMvc.perform(put(ID_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42));
    }

    // ---- Validacion y conflicto ----

    @Test
    void shouldReturn400_whenCreatingWithoutRequiredFields() throws Exception {
        // Act / Assert: firstName/lastName/nationalId ausentes -> @NotBlank -> 400
        mockMvc.perform(post(BASE_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(ERROR_PATH).value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.firstName").exists())
                .andExpect(jsonPath("$.fields.lastName").exists())
                .andExpect(jsonPath("$.fields.nationalId").exists());
        verify(visitorService, never()).create(anyString(), any());
    }

    @Test
    void shouldReturn409_whenNationalIdDuplicate() throws Exception {
        // Arrange
        given(visitorService.create(anyString(), any()))
                .willThrow(new DuplicateNationalIdException("duplicado"));

        // Act / Assert
        mockMvc.perform(post(BASE_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(ERROR_PATH).value("CONFLICT"))
                .andExpect(jsonPath("$.fields.nationalId").exists());
    }

    @Test
    void shouldReturn404_whenUpdatingUnknownVisitor() throws Exception {
        // Arrange
        given(visitorService.update(eq(42L), any())).willThrow(new EntityNotFoundException("no"));

        // Act / Assert
        mockMvc.perform(put(ID_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(ERROR_PATH).value("NOT_FOUND"));
    }

    private VisitorResponse sample() {
        return new VisitorResponse(42L, "Ada", "Lovelace", "X1234567Z", "1234ABC", "Contoso",
                "Reunion", 1L, Instant.parse("2026-07-04T10:00:00Z"));
    }
}
