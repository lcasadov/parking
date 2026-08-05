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
import com.aleatica.parking.employee.application.EmployeeVehicleConflictException;
import com.aleatica.parking.employee.application.MyVehicleService;
import com.aleatica.parking.employee.dto.EmployeeVehicleResponse;
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
 * Tests del adaptador web self-service de vehículos (@WebMvcTest): exige autenticación (401 sin
 * sesión), resuelve el empleado del principal (no de la ruta) y devuelve el vehículo en estado
 * PENDING; mapea 400/404/409. La lógica se mockea (change {@code employee-vehicle-self-service}).
 */
@WebMvcTest(MyVehicleController.class)
@Import(SecurityConfig.class)
class MyVehicleControllerTest {

    private static final String BASE_URL = "/api/v1/me/vehicles";
    private static final String VEHICLE_URL = BASE_URL + "/3";
    private static final String LOGIN = "jperez";
    private static final String ROLE_EMPLOYEE = "EMPLOYEE";
    private static final String PLATE = "1234ABC";

    private static final String CREATE_ONLY_PLATE = "{\"licensePlate\":\"1234ABC\"}";
    private static final String BLANK_PLATE = "{\"licensePlate\":\"  \"}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MyVehicleService vehicleService;

    @Test
    void shouldReturn401_whenListingWithoutSession() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldListOwnVehicles_forAuthenticatedEmployee() throws Exception {
        given(vehicleService.list(LOGIN)).willReturn(List.of(pending()));

        mockMvc.perform(get(BASE_URL).with(user(LOGIN).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].licensePlate").value(PLATE))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void shouldReturn201Pending_whenEmployeeCreatesOwnVehicle() throws Exception {
        given(vehicleService.create(eq(LOGIN), any())).willReturn(pending());

        mockMvc.perform(post(BASE_URL).with(user(LOGIN).roles(ROLE_EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_ONLY_PLATE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldReturn400_whenCreatingWithoutPlate() throws Exception {
        mockMvc.perform(post(BASE_URL).with(user(LOGIN).roles(ROLE_EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON).content(BLANK_PLATE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.licensePlate").exists());
    }

    @Test
    void shouldReturn409_whenCreatingDuplicatePlate() throws Exception {
        willThrow(new EmployeeVehicleConflictException("licensePlate", "duplicada"))
                .given(vehicleService).create(eq(LOGIN), any());

        mockMvc.perform(post(BASE_URL).with(user(LOGIN).roles(ROLE_EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_ONLY_PLATE))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    void shouldReturn200_whenEmployeeUpdatesOwnVehicle() throws Exception {
        given(vehicleService.update(eq(LOGIN), eq(3L), any())).willReturn(pending());

        mockMvc.perform(put(VEHICLE_URL).with(user(LOGIN).roles(ROLE_EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_ONLY_PLATE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldReturn404_whenUpdatingForeignOrUnknownVehicle() throws Exception {
        willThrow(new EntityNotFoundException("Vehiculo no encontrado: 3"))
                .given(vehicleService).update(eq(LOGIN), eq(3L), any());

        mockMvc.perform(put(VEHICLE_URL).with(user(LOGIN).roles(ROLE_EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_ONLY_PLATE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void shouldReturn204_whenEmployeeDeletesOwnVehicle() throws Exception {
        mockMvc.perform(delete(VEHICLE_URL).with(user(LOGIN).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isNoContent());
        verify(vehicleService).delete(LOGIN, 3L);
    }

    private EmployeeVehicleResponse pending() {
        return new EmployeeVehicleResponse(
                3L, 7L, PLATE, "Seat", "Leon", "Gris",
                VehicleStatus.PENDING, null, Instant.parse("2026-01-01T00:00:00Z"));
    }
}
