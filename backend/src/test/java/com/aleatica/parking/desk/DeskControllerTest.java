package com.aleatica.parking.desk;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aleatica.parking.config.SecurityConfig;
import com.aleatica.parking.desk.application.DeskConflictException;
import com.aleatica.parking.desk.application.DeskService;
import com.aleatica.parking.desk.dto.DeskResponse;
import com.aleatica.parking.employee.dto.PageResponse;
import java.math.BigDecimal;
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
 * Tests del adaptador web de puestos (@WebMvcTest): RBAC (401 sin sesion, 403 para
 * EMPLOYEE en las mutaciones, 200 en lectura), codigos HTTP del CRUD, mapeo del rango
 * 1-65 a 400 y del numero duplicado a 409. La logica se mockea; aqui solo se verifica el
 * contrato HTTP y la autorizacion.
 */
@WebMvcTest(DeskController.class)
@Import(SecurityConfig.class)
class DeskControllerTest {

    private static final String BASE_URL = "/api/v1/desks";
    private static final String ADMIN = "admin";
    private static final String EMP = "empleado";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_EMPLOYEE = "EMPLOYEE";

    private static final String CREATE_BODY = "{\"number\":12,\"category\":\"STANDARD\",\"coordX\":30.5,\"coordY\":47.0}";
    private static final String UPDATE_BODY = "{\"category\":\"EXECUTIVE\"}";
    private static final String ACTIVATION_BODY = "{\"active\":false}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeskService deskService;

    @Test
    void shouldReturn401_whenListingWithoutSession() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403_whenEmployeeCreatesDesk() throws Exception {
        mockMvc.perform(post(BASE_URL).with(user(EMP).roles(ROLE_EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403_whenEmployeeUpdatesDesk() throws Exception {
        mockMvc.perform(put(BASE_URL + "/5").with(user(EMP).roles(ROLE_EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403_whenEmployeeSetsActivation() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/5/activation").with(user(EMP).roles(ROLE_EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON).content(ACTIVATION_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnPage_whenEmployeeListsDesks() throws Exception {
        // Arrange: la lectura del catalogo la puede hacer un EMPLOYEE
        given(deskService.list(any(), any()))
                .willReturn(new PageResponse<>(List.of(sample()), 1, 1, 20, 0, true, true));

        // Act / Assert
        mockMvc.perform(get(BASE_URL).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].number").value(12))
                .andExpect(jsonPath("$.content[0].category").value("STANDARD"));
    }

    @Test
    void shouldReturn201_whenAdminCreatesDesk() throws Exception {
        // Arrange
        given(deskService.create(any())).willReturn(sample());

        // Act / Assert
        mockMvc.perform(post(BASE_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.number").value(12))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldReturn400_whenDeskNumberNotPositive() throws Exception {
        // Act / Assert: el numero de puesto debe ser >= 1 (@Min(1)); el tope superior se quito (V34).
        mockMvc.perform(post(BASE_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":0,\"category\":\"STANDARD\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.number").exists());
    }

    @Test
    void shouldReturn409_whenCreatingDeskWithDuplicateNumber() throws Exception {
        // Arrange
        willThrow(new DeskConflictException("number", "El numero de puesto ya esta en uso"))
                .given(deskService).create(any());

        // Act / Assert
        mockMvc.perform(post(BASE_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.fields.number").exists());
    }

    @Test
    void shouldReturn200_whenAdminUpdatesCategory() throws Exception {
        // Arrange
        given(deskService.update(eq(5L), any()))
                .willReturn(new DeskResponse(5L, 12, "D-12", DeskCategory.EXECUTIVE,
                        new BigDecimal("30.5"), new BigDecimal("47.0"), true,
                        Instant.parse("2026-01-01T00:00:00Z")));

        // Act / Assert
        mockMvc.perform(put(BASE_URL + "/5").with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("EXECUTIVE"));
    }

    @Test
    void shouldReturn200_whenAdminDeactivatesDesk() throws Exception {
        // Arrange
        given(deskService.setActivation(eq(5L), eq(false)))
                .willReturn(new DeskResponse(5L, 12, "D-12", DeskCategory.STANDARD,
                        new BigDecimal("30.5"), new BigDecimal("47.0"), false,
                        Instant.parse("2026-01-01T00:00:00Z")));

        // Act / Assert
        mockMvc.perform(patch(BASE_URL + "/5/activation").with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(ACTIVATION_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    private DeskResponse sample() {
        return new DeskResponse(5L, 12, "D-12", DeskCategory.STANDARD,
                new BigDecimal("30.5"), new BigDecimal("47.0"), true,
                Instant.parse("2026-01-01T00:00:00Z"));
    }
}
