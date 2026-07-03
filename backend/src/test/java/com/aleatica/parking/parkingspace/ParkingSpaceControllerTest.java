package com.aleatica.parking.parkingspace;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aleatica.parking.config.SecurityConfig;
import com.aleatica.parking.employee.dto.PageResponse;
import com.aleatica.parking.parkingspace.application.ParkingSpaceConflictException;
import com.aleatica.parking.parkingspace.application.ParkingSpaceService;
import com.aleatica.parking.parkingspace.dto.ParkingSpaceResponse;
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
 * Tests del adaptador web de plazas (@WebMvcTest): RBAC (401 sin sesion, 403 para
 * EMPLOYEE en los cuatro endpoints), codigos HTTP del CRUD/configuracion y mapeo
 * de conflicto a 409. La logica se mockea; aqui solo se verifica el contrato HTTP
 * y la autorizacion.
 */
@WebMvcTest(ParkingSpaceController.class)
@Import(SecurityConfig.class)
class ParkingSpaceControllerTest {

    private static final String BASE_URL = "/api/v1/parking-spaces";
    private static final String CONFIGURE_URL = BASE_URL + "/configure";
    private static final String ADMIN = "admin";
    private static final String EMP = "empleado";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_EMPLOYEE = "EMPLOYEE";
    private static final String LABEL = "P-08";

    private static final String CREATE_BODY = "{\"label\":\"P-08\"}";
    private static final String UPDATE_BODY = "{\"label\":\"P-09\",\"active\":true}";
    private static final String CONFIGURE_BODY = "{\"total\":50}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ParkingSpaceService parkingSpaceService;

    @Test
    void shouldReturn401_whenListingWithoutSession() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403_whenEmployeeRoleListsSpaces() throws Exception {
        mockMvc.perform(get(BASE_URL).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void shouldReturn403_whenEmployeeRoleCreatesSpace() throws Exception {
        mockMvc.perform(post(BASE_URL).with(user(EMP).roles(ROLE_EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403_whenEmployeeRoleUpdatesSpace() throws Exception {
        mockMvc.perform(put(BASE_URL + "/5").with(user(EMP).roles(ROLE_EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403_whenEmployeeRoleConfiguresSpaces() throws Exception {
        mockMvc.perform(post(CONFIGURE_URL).with(user(EMP).roles(ROLE_EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON).content(CONFIGURE_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnPage_whenAdminListsSpaces() throws Exception {
        // Arrange
        given(parkingSpaceService.list(any(), any()))
                .willReturn(new PageResponse<>(List.of(sample()), 1, 1, 20, 0, true, true));

        // Act / Assert
        mockMvc.perform(get(BASE_URL).with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].label").value(LABEL))
                .andExpect(jsonPath("$.content[0].active").value(true))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldReturn201_whenAdminCreatesSpace() throws Exception {
        // Arrange
        given(parkingSpaceService.create(any())).willReturn(sample());

        // Act / Assert
        mockMvc.perform(post(BASE_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.label").value(LABEL))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldReturn400_whenCreatingSpaceWithBlankLabel() throws Exception {
        // Act / Assert: label en blanco
        mockMvc.perform(post(BASE_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"label\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.label").exists());
    }

    @Test
    void shouldReturn409_whenCreatingSpaceWithExistingLabel() throws Exception {
        // Arrange
        willThrow(new ParkingSpaceConflictException("label", "La etiqueta ya esta en uso"))
                .given(parkingSpaceService).create(any());

        // Act / Assert
        mockMvc.perform(post(BASE_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.fields.label").exists());
    }

    @Test
    void shouldReturn200_whenAdminUpdatesSpace() throws Exception {
        // Arrange
        given(parkingSpaceService.update(eq(5L), any()))
                .willReturn(new ParkingSpaceResponse(5L, "P-09", true, Instant.parse("2026-01-01T00:00:00Z")));

        // Act / Assert
        mockMvc.perform(put(BASE_URL + "/5").with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("P-09"));
    }

    @Test
    void shouldReturn404_whenUpdatingNonExistentSpace() throws Exception {
        // Arrange
        willThrow(new EntityNotFoundException("Plaza no encontrada: 999"))
                .given(parkingSpaceService).update(eq(999L), any());

        // Act / Assert
        mockMvc.perform(put(BASE_URL + "/999").with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void shouldReturn409_whenUpdatingToLabelUsedByAnotherSpace() throws Exception {
        // Arrange
        willThrow(new ParkingSpaceConflictException("label", "La etiqueta ya esta en uso"))
                .given(parkingSpaceService).update(eq(5L), any());

        // Act / Assert
        mockMvc.perform(put(BASE_URL + "/5").with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fields.label").exists());
    }

    @Test
    void shouldReturn200_whenAdminConfiguresTotal() throws Exception {
        // Arrange
        given(parkingSpaceService.configure(50)).willReturn(List.of(sample()));

        // Act / Assert
        mockMvc.perform(post(CONFIGURE_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(CONFIGURE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].label").value(LABEL));
    }

    @Test
    void shouldReturn400_whenConfiguringWithNegativeTotal() throws Exception {
        // Act / Assert
        mockMvc.perform(post(CONFIGURE_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"total\":-3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.total").exists());
    }

    private ParkingSpaceResponse sample() {
        return new ParkingSpaceResponse(5L, LABEL, true, Instant.parse("2026-01-01T00:00:00Z"));
    }
}
