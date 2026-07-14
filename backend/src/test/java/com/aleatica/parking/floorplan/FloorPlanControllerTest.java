package com.aleatica.parking.floorplan;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aleatica.parking.concurrency.ConcurrencyRetry;
import com.aleatica.parking.config.SecurityConfig;
import com.aleatica.parking.desk.DeskCategory;
import com.aleatica.parking.floorplan.application.FloorPlanCommandService;
import com.aleatica.parking.floorplan.application.FloorPlanQueryService;
import com.aleatica.parking.floorplan.dto.DeskRequestResponse;
import com.aleatica.parking.floorplan.dto.FloorPlanDeskResponse;
import com.aleatica.parking.floorplan.dto.FloorPlanResponse;
import com.aleatica.parking.request.application.DuplicatePendingRequestException;
import com.aleatica.parking.request.application.OutsideRequestWindowException;
import com.aleatica.parking.request.application.SpaceUnavailableException;
import com.aleatica.parking.request.domain.RequestStatus;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
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
 * Tests del adaptador web del plano ({@code @WebMvcTest}): RBAC (401 sin sesion, 403 por rol
 * incorrecto), codigos HTTP de los tres endpoints, y la forma de error
 * {@code { error, message, fields, timestamp }} para 400/404/409. La logica se mockea; aqui
 * solo se verifica el contrato HTTP y la autorizacion.
 */
@WebMvcTest(FloorPlanController.class)
@Import({SecurityConfig.class, ConcurrencyRetry.class})
class FloorPlanControllerTest {

    private static final String BASE_URL = "/api/v1/floor-plan";
    private static final String REQUEST_URL = BASE_URL + "/desks/42/request";
    private static final String POSITION_URL = BASE_URL + "/desks/42/position";

    private static final String ADMIN = "admin";
    private static final String EMP = "empleado";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_EMPLOYEE = "EMPLOYEE";
    private static final String ERROR_PATH = "$.error";

    private static final String DATE = "2026-07-10";
    private static final String REQUEST_BODY = "{\"date\":\"" + DATE + "\"}";
    private static final String POSITION_BODY = "{\"coordX\":30.5,\"coordY\":47.0}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FloorPlanQueryService floorPlanQueryService;

    @MockBean
    private FloorPlanCommandService floorPlanCommandService;

    // ---- GET /floor-plan ----

    @Test
    void shouldReturn401_whenGettingFloorPlanWithoutSession() throws Exception {
        mockMvc.perform(get(BASE_URL).param("date", DATE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnPlan_whenEmployeeGetsFloorPlan() throws Exception {
        // Arrange: cualquier usuario autenticado puede ver el plano
        given(floorPlanQueryService.floorPlanForDate(anyString(), any()))
                .willReturn(new FloorPlanResponse(LocalDate.parse(DATE), List.of(sampleDesk())));

        // Act / Assert
        mockMvc.perform(get(BASE_URL).param("date", DATE).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desks[0].deskId").value(42))
                .andExpect(jsonPath("$.desks[0].state").value("FREE"));
    }

    @Test
    void shouldReturn400_whenGettingFloorPlanOutsideWindow() throws Exception {
        // Arrange
        given(floorPlanQueryService.floorPlanForDate(anyString(), any()))
                .willThrow(new OutsideRequestWindowException("fuera de ventana"));

        // Act / Assert
        mockMvc.perform(get(BASE_URL).param("date", DATE).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(ERROR_PATH).value("OUTSIDE_REQUEST_WINDOW"));
    }

    @Test
    void shouldReturn400_whenGettingFloorPlanWithoutDate() throws Exception {
        mockMvc.perform(get(BASE_URL).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(ERROR_PATH).value("VALIDATION_ERROR"));
    }

    // ---- POST /floor-plan/desks/{id}/request ----

    @Test
    void shouldReturn401_whenRequestingDeskWithoutSession() throws Exception {
        mockMvc.perform(post(REQUEST_URL)
                        .contentType(MediaType.APPLICATION_JSON).content(REQUEST_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403_whenAdminRequestsDeskFromFloorPlan() throws Exception {
        mockMvc.perform(post(REQUEST_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(REQUEST_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn201_whenEmployeeRequestsFreeDesk() throws Exception {
        // Arrange
        given(floorPlanCommandService.requestDesk(anyString(), eq(42L), any()))
                .willReturn(new DeskRequestResponse(
                        128L, 42L, FloorPlanDeskState.MINE, RequestStatus.PENDING));

        // Act / Assert
        mockMvc.perform(post(REQUEST_URL).with(user(EMP).roles(ROLE_EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON).content(REQUEST_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requestId").value(128))
                .andExpect(jsonPath("$.deskId").value(42))
                .andExpect(jsonPath("$.state").value("MINE"));
    }

    @Test
    void shouldReturn409_whenRequestingNonFreeDesk() throws Exception {
        // Arrange
        willThrow(new SpaceUnavailableException("El puesto no esta disponible para la fecha solicitada"))
                .given(floorPlanCommandService).requestDesk(anyString(), eq(42L), any());

        // Act / Assert
        mockMvc.perform(post(REQUEST_URL).with(user(EMP).roles(ROLE_EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON).content(REQUEST_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(ERROR_PATH).value("SPACE_NOT_AVAILABLE"));
    }

    @Test
    void shouldReturn409_whenRequestingDeskWithDuplicatePending() throws Exception {
        // Arrange
        willThrow(new DuplicatePendingRequestException("Ya existe una solicitud pendiente para esa fecha"))
                .given(floorPlanCommandService).requestDesk(anyString(), eq(42L), any());

        // Act / Assert
        mockMvc.perform(post(REQUEST_URL).with(user(EMP).roles(ROLE_EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON).content(REQUEST_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(ERROR_PATH).value("REQUEST_ALREADY_PENDING"));
    }

    @Test
    void shouldReturn404_whenRequestingUnknownDesk() throws Exception {
        // Arrange
        willThrow(new EntityNotFoundException("Puesto no encontrado: 42"))
                .given(floorPlanCommandService).requestDesk(anyString(), eq(42L), any());

        // Act / Assert
        mockMvc.perform(post(REQUEST_URL).with(user(EMP).roles(ROLE_EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON).content(REQUEST_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(ERROR_PATH).value("NOT_FOUND"));
    }

    @Test
    void shouldReturn400_whenRequestingDeskWithoutDate() throws Exception {
        mockMvc.perform(post(REQUEST_URL).with(user(EMP).roles(ROLE_EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(ERROR_PATH).value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.date").exists());
    }

    // ---- PUT /floor-plan/desks/{id}/position ----

    @Test
    void shouldReturn401_whenUpdatingPositionWithoutSession() throws Exception {
        mockMvc.perform(put(POSITION_URL)
                        .contentType(MediaType.APPLICATION_JSON).content(POSITION_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403_whenEmployeeUpdatesDeskPosition() throws Exception {
        mockMvc.perform(put(POSITION_URL).with(user(EMP).roles(ROLE_EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON).content(POSITION_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn204_whenAdminUpdatesDeskPosition() throws Exception {
        mockMvc.perform(put(POSITION_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(POSITION_BODY))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn400_whenCoordinatesOutOfRange() throws Exception {
        mockMvc.perform(put(POSITION_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"coordX\":150,\"coordY\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(ERROR_PATH).value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.coordX").exists())
                .andExpect(jsonPath("$.fields.coordY").exists());
    }

    @Test
    void shouldReturn400_whenCoordinatesMissing() throws Exception {
        mockMvc.perform(put(POSITION_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(ERROR_PATH).value("VALIDATION_ERROR"));
    }

    @Test
    void shouldReturn404_whenUpdatingPositionOfUnknownDesk() throws Exception {
        // Arrange
        willThrow(new EntityNotFoundException("Puesto no encontrado: 42"))
                .given(floorPlanCommandService).updatePosition(anyLong(), any(), any());

        // Act / Assert
        mockMvc.perform(put(POSITION_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(POSITION_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(ERROR_PATH).value("NOT_FOUND"));
    }

    private FloorPlanDeskResponse sampleDesk() {
        return new FloorPlanDeskResponse(42L, 12, DeskCategory.STANDARD,
                new BigDecimal("30.5"), new BigDecimal("47.0"), FloorPlanDeskState.FREE);
    }
}
