package com.aleatica.parking.availability;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aleatica.parking.availability.application.AvailabilityService;
import com.aleatica.parking.availability.dto.AdminWeeklyCalendarResponse;
import com.aleatica.parking.availability.dto.MyWeekResponse;
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
 * Tests del adaptador web de calendario (@WebMvcTest): RBAC del calendario admin (401 sin
 * sesion, 403 para EMPLOYEE, 200 para ADMIN), validacion de {@code weekStart} (400 ausente
 * o con formato invalido), y "Mi Semana" para cualquier usuario autenticado (200 con o sin
 * weekStart, 401 sin sesion). La logica se mockea; aqui solo se verifica el contrato HTTP.
 */
@WebMvcTest(CalendarController.class)
@Import(SecurityConfig.class)
class CalendarControllerTest {

    private static final String ADMIN_URL = "/api/v1/calendar/admin";
    private static final String MY_WEEK_URL = "/api/v1/calendar/my-week";
    private static final String WEEK_START = "2026-07-06";

    private static final String ADMIN = "admin";
    private static final String EMP = "empleado";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_EMPLOYEE = "EMPLOYEE";
    private static final String ERROR_PATH = "$.error";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AvailabilityService availabilityService;

    // ---- Calendario admin ----

    @Test
    void shouldReturn401_whenAdminCalendarWithoutSession() throws Exception {
        mockMvc.perform(get(ADMIN_URL).param("weekStart", WEEK_START))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnForbidden_whenEmployeeCallsAdminCalendar() throws Exception {
        mockMvc.perform(get(ADMIN_URL).param("weekStart", WEEK_START).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath(ERROR_PATH).value("FORBIDDEN"));
    }

    @Test
    void shouldReturnAdminCalendar_whenCallerIsAdmin() throws Exception {
        // Arrange
        LocalDate monday = LocalDate.parse(WEEK_START);
        given(availabilityService.adminCalendar(any()))
                .willReturn(new AdminWeeklyCalendarResponse(monday, List.of(monday), List.of()));

        // Act / Assert
        mockMvc.perform(get(ADMIN_URL).param("weekStart", WEEK_START).with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekStart").value(WEEK_START));
    }

    @Test
    void shouldReturnBadRequest_whenWeekStartMissing() throws Exception {
        mockMvc.perform(get(ADMIN_URL).with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(ERROR_PATH).value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.weekStart").exists());
    }

    @Test
    void shouldReturnBadRequest_whenWeekStartInvalid() throws Exception {
        mockMvc.perform(get(ADMIN_URL).param("weekStart", "not-a-date").with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(ERROR_PATH).value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.weekStart").exists());
    }

    // ---- Mi Semana ----

    @Test
    void shouldReturn401_whenMyWeekWithoutSession() throws Exception {
        mockMvc.perform(get(MY_WEEK_URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnMyWeek_whenEmployeeAuthenticatedWithWeekStart() throws Exception {
        // Arrange
        LocalDate monday = LocalDate.parse(WEEK_START);
        given(availabilityService.myWeek(eq(EMP), any()))
                .willReturn(new MyWeekResponse(monday, List.of()));

        // Act / Assert
        mockMvc.perform(get(MY_WEEK_URL).param("weekStart", WEEK_START).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekStart").value(WEEK_START));
    }

    @Test
    void shouldReturnMyWeek_whenWeekStartOmitted() throws Exception {
        // Arrange: sin weekStart -> el controlador pasa null al servicio (semana actual)
        LocalDate monday = LocalDate.parse(WEEK_START);
        given(availabilityService.myWeek(eq(EMP), isNull()))
                .willReturn(new MyWeekResponse(monday, List.of()));

        // Act / Assert
        mockMvc.perform(get(MY_WEEK_URL).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekStart").value(WEEK_START));
    }
}
