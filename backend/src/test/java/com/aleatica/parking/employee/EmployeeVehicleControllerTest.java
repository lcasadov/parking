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
import com.aleatica.parking.employee.application.EmployeeVehicleService;
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
 * Tests del adaptador web de vehiculos de empleado (@WebMvcTest): RBAC (401 sin sesion, 403 para
 * EMPLOYEE), codigos HTTP del CRUD y mapeo de errores (400 matricula ausente, 409 duplicado, 404
 * empleado/vehiculo inexistente). La logica se mockea; aqui solo se verifica el contrato HTTP y la
 * autorizacion (change {@code employee-vehicles}).
 */
@WebMvcTest(EmployeeVehicleController.class)
@Import(SecurityConfig.class)
class EmployeeVehicleControllerTest {

    private static final String BASE_URL = "/api/v1/employees/15/vehicles";
    private static final String VEHICLE_URL = BASE_URL + "/3";
    private static final String ADMIN = "admin";
    private static final String EMP = "empleado";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_EMPLOYEE = "EMPLOYEE";
    private static final String PLATE = "1234ABC";

    private static final String CREATE_ONLY_PLATE = "{\"licensePlate\":\"1234ABC\"}";
    private static final String CREATE_FULL = """
            {"licensePlate":"1234abc","brand":"Seat","model":"Leon","color":"Gris"}""";
    private static final String BLANK_PLATE = "{\"licensePlate\":\"  \",\"brand\":\"Seat\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeVehicleService vehicleService;

    @Test
    void shouldReturn401_whenListingWithoutSession() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403_whenEmployeeRoleListsVehicles() throws Exception {
        mockMvc.perform(get(BASE_URL).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void shouldReturn403_whenEmployeeRoleCreatesVehicle() throws Exception {
        mockMvc.perform(post(BASE_URL).with(user(EMP).roles(ROLE_EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_ONLY_PLATE))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnList_whenAdminListsVehicles() throws Exception {
        given(vehicleService.list(15L)).willReturn(List.of(sample()));

        mockMvc.perform(get(BASE_URL).with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].licensePlate").value(PLATE))
                .andExpect(jsonPath("$[0].brand").value("Seat"));
    }

    @Test
    void shouldReturn201_whenAdminCreatesVehicleWithOnlyPlate() throws Exception {
        given(vehicleService.create(eq(15L), any())).willReturn(sample());

        mockMvc.perform(post(BASE_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_ONLY_PLATE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.licensePlate").value(PLATE));
    }

    @Test
    void shouldReturn201_whenAdminCreatesVehicleWithAllFields() throws Exception {
        given(vehicleService.create(eq(15L), any())).willReturn(sample());

        mockMvc.perform(post(BASE_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_FULL))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.model").value("Leon"));
    }

    @Test
    void shouldReturn400_whenCreatingVehicleWithoutPlate() throws Exception {
        mockMvc.perform(post(BASE_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(BLANK_PLATE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.licensePlate").exists());
    }

    @Test
    void shouldReturn404_whenCreatingVehicleForUnknownEmployee() throws Exception {
        willThrow(new EntityNotFoundException("Empleado no encontrado: 15"))
                .given(vehicleService).create(eq(15L), any());

        mockMvc.perform(post(BASE_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_ONLY_PLATE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void shouldReturn409_whenCreatingVehicleWithDuplicatePlate() throws Exception {
        willThrow(new EmployeeVehicleConflictException("licensePlate", "duplicada"))
                .given(vehicleService).create(eq(15L), any());

        mockMvc.perform(post(BASE_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_ONLY_PLATE))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.fields.licensePlate").exists());
    }

    @Test
    void shouldReturn200_whenAdminUpdatesVehicle() throws Exception {
        given(vehicleService.update(eq(15L), eq(3L), any())).willReturn(sample());

        mockMvc.perform(put(VEHICLE_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_FULL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.licensePlate").value(PLATE));
    }

    @Test
    void shouldReturn404_whenUpdatingForeignOrUnknownVehicle() throws Exception {
        willThrow(new EntityNotFoundException("Vehiculo no encontrado: 3"))
                .given(vehicleService).update(eq(15L), eq(3L), any());

        mockMvc.perform(put(VEHICLE_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_FULL))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void shouldReturn204_whenAdminDeletesVehicle() throws Exception {
        mockMvc.perform(delete(VEHICLE_URL).with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isNoContent());
        verify(vehicleService).delete(15L, 3L);
    }

    @Test
    void shouldReturn404_whenDeletingForeignOrUnknownVehicle() throws Exception {
        willThrow(new EntityNotFoundException("Vehiculo no encontrado: 3"))
                .given(vehicleService).delete(15L, 3L);

        mockMvc.perform(delete(VEHICLE_URL).with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    private EmployeeVehicleResponse sample() {
        return new EmployeeVehicleResponse(
                3L, 15L, PLATE, "Seat", "Leon", "Gris",
                com.aleatica.parking.employee.VehicleStatus.APPROVED, null,
                Instant.parse("2026-01-01T00:00:00Z"));
    }
}
