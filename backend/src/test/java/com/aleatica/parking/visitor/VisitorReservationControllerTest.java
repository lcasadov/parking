package com.aleatica.parking.visitor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aleatica.parking.config.SecurityConfig;
import com.aleatica.parking.employee.dto.PageResponse;
import com.aleatica.parking.visitor.application.PastVisitorReservationCancellationException;
import com.aleatica.parking.visitor.application.SpaceNotAvailableForReservationException;
import com.aleatica.parking.visitor.application.VisitorReservationService;
import com.aleatica.parking.visitor.dto.VisitorReservationResponse;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests del adaptador web de reservas de visitante (@WebMvcTest) sobre los 3 endpoints:
 * RBAC (401 sin sesion, 403 a {@code EMPLOYEE}), codigos HTTP de cada operacion, y la
 * forma de error {@code { error, message, fields, timestamp }} para 400/404/409. La
 * logica se mockea; aqui solo se verifica el contrato HTTP.
 */
@WebMvcTest(VisitorReservationController.class)
@Import(SecurityConfig.class)
class VisitorReservationControllerTest {

    private static final String BASE_URL = "/api/v1/visitor-reservations";
    private static final String ID_URL = BASE_URL + "/7";

    private static final String ADMIN = "admin";
    private static final String EMP = "empleado";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_EMPLOYEE = "EMPLOYEE";

    private static final String CREATE_BODY =
            "{\"visitorId\":42,\"parkingSpaceId\":8,\"reservationDate\":\"2026-07-10\"}";
    private static final String ERROR_PATH = "$.error";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VisitorReservationService reservationService;

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

    @Test
    void shouldReturn401_whenCancellingWithoutSession() throws Exception {
        mockMvc.perform(delete(ID_URL)).andExpect(status().isUnauthorized());
    }

    // ---- 403 a EMPLOYEE ----

    @Test
    void shouldReturn403_whenEmployeeCreatesReservation() throws Exception {
        mockMvc.perform(post(BASE_URL).with(user(EMP).roles(ROLE_EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath(ERROR_PATH).value("FORBIDDEN"));
        verify(reservationService, never()).create(anyString(), any());
    }

    @Test
    void shouldReturn403_whenEmployeeCancelsReservation() throws Exception {
        mockMvc.perform(delete(ID_URL).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isForbidden());
        verify(reservationService, never()).cancel(any());
    }

    // ---- Caminos felices y codigos ----

    @Test
    void shouldReturn201_whenAdminCreatesReservation() throws Exception {
        // Arrange
        given(reservationService.create(anyString(), any())).willReturn(sample());

        // Act / Assert
        mockMvc.perform(post(BASE_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.parkingSpaceId").value(8));
    }

    @Test
    void shouldReturn200_whenAdminListsReservations() throws Exception {
        // Arrange
        given(reservationService.list(any(), any(), any()))
                .willReturn(new PageResponse<>(List.of(sample()), 1, 1, 20, 0, true, true));

        // Act / Assert
        mockMvc.perform(get(BASE_URL).with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(7))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldReturn204_whenAdminCancelsFutureReservation() throws Exception {
        // Arrange
        willDoNothing().given(reservationService).cancel(eq(7L));

        // Act / Assert
        mockMvc.perform(delete(ID_URL).with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isNoContent());
    }

    // ---- Validacion, disponibilidad y anulacion ----

    @Test
    void shouldReturn400_whenCreatingWithoutRequiredFields() throws Exception {
        // Act / Assert: visitorId/parkingSpaceId/reservationDate ausentes -> @NotNull -> 400
        mockMvc.perform(post(BASE_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(ERROR_PATH).value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.visitorId").exists())
                .andExpect(jsonPath("$.fields.parkingSpaceId").exists())
                .andExpect(jsonPath("$.fields.reservationDate").exists());
        verify(reservationService, never()).create(anyString(), any());
    }

    @Test
    void shouldReturn409_whenSpaceAlreadyOccupied() throws Exception {
        // Arrange
        given(reservationService.create(anyString(), any()))
                .willThrow(new SpaceNotAvailableForReservationException("ocupada"));

        // Act / Assert
        mockMvc.perform(post(BASE_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(ERROR_PATH).value("SPACE_NOT_AVAILABLE"))
                .andExpect(jsonPath("$.fields.parkingSpaceId").exists());
    }

    @Test
    void shouldReturn404_whenVisitorOrSpaceUnknown() throws Exception {
        // Arrange
        given(reservationService.create(anyString(), any()))
                .willThrow(new EntityNotFoundException("no"));

        // Act / Assert
        mockMvc.perform(post(BASE_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(ERROR_PATH).value("NOT_FOUND"));
    }

    @Test
    void shouldReturn400_whenCancellingPastReservation() throws Exception {
        // Arrange
        willThrow(new PastVisitorReservationCancellationException("pasada"))
                .given(reservationService).cancel(eq(7L));

        // Act / Assert
        mockMvc.perform(delete(ID_URL).with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(ERROR_PATH).value("VISITOR_RESERVATION_NOT_CANCELLABLE"));
    }

    @Test
    void shouldReturn404_whenCancellingUnknownReservation() throws Exception {
        // Arrange
        willThrow(new EntityNotFoundException("no")).given(reservationService).cancel(eq(7L));

        // Act / Assert
        mockMvc.perform(delete(ID_URL).with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(ERROR_PATH).value("NOT_FOUND"));
    }

    private VisitorReservationResponse sample() {
        return new VisitorReservationResponse(7L, 42L, 8L, LocalDate.of(2026, 7, 10),
                "Puerta norte", 1L, Instant.parse("2026-07-04T10:00:00Z"));
    }
}
