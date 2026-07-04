package com.aleatica.parking.availability;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aleatica.parking.availability.application.AvailabilityService;
import com.aleatica.parking.availability.dto.AvailabilityItemResponse;
import com.aleatica.parking.availability.dto.AvailabilityResponse;
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
 * Tests del adaptador web de disponibilidad (@WebMvcTest): 401 sin sesion, camino feliz
 * autenticado, y validacion del parametro {@code date} (ausente o con formato invalido)
 * traducida a 400 con la forma de error {@code { error, message, fields, timestamp }}. La
 * logica se mockea; aqui solo se verifica el contrato HTTP.
 */
@WebMvcTest(AvailabilityController.class)
@Import(SecurityConfig.class)
class AvailabilityControllerTest {

    private static final String BASE_URL = "/api/v1/availability";
    private static final String DATE = "2026-07-10";
    private static final String EMP = "empleado";
    private static final String ROLE_EMPLOYEE = "EMPLOYEE";
    private static final String ERROR_PATH = "$.error";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AvailabilityService availabilityService;

    @Test
    void shouldReturn401_whenNoSession() throws Exception {
        mockMvc.perform(get(BASE_URL).param("date", DATE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnAvailability_whenAuthenticatedWithValidDate() throws Exception {
        // Arrange
        given(availabilityService.availabilityForDate(any()))
                .willReturn(new AvailabilityResponse(LocalDate.parse(DATE),
                        List.of(new AvailabilityItemResponse(8L, "P-08"))));

        // Act / Assert
        mockMvc.perform(get(BASE_URL).param("date", DATE).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value(DATE))
                .andExpect(jsonPath("$.availableResources[0].parkingSpaceId").value(8))
                .andExpect(jsonPath("$.availableResources[0].label").value("P-08"));
    }

    @Test
    void shouldReturnBadRequest_whenDateMissing() throws Exception {
        mockMvc.perform(get(BASE_URL).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(ERROR_PATH).value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.date").exists());
    }

    @Test
    void shouldReturnBadRequest_whenDateMalformed() throws Exception {
        mockMvc.perform(get(BASE_URL).param("date", "10-07-2026").with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(ERROR_PATH).value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.date").exists());
    }
}
