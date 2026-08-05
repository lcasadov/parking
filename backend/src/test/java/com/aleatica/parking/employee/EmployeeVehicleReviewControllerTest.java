package com.aleatica.parking.employee;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aleatica.parking.config.SecurityConfig;
import com.aleatica.parking.employee.application.EmployeeVehicleReviewService;
import com.aleatica.parking.employee.application.VehicleReviewConflictException;
import com.aleatica.parking.employee.dto.EmployeeVehicleReviewResponse;
import com.aleatica.parking.employee.dto.EmployeeVehicleReviewResponse.Owner;
import com.aleatica.parking.employee.dto.PageResponse;
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
 * Tests del adaptador web de la bandeja de validación (@WebMvcTest): RBAC (401 sin sesión, 403 para
 * EMPLOYEE), códigos HTTP de las acciones y mapeo de errores (400 motivo ausente, 409 ya procesado,
 * 404 inexistente). La lógica se mockea (change {@code employee-vehicle-self-service}, Fase 2).
 */
@WebMvcTest(EmployeeVehicleReviewController.class)
@Import(SecurityConfig.class)
class EmployeeVehicleReviewControllerTest {

    private static final String BASE_URL = "/api/v1/employee-vehicles";
    private static final String ADMIN = "admin";
    private static final String EMP = "empleado";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_EMPLOYEE = "EMPLOYEE";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeVehicleReviewService reviewService;

    @Test
    void shouldReturn401_whenListingWithoutSession() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403_whenEmployeeListsReview() throws Exception {
        mockMvc.perform(get(BASE_URL).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnPage_whenAdminLists() throws Exception {
        given(reviewService.list(eq(List.of(VehicleStatus.PENDING)), any()))
                .willReturn(new PageResponse<>(List.of(sample()), 1, 1, 20, 0, true, true));

        mockMvc.perform(get(BASE_URL).param("status", "PENDING").with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].licensePlate").value("1234ABC"))
                .andExpect(jsonPath("$.content[0].employee.fullName").value("Ana García"));
    }

    @Test
    void shouldReturnPendingCount() throws Exception {
        given(reviewService.pendingCount()).willReturn(4L);

        mockMvc.perform(get(BASE_URL + "/pending-count").with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(4));
    }

    @Test
    void shouldReturnCountsByStatus() throws Exception {
        given(reviewService.countsByStatus())
                .willReturn(java.util.Map.of(VehicleStatus.PENDING, 3L, VehicleStatus.APPROVED, 5L));

        mockMvc.perform(get(BASE_URL + "/counts").with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.PENDING").value(3))
                .andExpect(jsonPath("$.APPROVED").value(5));
    }

    @Test
    void shouldReturnHistory() throws Exception {
        given(reviewService.history(3L)).willReturn(List.of());

        mockMvc.perform(get(BASE_URL + "/3/history").with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldApprove() throws Exception {
        given(reviewService.approve(eq(3L), any())).willReturn(sample());

        mockMvc.perform(post(BASE_URL + "/3/approve").with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.licensePlate").value("1234ABC"));
    }

    @Test
    void shouldReturn409_whenApprovingAlreadyProcessed() throws Exception {
        willThrow(new VehicleReviewConflictException("ya procesado"))
                .given(reviewService).approve(eq(3L), any());

        mockMvc.perform(post(BASE_URL + "/3/approve").with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    void shouldReturn404_whenApprovingUnknown() throws Exception {
        willThrow(new EntityNotFoundException("Vehiculo no encontrado: 3"))
                .given(reviewService).approve(eq(3L), any());

        mockMvc.perform(post(BASE_URL + "/3/approve").with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void shouldReturn400_whenRejectingWithoutReason() throws Exception {
        mockMvc.perform(post(BASE_URL + "/3/reject").with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.reason").exists());
    }

    @Test
    void shouldRejectWithReason() throws Exception {
        given(reviewService.reject(eq(3L), any(), any())).willReturn(sample());

        mockMvc.perform(post(BASE_URL + "/3/reject").with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Matrícula ilegible\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn403_whenEmployeeApproves() throws Exception {
        mockMvc.perform(post(BASE_URL + "/3/approve").with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldChangeStatus() throws Exception {
        given(reviewService.changeStatus(eq(3L), eq(VehicleStatus.PENDING), any(), any()))
                .willReturn(sample());

        mockMvc.perform(post(BASE_URL + "/3/status").with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PENDING\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn400_whenChangingStatusWithoutTarget() throws Exception {
        mockMvc.perform(post(BASE_URL + "/3/status").with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.status").exists());
    }

    @Test
    void shouldReturn403_whenEmployeeChangesStatus() throws Exception {
        mockMvc.perform(post(BASE_URL + "/3/status").with(user(EMP).roles(ROLE_EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PENDING\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldConfirmDeletion() throws Exception {
        mockMvc.perform(post(BASE_URL + "/3/confirm-deletion").with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isNoContent());
        verify(reviewService).confirmDeletion(eq(3L), any());
    }

    @Test
    void shouldRestore() throws Exception {
        given(reviewService.restore(eq(3L), any())).willReturn(sample());

        mockMvc.perform(post(BASE_URL + "/3/restore").with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isOk());
    }

    private EmployeeVehicleReviewResponse sample() {
        return new EmployeeVehicleReviewResponse(
                3L, new Owner(15L, "Ana García", "IT"), "1234ABC", "Seat", "Leon", "Gris",
                VehicleStatus.PENDING, null, Instant.parse("2026-07-31T10:00:00Z"));
    }
}
