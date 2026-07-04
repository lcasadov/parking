package com.aleatica.parking.fixedassignment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aleatica.parking.concurrency.ConcurrencyRetry;
import com.aleatica.parking.config.SecurityConfig;
import com.aleatica.parking.fixedassignment.application.FixedAssignmentService;
import com.aleatica.parking.fixedassignment.application.InvalidDayOfWeekException;
import com.aleatica.parking.fixedassignment.dto.FixedAssignmentResponse;
import com.aleatica.parking.resource.ResourceType;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests del adaptador web de asignaciones fijas (@WebMvcTest): RBAC (401 sin sesion,
 * 403 por rol), codigos HTTP de cada endpoint, verificacion de pertenencia (BOLA)
 * traducida a 403, y la forma de error {@code { error, message, fields, timestamp }}
 * para 400/404/409. La logica se mockea; aqui solo se verifica el contrato HTTP.
 */
@WebMvcTest(FixedAssignmentController.class)
@Import({SecurityConfig.class, ConcurrencyRetry.class})
class FixedAssignmentControllerTest {

    private static final String BASE_URL = "/api/v1/fixed-assignments";
    private static final String EMP_URL = BASE_URL + "/employee/15";
    private static final String ADMIN = "admin";
    private static final String EMP = "empleado";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_EMPLOYEE = "EMPLOYEE";

    private static final String PUT_BODY = "{\"parkingSpaceId\":8,\"daysOfWeek\":[1,2,3]}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FixedAssignmentService fixedAssignmentService;

    @Test
    void shouldReturn401_whenListingWithoutSession() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401_whenQueryingEmployeeWithoutSession() throws Exception {
        mockMvc.perform(get(EMP_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403_whenEmployeeListsAllAssignments() throws Exception {
        mockMvc.perform(get(BASE_URL).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void shouldReturn403_whenEmployeeSetsAssignments() throws Exception {
        mockMvc.perform(put(EMP_URL).with(user(EMP).roles(ROLE_EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON).content(PUT_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403_whenEmployeeRevokesAssignments() throws Exception {
        mockMvc.perform(delete(EMP_URL).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnPage_whenAdminListsAssignments() throws Exception {
        // Arrange
        given(fixedAssignmentService.list(any()))
                .willReturn(new com.aleatica.parking.employee.dto.PageResponse<>(
                        List.of(sample(1)), 1, 1, 20, 0, true, true));

        // Act / Assert
        mockMvc.perform(get(BASE_URL).with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].dayOfWeek").value(1))
                .andExpect(jsonPath("$.content[0].active").value(true))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldReturnOwnAssignments_whenEmployeeQueriesSelf() throws Exception {
        // Arrange
        given(fixedAssignmentService.getEmployeeAssignments(eq(15L), anyString(), anyBoolean()))
                .willReturn(List.of(sample(1)));

        // Act / Assert
        mockMvc.perform(get(EMP_URL).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].employeeId").value(15))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void shouldReturn403_whenEmployeeQueriesOtherEmployee() throws Exception {
        // Arrange: el servicio aplica BOLA y lanza AccessDenied
        willThrow(new AccessDeniedException("otro empleado"))
                .given(fixedAssignmentService).getEmployeeAssignments(eq(15L), anyString(), anyBoolean());

        // Act / Assert
        mockMvc.perform(get(EMP_URL).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void shouldReturn200_whenAdminSetsAssignments() throws Exception {
        // Arrange
        given(fixedAssignmentService.setAssignments(eq(15L), any(), anyString()))
                .willReturn(List.of(sample(1), sample(2)));

        // Act / Assert
        mockMvc.perform(put(EMP_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(PUT_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dayOfWeek").value(1))
                .andExpect(jsonPath("$[1].dayOfWeek").value(2));
    }

    @Test
    void shouldReturn400_whenSettingWithEmptyDays() throws Exception {
        // Act / Assert: lista vacia -> @NotEmpty -> 400 sobre el campo daysOfWeek
        mockMvc.perform(put(EMP_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parkingSpaceId\":8,\"daysOfWeek\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.daysOfWeek").exists());
    }

    @Test
    void shouldReturn400_whenServiceRejectsDayOutOfRange() throws Exception {
        // Arrange
        willThrow(new InvalidDayOfWeekException("dia invalido"))
                .given(fixedAssignmentService).setAssignments(eq(15L), any(), anyString());

        // Act / Assert
        mockMvc.perform(put(EMP_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(PUT_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.daysOfWeek").exists());
    }

    @Test
    void shouldReturn404_whenSettingForUnknownEmployee() throws Exception {
        // Arrange
        willThrow(new EntityNotFoundException("Empleado no encontrado: 15"))
                .given(fixedAssignmentService).setAssignments(eq(15L), any(), anyString());

        // Act / Assert
        mockMvc.perform(put(EMP_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(PUT_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void shouldReturn409_whenUniqueIndexViolated() throws Exception {
        // Arrange: la red dura es el indice unico filtrado -> DataIntegrityViolation
        willThrow(new DataIntegrityViolationException("UX_fixed_assignments_space_day_active"))
                .given(fixedAssignmentService).setAssignments(eq(15L), any(), anyString());

        // Act / Assert
        mockMvc.perform(put(EMP_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(PUT_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    void shouldReturn204_whenAdminRevokes() throws Exception {
        // Arrange
        willDoNothing().given(fixedAssignmentService).revoke(eq(15L), anyString());

        // Act / Assert
        mockMvc.perform(delete(EMP_URL).with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404_whenRevokingEmployeeWithoutActiveAssignment() throws Exception {
        // Arrange
        willThrow(new EntityNotFoundException("sin asignacion activa"))
                .given(fixedAssignmentService).revoke(eq(15L), anyString());

        // Act / Assert
        mockMvc.perform(delete(EMP_URL).with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    private FixedAssignmentResponse sample(int day) {
        return new FixedAssignmentResponse(
                (long) day, 8L, 15L, day, true, 1L,
                Instant.parse("2026-01-01T00:00:00Z"), null, null, ResourceType.PARKING);
    }
}
