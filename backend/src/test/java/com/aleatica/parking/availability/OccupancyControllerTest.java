package com.aleatica.parking.availability;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aleatica.parking.availability.application.AvailabilityService;
import com.aleatica.parking.availability.dto.OccupancyResponse;
import com.aleatica.parking.config.SecurityConfig;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests del adaptador web de ocupacion por fecha (@WebMvcTest): RBAC (401 sin sesion, 403 para
 * EMPLOYEE, 200 para ADMIN y AGENCIA), y validacion de {@code date} (400 ausente). Change
 * {@code restructure-admin-workflows}, capability {@code releases}, design §D5: base del pivote
 * por-fecha, accesible a ADMIN y AGENCIA (solo lectura). La logica se mockea; aqui solo se
 * verifica el contrato HTTP.
 */
@WebMvcTest(OccupancyController.class)
@Import(SecurityConfig.class)
class OccupancyControllerTest {

    private static final String URL = "/api/v1/occupancy";
    private static final String DATE = "2026-07-10";

    private static final String ADMIN = "admin";
    private static final String AGENCY = "agencia";
    private static final String EMP = "empleado";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_AGENCIA = "AGENCIA";
    private static final String ROLE_EMPLOYEE = "EMPLOYEE";
    private static final String ERROR_PATH = "$.error";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AvailabilityService availabilityService;

    @Test
    void shouldReturn401_whenWithoutSession() throws Exception {
        mockMvc.perform(get(URL).param("date", DATE)).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403_whenEmployeeRequestsOccupancy() throws Exception {
        mockMvc.perform(get(URL).param("date", DATE).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath(ERROR_PATH).value("FORBIDDEN"));
    }

    @Test
    void shouldReturn200_whenAdminRequestsOccupancy() throws Exception {
        // Arrange
        LocalDate date = LocalDate.parse(DATE);
        given(availabilityService.occupancyForDate(any())).willReturn(new OccupancyResponse(date, List.of()));

        // Act / Assert
        mockMvc.perform(get(URL).param("date", DATE).with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value(DATE));
    }

    @Test
    void shouldReturn200_whenAgencyRequestsOccupancy() throws Exception {
        // Arrange (change restructure-admin-workflows, design D5): AGENCIA gana la ocupacion read-only
        LocalDate date = LocalDate.parse(DATE);
        given(availabilityService.occupancyForDate(any())).willReturn(new OccupancyResponse(date, List.of()));

        // Act / Assert
        mockMvc.perform(get(URL).param("date", DATE).with(user(AGENCY).roles(ROLE_AGENCIA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value(DATE));
    }

    @Test
    void shouldReturnBadRequest_whenDateMissing() throws Exception {
        mockMvc.perform(get(URL).with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(ERROR_PATH).value("VALIDATION_ERROR"));
    }
}
