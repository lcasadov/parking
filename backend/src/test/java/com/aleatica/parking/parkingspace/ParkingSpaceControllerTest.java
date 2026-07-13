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
    private static final int NUMBER = 1007;
    private static final String LABEL = "1007";

    private static final String CREATE_BODY = "{\"number\":1007}";
    private static final String UPDATE_BODY = "{\"number\":2003,\"active\":true}";
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
        given(parkingSpaceService.list(any(), any(), any()))
                .willReturn(new PageResponse<>(List.of(sample()), 1, 1, 20, 0, true, true));

        // Act / Assert
        mockMvc.perform(get(BASE_URL).with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].number").value(NUMBER))
                .andExpect(jsonPath("$.content[0].floor").value(1))
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
                .andExpect(jsonPath("$.number").value(NUMBER))
                .andExpect(jsonPath("$.floor").value(1))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldReturn400_whenCreatingSpaceWithNumberBelowThousand() throws Exception {
        // Act / Assert: number < 1000 viola @Min
        mockMvc.perform(post(BASE_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"number\":999}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.number").exists());
    }

    @Test
    void shouldReturn400_whenCreatingSpaceWithoutNumber() throws Exception {
        // Act / Assert: number ausente viola @NotNull
        mockMvc.perform(post(BASE_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.number").exists());
    }

    @Test
    void shouldReturn409_whenCreatingSpaceWithExistingNumber() throws Exception {
        // Arrange
        willThrow(new ParkingSpaceConflictException("number", "El numero ya esta en uso"))
                .given(parkingSpaceService).create(any());

        // Act / Assert
        mockMvc.perform(post(BASE_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.fields.number").exists());
    }

    @Test
    void shouldReturn200_whenAdminUpdatesSpace() throws Exception {
        // Arrange
        given(parkingSpaceService.update(eq(5L), any()))
                .willReturn(new ParkingSpaceResponse(
                        5L, 2003, "2003", 2, true, Instant.parse("2026-01-01T00:00:00Z")));

        // Act / Assert
        mockMvc.perform(put(BASE_URL + "/5").with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(2003))
                .andExpect(jsonPath("$.floor").value(2))
                .andExpect(jsonPath("$.label").value("2003"));
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
    void shouldReturn409_whenUpdatingToNumberUsedByAnotherSpace() throws Exception {
        // Arrange
        willThrow(new ParkingSpaceConflictException("number", "El numero ya esta en uso"))
                .given(parkingSpaceService).update(eq(5L), any());

        // Act / Assert
        mockMvc.perform(put(BASE_URL + "/5").with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fields.number").exists());
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
        return new ParkingSpaceResponse(
                5L, NUMBER, LABEL, 1, true, Instant.parse("2026-01-01T00:00:00Z"));
    }
}
