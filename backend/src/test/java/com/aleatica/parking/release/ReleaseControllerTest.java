package com.aleatica.parking.release;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aleatica.parking.concurrency.ConcurrencyRetry;
import com.aleatica.parking.config.SecurityConfig;
import com.aleatica.parking.employee.dto.PageResponse;
import com.aleatica.parking.resource.ResourceType;
import com.aleatica.parking.release.application.NoFixedAssignmentException;
import com.aleatica.parking.release.application.PastReleaseCancellationException;
import com.aleatica.parking.release.application.ReleaseDateInPastException;
import com.aleatica.parking.release.application.ReleaseService;
import com.aleatica.parking.release.application.ResourceAlreadyReleasedException;
import com.aleatica.parking.release.domain.ReleaseType;
import com.aleatica.parking.release.dto.ReleaseResponse;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests del adaptador web de liberaciones (@WebMvcTest) sobre los 4 endpoints: RBAC
 * (401 sin sesion, 403 por rol), codigos HTTP de cada operacion, verificacion de
 * pertenencia (BOLA) traducida a 403, y la forma de error
 * {@code { error, message, fields, timestamp }} para 400/404/409. La logica se mockea;
 * aqui solo se verifica el contrato HTTP.
 */
@WebMvcTest(ReleaseController.class)
@Import({SecurityConfig.class, ConcurrencyRetry.class})
class ReleaseControllerTest {

    private static final String BASE_URL = "/api/v1/releases";
    private static final String MINE_URL = BASE_URL + "/mine";
    private static final String ADMIN_URL = BASE_URL + "/administrative";
    private static final String ADMIN_MINE_URL = ADMIN_URL + "/mine";
    private static final String ID_URL = BASE_URL + "/42";

    private static final String ADMIN = "admin";
    private static final String AGENCY = "agencia";
    private static final String EMP = "empleado";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_AGENCIA = "AGENCIA";
    private static final String ROLE_EMPLOYEE = "EMPLOYEE";

    private static final String CREATE_BODY = "{\"releaseDate\":\"2026-07-10\"}";
    private static final String ADMIN_BODY =
            "{\"employeeId\":15,\"parkingSpaceId\":8,\"releaseDate\":\"2026-07-10\",\"reason\":\"Ausencia\"}";
    private static final String ERROR_PATH = "$.error";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReleaseService releaseService;

    // ---- 401 sin sesion ----

    @Test
    void shouldReturn401_whenCreatingWithoutSession() throws Exception {
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401_whenListingMineWithoutSession() throws Exception {
        mockMvc.perform(get(MINE_URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401_whenAdministrativeWithoutSession() throws Exception {
        mockMvc.perform(post(ADMIN_URL).contentType(MediaType.APPLICATION_JSON).content(ADMIN_BODY))
                .andExpect(status().isUnauthorized());
    }

    // ---- 403 por rol ----

    @Test
    void shouldReturn403_whenAdminCreatesVoluntaryRelease() throws Exception {
        mockMvc.perform(post(BASE_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403_whenNonAdminCreatesAdministrativeRelease() throws Exception {
        mockMvc.perform(post(ADMIN_URL).with(user(EMP).roles(ROLE_EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON).content(ADMIN_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath(ERROR_PATH).value("FORBIDDEN"));
        verify(releaseService, never()).createAdministrativeRelease(anyString(), any());
    }

    @Test
    void shouldReturn403_whenAdminListsMine() throws Exception {
        mockMvc.perform(get(MINE_URL).with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403_whenAdminCancelsRelease() throws Exception {
        mockMvc.perform(delete(ID_URL).with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isForbidden());
    }

    // ---- Caminos felices y codigos ----

    @Test
    void shouldReturn201_whenEmployeeCreatesVoluntaryRelease() throws Exception {
        // Arrange
        given(releaseService.createRelease(anyString(), any())).willReturn(sample(ReleaseType.VOLUNTARY));

        // Act / Assert
        mockMvc.perform(post(BASE_URL).with(user(EMP).roles(ROLE_EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("VOLUNTARY"));
    }

    @Test
    void shouldReturn201_whenAdminCreatesAdministrativeRelease() throws Exception {
        // Arrange
        given(releaseService.createAdministrativeRelease(anyString(), any()))
                .willReturn(sample(ReleaseType.ADMINISTRATIVE));

        // Act / Assert
        mockMvc.perform(post(ADMIN_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(ADMIN_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("ADMINISTRATIVE"));
    }

    @Test
    void shouldReturn200_whenEmployeeListsOwnReleases() throws Exception {
        // Arrange
        given(releaseService.listMyReleases(anyString(), any()))
                .willReturn(new PageResponse<>(List.of(sample(ReleaseType.VOLUNTARY)), 1, 1, 20, 0, true, true));

        // Act / Assert
        mockMvc.perform(get(MINE_URL).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].employeeId").value(15))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldReturn204_whenEmployeeCancelsOwnRelease() throws Exception {
        // Arrange
        willDoNothing().given(releaseService).cancelRelease(eq(42L), anyString());

        // Act / Assert
        mockMvc.perform(delete(ID_URL).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn403_whenCancellingOtherEmployeeRelease() throws Exception {
        // Arrange: el servicio aplica BOLA y lanza AccessDenied
        willThrow(new AccessDeniedException("ajena")).given(releaseService).cancelRelease(eq(42L), anyString());

        // Act / Assert
        mockMvc.perform(delete(ID_URL).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath(ERROR_PATH).value("FORBIDDEN"));
    }

    @Test
    void shouldReturn409_whenCancellingPastRelease() throws Exception {
        // Arrange
        willThrow(new PastReleaseCancellationException("pasada"))
                .given(releaseService).cancelRelease(eq(42L), anyString());

        // Act / Assert
        mockMvc.perform(delete(ID_URL).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(ERROR_PATH).value("RELEASE_NOT_CANCELLABLE"));
    }

    @Test
    void shouldReturn404_whenCancellingUnknownRelease() throws Exception {
        // Arrange
        willThrow(new EntityNotFoundException("no")).given(releaseService).cancelRelease(eq(42L), anyString());

        // Act / Assert
        mockMvc.perform(delete(ID_URL).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(ERROR_PATH).value("NOT_FOUND"));
    }

    @Test
    void shouldReturn400_whenCreatingWithReleaseDateInPast() throws Exception {
        // Arrange
        willThrow(new ReleaseDateInPastException("pasado"))
                .given(releaseService).createRelease(anyString(), any());

        // Act / Assert
        mockMvc.perform(post(BASE_URL).with(user(EMP).roles(ROLE_EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(ERROR_PATH).value("RELEASE_DATE_IN_PAST"))
                .andExpect(jsonPath("$.fields.releaseDate").exists());
    }

    @Test
    void shouldReturn409_whenNoFixedAssignmentForThatDay() throws Exception {
        // Arrange
        willThrow(new NoFixedAssignmentException("sin asignacion"))
                .given(releaseService).createRelease(anyString(), any());

        // Act / Assert
        mockMvc.perform(post(BASE_URL).with(user(EMP).roles(ROLE_EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(ERROR_PATH).value("NO_FIXED_ASSIGNMENT"));
    }

    @Test
    void shouldReturn409_whenResourceAlreadyReleased() throws Exception {
        // Arrange
        willThrow(new ResourceAlreadyReleasedException("ya liberado"))
                .given(releaseService).createRelease(anyString(), any());

        // Act / Assert
        mockMvc.perform(post(BASE_URL).with(user(EMP).roles(ROLE_EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(ERROR_PATH).value("RESOURCE_ALREADY_RELEASED"))
                .andExpect(jsonPath("$.fields.parkingSpaceId").exists());
    }

    @Test
    void shouldReturn400_whenAdministrativeReleaseMissingReason() throws Exception {
        // Act / Assert: reason ausente -> @NotBlank -> 400 con fields.reason
        String noReason = "{\"employeeId\":15,\"parkingSpaceId\":8,\"releaseDate\":\"2026-07-10\"}";
        mockMvc.perform(post(ADMIN_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(noReason))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(ERROR_PATH).value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.reason").exists());
        verify(releaseService, never()).createAdministrativeRelease(anyString(), any());
    }

    @Test
    void shouldReturn400_whenCreatingWithoutReleaseDate() throws Exception {
        // Act / Assert: releaseDate ausente -> @NotNull -> 400
        mockMvc.perform(post(BASE_URL).with(user(EMP).roles(ROLE_EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(ERROR_PATH).value("VALIDATION_ERROR"));
    }

    @Test
    void shouldReturn404_whenAdministrativeEmployeeUnknown() throws Exception {
        // Arrange
        willThrow(new EntityNotFoundException("no"))
                .given(releaseService).createAdministrativeRelease(anyString(), any());

        // Act / Assert
        mockMvc.perform(post(ADMIN_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(ADMIN_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(ERROR_PATH).value("NOT_FOUND"));
    }

    // ---- Historial de liberaciones administrativas propias (GET /releases/administrative/mine) ----

    @Test
    void shouldReturn401_whenListingMyAdministrativeReleasesWithoutSession() throws Exception {
        mockMvc.perform(get(ADMIN_MINE_URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403_whenEmployeeListsMyAdministrativeReleases() throws Exception {
        mockMvc.perform(get(ADMIN_MINE_URL).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath(ERROR_PATH).value("FORBIDDEN"));
    }

    @Test
    void shouldReturn200_whenAdminListsMyAdministrativeReleases() throws Exception {
        // Arrange
        given(releaseService.listMyAdministrativeReleases(anyString(), any()))
                .willReturn(new PageResponse<>(List.of(sample(ReleaseType.ADMINISTRATIVE)), 1, 1, 20, 0, true, true));

        // Act / Assert
        mockMvc.perform(get(ADMIN_MINE_URL).with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("ADMINISTRATIVE"));
    }

    @Test
    void shouldReturn200_whenAgencyListsMyAdministrativeReleases() throws Exception {
        // Arrange (change restructure-admin-workflows, design D5): AGENCIA ve su propio historial
        given(releaseService.listMyAdministrativeReleases(anyString(), any()))
                .willReturn(new PageResponse<>(List.of(sample(ReleaseType.ADMINISTRATIVE)), 1, 1, 20, 0, true, true));

        // Act / Assert
        mockMvc.perform(get(ADMIN_MINE_URL).with(user(AGENCY).roles(ROLE_AGENCIA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("ADMINISTRATIVE"));
    }

    private ReleaseResponse sample(ReleaseType type) {
        String reason = type == ReleaseType.ADMINISTRATIVE ? "Ausencia" : null;
        Long releasedBy = type == ReleaseType.ADMINISTRATIVE ? 1L : 15L;
        return new ReleaseResponse(
                42L, 8L, 15L, LocalDate.of(2026, 7, 10), type, reason, releasedBy,
                Instant.parse("2026-07-04T10:00:00Z"), ResourceType.PARKING);
    }
}
