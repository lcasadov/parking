package com.aleatica.parking.request;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aleatica.parking.concurrency.ConcurrencyRetry;
import com.aleatica.parking.config.SecurityConfig;
import com.aleatica.parking.employee.dto.PageResponse;
import com.aleatica.parking.request.application.DuplicatePendingRequestException;
import com.aleatica.parking.request.application.NoAvailabilityException;
import com.aleatica.parking.request.application.OutsideRequestWindowException;
import com.aleatica.parking.request.application.RejectionReasonRequiredException;
import com.aleatica.parking.request.application.RequestNotPendingException;
import com.aleatica.parking.request.application.RequestService;
import com.aleatica.parking.request.application.RequestStateException;
import com.aleatica.parking.request.application.ResendTooSoonException;
import com.aleatica.parking.request.application.ResourceSelectionRequiredException;
import com.aleatica.parking.request.application.SpaceUnavailableException;
import com.aleatica.parking.request.domain.RequestStatus;
import com.aleatica.parking.request.dto.RequestResponse;
import com.aleatica.parking.request.dto.RequestSwapResponse;
import com.aleatica.parking.request.dto.SuggestedParkingSpaceResponse;
import com.aleatica.parking.request.dto.SuggestedResourceResponse;
import com.aleatica.parking.resource.ResourceType;
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
 * Tests del adaptador web de solicitudes (@WebMvcTest) sobre los 7 endpoints: RBAC
 * (401 sin sesion, 403 por rol), codigos HTTP de cada operacion, verificacion de
 * pertenencia (BOLA) traducida a 403, y la forma de error
 * {@code { error, message, fields, timestamp }} para 400/404/409. La logica se mockea;
 * aqui solo se verifica el contrato HTTP.
 */
@WebMvcTest(RequestController.class)
@Import({SecurityConfig.class, ConcurrencyRetry.class})
class RequestControllerTest {

    private static final String BASE_URL = "/api/v1/requests";
    private static final String MINE_URL = BASE_URL + "/mine";
    private static final String SUGGESTED_URL = BASE_URL + "/suggested";
    private static final String ADMIN_ASSIGN_URL = BASE_URL + "/admin";
    private static final String REASSIGN_URL = BASE_URL + "/admin/reassign";
    private static final String SWAP_URL = BASE_URL + "/admin/swap";
    private static final String REASSIGN_BODY = "{\"requestId\":42,\"newResourceId\":9}";
    private static final String SWAP_BODY = "{\"requestIdA\":42,\"requestIdB\":43}";
    private static final String SUGGESTED_SPACE_URL = BASE_URL + "/admin/suggested-space";
    private static final String PENDING_URL = BASE_URL + "/pending";
    private static final String ID_URL = BASE_URL + "/42";
    private static final String CANCEL_URL = ID_URL + "/cancel";
    private static final String APPROVE_URL = ID_URL + "/approve";
    private static final String REJECT_URL = ID_URL + "/reject";
    private static final String RESEND_URL = ID_URL + "/resend";

    private static final String ADMIN = "admin";
    private static final String EMP = "empleado";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_EMPLOYEE = "EMPLOYEE";

    private static final String CREATE_BODY = "{\"requestedDate\":\"2026-07-10\"}";
    private static final String ADMIN_ASSIGN_BODY =
            "{\"employeeId\":15,\"requestedDate\":\"2026-07-10\",\"resourceType\":\"PARKING\","
                    + "\"resourceId\":8}";
    private static final String APPROVE_BODY = "{\"parkingSpaceId\":8,\"approvalNote\":\"ok\"}";
    private static final String REJECT_BODY = "{\"reasonCode\":\"NO_AVAILABILITY\"}";
    private static final String ERROR_PATH = "$.error";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RequestService requestService;

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
    void shouldReturn401_whenListingPendingWithoutSession() throws Exception {
        mockMvc.perform(get(PENDING_URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401_whenListingByStatusWithoutSession() throws Exception {
        mockMvc.perform(get(BASE_URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401_whenResendingWithoutSession() throws Exception {
        mockMvc.perform(post(RESEND_URL)).andExpect(status().isUnauthorized());
    }

    // ---- 403 por rol ----

    @Test
    void shouldReturn403_whenEmployeeListsPending() throws Exception {
        mockMvc.perform(get(PENDING_URL).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath(ERROR_PATH).value("FORBIDDEN"));
    }

    @Test
    void shouldReturn403_whenEmployeeListsByStatus() throws Exception {
        mockMvc.perform(get(BASE_URL).param("status", "APPROVED").with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath(ERROR_PATH).value("FORBIDDEN"));
    }

    @Test
    void shouldReturn403_whenEmployeeGetsRequestDetail() throws Exception {
        mockMvc.perform(get(ID_URL).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403_whenEmployeeApproves() throws Exception {
        mockMvc.perform(post(APPROVE_URL).with(user(EMP).roles(ROLE_EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON).content(APPROVE_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403_whenEmployeeRejects() throws Exception {
        mockMvc.perform(post(REJECT_URL).with(user(EMP).roles(ROLE_EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON).content(REJECT_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403_whenAdminCreatesRequest() throws Exception {
        mockMvc.perform(post(BASE_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403_whenAdminCancelsRequest() throws Exception {
        mockMvc.perform(post(CANCEL_URL).with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403_whenAdminResendsRequest() throws Exception {
        // RBAC: el reenvio es exclusivo de EMPLOYEE (el dueno), no de ADMIN
        mockMvc.perform(post(RESEND_URL).with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isForbidden());
    }

    // ---- Caminos felices y codigos ----

    @Test
    void shouldReturn201_whenEmployeeCreatesRequest() throws Exception {
        // Arrange
        given(requestService.create(anyString(), any())).willReturn(sample(RequestStatus.PENDING));

        // Act / Assert
        mockMvc.perform(post(BASE_URL).with(user(EMP).roles(ROLE_EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldReturn200_whenEmployeeGetsSuggestedResource() throws Exception {
        // Arrange
        given(requestService.suggestForEmployee(anyString(), any(), any()))
                .willReturn(SuggestedResourceResponse.available("Plaza 3005"));

        // Act / Assert
        mockMvc.perform(get(SUGGESTED_URL).param("date", "2026-07-10").param("resourceType", "PARKING")
                        .with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.resourceLabel").value("Plaza 3005"));
    }

    @Test
    void shouldReturn403_whenAdminGetsSuggestedResource() throws Exception {
        mockMvc.perform(get(SUGGESTED_URL).param("date", "2026-07-10")
                        .with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn400_whenSuggestedResourceMissingDate() throws Exception {
        mockMvc.perform(get(SUGGESTED_URL).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn200_whenEmployeeListsOwnRequests() throws Exception {
        // Arrange
        given(requestService.listMine(anyString(), eq(null), eq(null), eq(null), any()))
                .willReturn(new PageResponse<>(List.of(sample(RequestStatus.PENDING)), 1, 1, 20, 0, true, true));

        // Act / Assert
        mockMvc.perform(get(MINE_URL).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].employeeId").value(15))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldReturn200_whenAdminListsPending() throws Exception {
        // Arrange
        given(requestService.listPending(any()))
                .willReturn(new PageResponse<>(List.of(sample(RequestStatus.PENDING)), 1, 1, 20, 0, true, true));

        // Act / Assert
        mockMvc.perform(get(PENDING_URL).with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));
    }

    @Test
    void shouldReturn200_whenAdminListsByStatus() throws Exception {
        // Arrange
        given(requestService.listByStatus(eq(RequestStatus.APPROVED), any()))
                .willReturn(new PageResponse<>(List.of(sample(RequestStatus.APPROVED)), 1, 1, 20, 0, true, true));

        // Act / Assert
        mockMvc.perform(get(BASE_URL).param("status", "APPROVED").with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("APPROVED"));
    }

    @Test
    void shouldReturn200_whenAdminListsAllWithoutStatusFilter() throws Exception {
        // Arrange: sin filtro de estado -> el servicio recibe status nulo
        given(requestService.listByStatus(eq(null), any()))
                .willReturn(new PageResponse<>(List.of(sample(RequestStatus.REJECTED)), 1, 1, 20, 0, true, true));

        // Act / Assert
        mockMvc.perform(get(BASE_URL).with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("REJECTED"));
    }

    @Test
    void shouldReturn200_whenAdminGetsRequestDetail() throws Exception {
        // Arrange
        given(requestService.get(42L)).willReturn(sample(RequestStatus.PENDING));

        // Act / Assert
        mockMvc.perform(get(ID_URL).with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42));
    }

    @Test
    void shouldReturn404_whenGettingUnknownRequest() throws Exception {
        // Arrange
        willThrow(new EntityNotFoundException("no")).given(requestService).get(42L);

        // Act / Assert
        mockMvc.perform(get(ID_URL).with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(ERROR_PATH).value("NOT_FOUND"));
    }

    @Test
    void shouldReturn200_whenEmployeeCancelsOwnRequest() throws Exception {
        // Arrange
        given(requestService.cancel(eq(42L), anyString())).willReturn(sample(RequestStatus.CANCELLED));

        // Act / Assert
        mockMvc.perform(post(CANCEL_URL).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void shouldReturn403_whenCancellingOtherEmployeeRequest() throws Exception {
        // Arrange: el servicio aplica BOLA y lanza AccessDenied
        willThrow(new AccessDeniedException("ajena")).given(requestService).cancel(eq(42L), anyString());

        // Act / Assert
        mockMvc.perform(post(CANCEL_URL).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath(ERROR_PATH).value("FORBIDDEN"));
    }

    @Test
    void shouldReturn409_whenCancellingResolvedRequest() throws Exception {
        // Arrange
        willThrow(new RequestStateException("terminal")).given(requestService).cancel(eq(42L), anyString());

        // Act / Assert
        mockMvc.perform(post(CANCEL_URL).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(ERROR_PATH).value("CONFLICT"));
    }

    @Test
    void shouldReturn200_whenEmployeeResendsOwnPendingRequest() throws Exception {
        // Arrange
        given(requestService.resend(eq(42L), anyString())).willReturn(sample(RequestStatus.PENDING));

        // Act / Assert
        mockMvc.perform(post(RESEND_URL).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldReturn403_whenResendingOtherEmployeeRequest() throws Exception {
        // Arrange: el servicio aplica BOLA y lanza AccessDenied
        willThrow(new AccessDeniedException("ajena")).given(requestService).resend(eq(42L), anyString());

        // Act / Assert
        mockMvc.perform(post(RESEND_URL).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath(ERROR_PATH).value("FORBIDDEN"));
    }

    @Test
    void shouldReturn404_whenResendingUnknownRequest() throws Exception {
        // Arrange
        willThrow(new EntityNotFoundException("no")).given(requestService).resend(eq(42L), anyString());

        // Act / Assert
        mockMvc.perform(post(RESEND_URL).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(ERROR_PATH).value("NOT_FOUND"));
    }

    @Test
    void shouldReturn409_whenResendingNonPendingRequest() throws Exception {
        // Arrange
        willThrow(new RequestNotPendingException("no pendiente"))
                .given(requestService).resend(eq(42L), anyString());

        // Act / Assert
        mockMvc.perform(post(RESEND_URL).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(ERROR_PATH).value("REQUEST_NOT_PENDING"));
    }

    @Test
    void shouldReturn409_whenResendingTooSoon() throws Exception {
        // Arrange
        willThrow(new ResendTooSoonException("demasiado pronto"))
                .given(requestService).resend(eq(42L), anyString());

        // Act / Assert
        mockMvc.perform(post(RESEND_URL).with(user(EMP).roles(ROLE_EMPLOYEE)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(ERROR_PATH).value("RESEND_TOO_SOON"));
    }

    @Test
    void shouldReturn200_whenAdminApproves() throws Exception {
        // Arrange
        given(requestService.approve(eq(42L), any(), anyString())).willReturn(sample(RequestStatus.APPROVED));

        // Act / Assert
        mockMvc.perform(post(APPROVE_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(APPROVE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void shouldReturn409_whenApprovingUnavailableSpace() throws Exception {
        // Arrange
        willThrow(new SpaceUnavailableException("no disponible"))
                .given(requestService).approve(eq(42L), any(), anyString());

        // Act / Assert
        mockMvc.perform(post(APPROVE_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(APPROVE_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(ERROR_PATH).value("SPACE_NOT_AVAILABLE"));
    }

    @Test
    void shouldReturn400_whenApprovingWithoutSpace() throws Exception {
        // Act / Assert: parkingSpaceId ausente -> @NotNull -> 400
        mockMvc.perform(post(APPROVE_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"approvalNote\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(ERROR_PATH).value("VALIDATION_ERROR"));
    }

    @Test
    void shouldReturn200_whenAdminRejects() throws Exception {
        // Arrange
        given(requestService.reject(eq(42L), any(), anyString())).willReturn(sample(RequestStatus.REJECTED));

        // Act / Assert
        mockMvc.perform(post(REJECT_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(REJECT_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void shouldReturn400_whenRejectingOtherWithoutFreeText() throws Exception {
        // Arrange: reasonCode OTHER sin texto -> el servicio lanza validacion
        willThrow(new RejectionReasonRequiredException("obligatorio"))
                .given(requestService).reject(eq(42L), any(), anyString());

        // Act / Assert
        mockMvc.perform(post(REJECT_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reasonCode\":\"OTHER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(ERROR_PATH).value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.rejectionReason").exists());
    }

    @Test
    void shouldReturn400_whenRejectingWithoutReasonCode() throws Exception {
        // Act / Assert: reasonCode ausente -> @NotNull -> 400
        mockMvc.perform(post(REJECT_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(ERROR_PATH).value("VALIDATION_ERROR"));
    }

    @Test
    void shouldReturn400_whenCreatingOutsideWindow() throws Exception {
        // Arrange
        willThrow(new OutsideRequestWindowException("fuera")).given(requestService).create(anyString(), any());

        // Act / Assert
        mockMvc.perform(post(BASE_URL).with(user(EMP).roles(ROLE_EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(ERROR_PATH).value("OUTSIDE_REQUEST_WINDOW"))
                .andExpect(jsonPath("$.fields.requestedDate").exists());
    }

    @Test
    void shouldReturn409_whenCreatingDuplicatePending() throws Exception {
        // Arrange
        willThrow(new DuplicatePendingRequestException("dup")).given(requestService).create(anyString(), any());

        // Act / Assert
        mockMvc.perform(post(BASE_URL).with(user(EMP).roles(ROLE_EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(ERROR_PATH).value("REQUEST_ALREADY_PENDING"));
    }

    // ---- Asignacion puntual del admin (POST /requests/admin) ----

    @Test
    void shouldReturn401_whenAdminAssignWithoutSession() throws Exception {
        mockMvc.perform(post(ADMIN_ASSIGN_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(ADMIN_ASSIGN_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403_whenEmployeeUsesAdminAssign() throws Exception {
        mockMvc.perform(post(ADMIN_ASSIGN_URL).with(user(EMP).roles(ROLE_EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON).content(ADMIN_ASSIGN_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath(ERROR_PATH).value("FORBIDDEN"));
    }

    @Test
    void shouldReturn201_whenAdminAssignsResource() throws Exception {
        // Arrange
        given(requestService.adminAssign(anyString(), any())).willReturn(sample(RequestStatus.APPROVED));

        // Act / Assert
        mockMvc.perform(post(ADMIN_ASSIGN_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(ADMIN_ASSIGN_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    // ---- Reasignacion / swap admin (Feature B) ----

    @Test
    void shouldReturn403_whenEmployeeReassigns() throws Exception {
        mockMvc.perform(post(REASSIGN_URL).with(user(EMP).roles(ROLE_EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON).content(REASSIGN_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath(ERROR_PATH).value("FORBIDDEN"));
    }

    @Test
    void shouldReturn200_whenAdminReassigns() throws Exception {
        // Arrange
        given(requestService.adminReassign(anyString(), any()))
                .willReturn(sample(RequestStatus.APPROVED));

        // Act / Assert
        mockMvc.perform(post(REASSIGN_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(REASSIGN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void shouldReturn400_whenReassignMissingRequiredFields() throws Exception {
        mockMvc.perform(post(REASSIGN_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(ERROR_PATH).value("VALIDATION_ERROR"));
    }

    @Test
    void shouldReturn409_whenReassignTargetOccupied() throws Exception {
        // Arrange
        willThrow(new SpaceUnavailableException("ocupada"))
                .given(requestService).adminReassign(anyString(), any());

        // Act / Assert
        mockMvc.perform(post(REASSIGN_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(REASSIGN_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(ERROR_PATH).value("SPACE_NOT_AVAILABLE"));
    }

    @Test
    void shouldReturn403_whenEmployeeSwaps() throws Exception {
        mockMvc.perform(post(SWAP_URL).with(user(EMP).roles(ROLE_EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON).content(SWAP_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath(ERROR_PATH).value("FORBIDDEN"));
    }

    @Test
    void shouldReturn200_whenAdminSwaps() throws Exception {
        // Arrange
        given(requestService.adminSwap(anyString(), any())).willReturn(
                new RequestSwapResponse(sample(RequestStatus.APPROVED), sample(RequestStatus.APPROVED)));

        // Act / Assert
        mockMvc.perform(post(SWAP_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(SWAP_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestA.status").value("APPROVED"))
                .andExpect(jsonPath("$.requestB.status").value("APPROVED"));
    }

    @Test
    void shouldReturn409_whenSwapDifferentDates() throws Exception {
        // Arrange
        willThrow(new RequestStateException("fechas distintas"))
                .given(requestService).adminSwap(anyString(), any());

        // Act / Assert
        mockMvc.perform(post(SWAP_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(SWAP_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(ERROR_PATH).value("CONFLICT"));
    }

    @Test
    void shouldReturn400_whenAdminAssignMissingRequiredFields() throws Exception {
        // Act / Assert: employeeId y requestedDate ausentes -> @NotNull -> 400
        mockMvc.perform(post(ADMIN_ASSIGN_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(ERROR_PATH).value("VALIDATION_ERROR"));
    }

    @Test
    void shouldReturn400_whenAdminAssignDeskWithoutResourceId() throws Exception {
        // Arrange
        willThrow(new ResourceSelectionRequiredException("obligatorio"))
                .given(requestService).adminAssign(anyString(), any());

        // Act / Assert
        mockMvc.perform(post(ADMIN_ASSIGN_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeId\":15,\"requestedDate\":\"2026-07-10\","
                                + "\"resourceType\":\"DESK\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(ERROR_PATH).value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.resourceId").exists());
    }

    @Test
    void shouldReturn409_whenAdminAssignResourceUnavailable() throws Exception {
        // Arrange
        willThrow(new SpaceUnavailableException("ocupado"))
                .given(requestService).adminAssign(anyString(), any());

        // Act / Assert
        mockMvc.perform(post(ADMIN_ASSIGN_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(ADMIN_ASSIGN_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(ERROR_PATH).value("SPACE_NOT_AVAILABLE"));
    }

    @Test
    void shouldReturn409_whenAdminAssignAutoAssignHasNoAvailability() throws Exception {
        // Arrange
        willThrow(new NoAvailabilityException("sin plazas"))
                .given(requestService).adminAssign(anyString(), any());

        // Act / Assert
        mockMvc.perform(post(ADMIN_ASSIGN_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeId\":15,\"requestedDate\":\"2026-07-10\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(ERROR_PATH).value("NO_AVAILABILITY"));
    }

    // ---- Vista previa de la plaza auto-asignada (GET /requests/admin/suggested-space) ----

    @Test
    void shouldReturn401_whenSuggestedSpaceWithoutSession() throws Exception {
        mockMvc.perform(get(SUGGESTED_SPACE_URL).param("employeeId", "15").param("date", "2026-07-10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403_whenEmployeeRequestsSuggestedSpace() throws Exception {
        mockMvc.perform(get(SUGGESTED_SPACE_URL).with(user(EMP).roles(ROLE_EMPLOYEE))
                        .param("employeeId", "15").param("date", "2026-07-10"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath(ERROR_PATH).value("FORBIDDEN"));
    }

    @Test
    void shouldReturn200WithSpace_whenAdminRequestsSuggestedSpaceAndAvailable() throws Exception {
        // Arrange
        given(requestService.suggestedSpace(15L, LocalDate.of(2026, 7, 10)))
                .willReturn(new SuggestedParkingSpaceResponse(true, 8L, 1001, 1));

        // Act / Assert
        mockMvc.perform(get(SUGGESTED_SPACE_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .param("employeeId", "15").param("date", "2026-07-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.parkingSpaceId").value(8))
                .andExpect(jsonPath("$.number").value(1001))
                .andExpect(jsonPath("$.floor").value(1));
    }

    @Test
    void shouldReturn200Unavailable_whenAdminRequestsSuggestedSpaceAndNoneFree() throws Exception {
        // Arrange
        given(requestService.suggestedSpace(15L, LocalDate.of(2026, 7, 10)))
                .willReturn(SuggestedParkingSpaceResponse.unavailable());

        // Act / Assert
        mockMvc.perform(get(SUGGESTED_SPACE_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .param("employeeId", "15").param("date", "2026-07-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.parkingSpaceId").value(nullValue()));
    }

    @Test
    void shouldReturn404_whenSuggestedSpaceEmployeeUnknown() throws Exception {
        // Arrange
        willThrow(new EntityNotFoundException("no")).given(requestService)
                .suggestedSpace(999L, LocalDate.of(2026, 7, 10));

        // Act / Assert
        mockMvc.perform(get(SUGGESTED_SPACE_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .param("employeeId", "999").param("date", "2026-07-10"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(ERROR_PATH).value("NOT_FOUND"));
    }

    @Test
    void shouldReturn400_whenSuggestedSpaceMissingParams() throws Exception {
        mockMvc.perform(get(SUGGESTED_SPACE_URL).with(user(ADMIN).roles(ROLE_ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(ERROR_PATH).value("VALIDATION_ERROR"));
    }

    @Test
    void shouldReturn400_whenSuggestedSpaceDateMalformed() throws Exception {
        mockMvc.perform(get(SUGGESTED_SPACE_URL).with(user(ADMIN).roles(ROLE_ADMIN))
                        .param("employeeId", "15").param("date", "not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(ERROR_PATH).value("VALIDATION_ERROR"));
    }

    private RequestResponse sample(RequestStatus status) {
        return new RequestResponse(
                42L, 15L, LocalDate.of(2026, 7, 10), status,
                null, null, null, null, null, null, Instant.parse("2026-07-04T10:00:00Z"), ResourceType.PARKING,
                null, null);
    }
}
