package com.aleatica.parking.systemsettings;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aleatica.parking.config.SecurityConfig;
import com.aleatica.parking.systemsettings.application.SystemSettingsService;
import com.aleatica.parking.systemsettings.domain.ApprovalMode;
import com.aleatica.parking.systemsettings.dto.ParkingAddressResponse;
import com.aleatica.parking.systemsettings.dto.WeekendReservableResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests del adaptador web de {@code GET /settings/approval-mode} (@WebMvcTest): a diferencia de
 * {@code GET /admin/settings} (reservado a ADMIN, ver {@link SystemSettingsControllerTest}), este
 * endpoint es accesible a CUALQUIER autenticado (EMPLOYEE o ADMIN) y solo requiere sesion
 * (401 sin ella). La logica se mockea; aqui solo se verifica el contrato HTTP.
 */
@WebMvcTest(ApprovalModeController.class)
@Import(SecurityConfig.class)
class ApprovalModeControllerTest {

    private static final String URL = "/api/v1/settings/approval-mode";
    private static final String ADDRESS_URL = "/api/v1/settings/parking-address";
    private static final String WEEKEND_URL = "/api/v1/settings/weekend-reservable";
    private static final String ADMIN = "admin";
    private static final String EMP = "empleado";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_EMPLOYEE = "EMPLOYEE";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SystemSettingsService systemSettingsService;

    @Test
    void shouldReturn401_whenGettingWithoutSession() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn200_whenEmployeeGetsApprovalMode() throws Exception {
        given(systemSettingsService.approvalMode()).willReturn(ApprovalMode.MANUAL);

        mockMvc.perform(get(URL).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalMode").value("MANUAL"))
                .andExpect(jsonPath("$.updatedById").doesNotExist())
                .andExpect(jsonPath("$.updatedAt").doesNotExist());
    }

    @Test
    void shouldReturn200_whenAdminGetsApprovalMode() throws Exception {
        given(systemSettingsService.approvalMode()).willReturn(ApprovalMode.AUTOMATIC);

        mockMvc.perform(get(URL).with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalMode").value("AUTOMATIC"));
    }

    // ---- Direccion del parking (lectura de cualquier autenticado) ----

    @Test
    void shouldReturn401_whenGettingParkingAddressWithoutSession() throws Exception {
        mockMvc.perform(get(ADDRESS_URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn200_whenEmployeeGetsParkingAddress() throws Exception {
        given(systemSettingsService.parkingAddress())
                .willReturn(new ParkingAddressResponse("Av. de Europa 18"));

        mockMvc.perform(get(ADDRESS_URL).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parkingAddress").value("Av. de Europa 18"));
    }

    // ---- Permiso de reservas en fin de semana (lectura de cualquier autenticado) ----

    @Test
    void shouldReturn401_whenGettingWeekendWithoutSession() throws Exception {
        mockMvc.perform(get(WEEKEND_URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn200_whenEmployeeGetsWeekendReservable() throws Exception {
        given(systemSettingsService.weekendReservableView())
                .willReturn(new WeekendReservableResponse(false));

        mockMvc.perform(get(WEEKEND_URL).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekendReservable").value(false));
    }
}
