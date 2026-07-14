package com.aleatica.parking.request.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.aleatica.parking.audit.AuditEntry;
import com.aleatica.parking.audit.AuditRecorder;
import com.aleatica.parking.auth.domain.ClockPort;
import com.aleatica.parking.availability.application.AvailabilityService;
import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.notification.event.RequestApprovedEvent;
import com.aleatica.parking.notification.event.RequestCreatedEvent;
import com.aleatica.parking.notification.event.RequestRejectedEvent;
import com.aleatica.parking.request.domain.RejectionReasonCode;
import com.aleatica.parking.request.domain.Request;
import com.aleatica.parking.request.domain.RequestRepositoryPort;
import com.aleatica.parking.request.domain.RequestStatus;
import com.aleatica.parking.request.dto.RequestApproveRequest;
import com.aleatica.parking.request.dto.RequestCreateRequest;
import com.aleatica.parking.request.dto.RequestRejectRequest;
import com.aleatica.parking.request.dto.RequestResponse;
import com.aleatica.parking.resource.BookableResource;
import com.aleatica.parking.resource.ResourceResolvers;
import com.aleatica.parking.resource.ResourceType;
import com.aleatica.parking.systemsettings.application.SystemSettingsService;
import com.aleatica.parking.systemsettings.domain.ApprovalMode;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

/**
 * Tests unitarios de {@link RequestService} con un <strong>fake in-memory del puerto</strong>
 * {@link RequestRepositoryPort} (no un mock de Spring Data): ventana temporal, unicidad
 * {@code PENDING}, maquina de estados, verificacion de pertenencia (BOLA), disponibilidad al
 * aprobar y validacion del catalogo de rechazo. El servicio opera con el modelo de dominio; el
 * resto de colaboradores ({@code AvailabilityService}, repositorios de empleado, publicador de
 * eventos) se mockean. No toca la base de datos.
 */
@ExtendWith(MockitoExtension.class)
class RequestServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-04T10:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 4);
    private static final LocalDate WITHIN = LocalDate.of(2026, 7, 10);
    private static final LocalDate MAX_DAY = LocalDate.of(2026, 7, 18);
    private static final LocalDate BEFORE = LocalDate.of(2026, 7, 3);
    private static final LocalDate AFTER = LocalDate.of(2026, 7, 19);

    private static final String EMP_LOGIN = "employee";
    private static final String OTHER_LOGIN = "otheremployee";
    private static final String ADMIN_LOGIN = "admin";
    private static final Long EMP_ID = 15L;
    private static final Long OTHER_ID = 99L;
    private static final Long ADMIN_ID = 1L;
    private static final Long SPACE_ID = 8L;
    private static final Long REQUEST_ID = 42L;
    private static final String VALID_REASON = "Motivo detallado";

    private final InMemoryRequestRepository requestRepository = new InMemoryRequestRepository();

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ResourceResolvers resourceResolvers;

    @Mock
    private AvailabilityService availabilityService;

    @Mock
    private SystemSettingsService systemSettingsService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AuditRecorder auditRecorder;

    @Captor
    private ArgumentCaptor<Object> eventCaptor;

    @Captor
    private ArgumentCaptor<AuditEntry> auditCaptor;

    private final ClockPort clock = () -> NOW;

    private RequestService newService() {
        return new RequestService(
                requestRepository, employeeRepository, resourceResolvers,
                availabilityService, systemSettingsService, eventPublisher, auditRecorder, clock);
    }

    private void givenManualMode() {
        given(systemSettingsService.approvalMode()).willReturn(ApprovalMode.MANUAL);
    }

    // ---- Creacion: ventana + unicidad ----

    @Test
    void shouldCreatePendingRequest_whenDateWithinWindow() {
        // Arrange
        givenActor(EMP_LOGIN, EMP_ID);
        givenManualMode();

        // Act
        RequestResponse result = newService().create(EMP_LOGIN, new RequestCreateRequest(WITHIN, null));

        // Assert: estado inicial PENDING, sin plaza, con la fecha, el empleado y la marca de tiempo
        assertThat(result.status()).isEqualTo(RequestStatus.PENDING);
        assertThat(result.parkingSpaceId()).isNull();
        assertThat(result.requestedDate()).isEqualTo(WITHIN);
        assertThat(result.createdAt()).isEqualTo(NOW);
        assertThat(requestRepository.saves()).isEqualTo(1);
        verifyEventPublished(RequestCreatedEvent.class);
    }

    @Test
    void shouldCreatePendingRequest_whenDateExactlyToday() {
        // Arrange (frontera inferior inclusive)
        givenActor(EMP_LOGIN, EMP_ID);
        givenManualMode();

        // Act / Assert
        assertThat(newService().create(EMP_LOGIN, new RequestCreateRequest(TODAY, null)).status())
                .isEqualTo(RequestStatus.PENDING);
    }

    @Test
    void shouldCreatePendingRequest_whenDateExactlyMaxDay() {
        // Arrange (frontera superior inclusive: hoy+14)
        givenActor(EMP_LOGIN, EMP_ID);
        givenManualMode();

        // Act / Assert
        assertThat(newService().create(EMP_LOGIN, new RequestCreateRequest(MAX_DAY, null)).status())
                .isEqualTo(RequestStatus.PENDING);
    }

    @Test
    void shouldThrowOutsideWindow_whenDateBeforeToday() {
        // Arrange
        givenActor(EMP_LOGIN, EMP_ID);

        // Act / Assert
        assertThatThrownBy(() -> newService().create(EMP_LOGIN, new RequestCreateRequest(BEFORE, null)))
                .isInstanceOf(OutsideRequestWindowException.class);
        assertThat(requestRepository.saves()).isZero();
    }

    @Test
    void shouldThrowOutsideWindow_whenDateAfterMaxDay() {
        // Arrange
        givenActor(EMP_LOGIN, EMP_ID);

        // Act / Assert
        assertThatThrownBy(() -> newService().create(EMP_LOGIN, new RequestCreateRequest(AFTER, null)))
                .isInstanceOf(OutsideRequestWindowException.class);
        assertThat(requestRepository.saves()).isZero();
    }

    @Test
    void shouldThrowDuplicatePending_whenRequestForSameDateExists() {
        // Arrange: ya existe una PENDING del empleado para (PARKING, WITHIN)
        givenActor(EMP_LOGIN, EMP_ID);
        requestRepository.seed(pendingWithId(500L, EMP_ID, WITHIN));

        // Act / Assert
        assertThatThrownBy(() -> newService().create(EMP_LOGIN, new RequestCreateRequest(WITHIN, null)))
                .isInstanceOf(DuplicatePendingRequestException.class);
        assertThat(requestRepository.saves()).isZero();
    }

    @Test
    void shouldThrowNotFound_whenSessionUserNoLongerExists() {
        // Arrange: el principal de la sesion ya no resuelve a un empleado
        given(employeeRepository.findByLogin(EMP_LOGIN)).willReturn(Optional.empty());

        // Act / Assert
        assertThatThrownBy(() -> newService().create(EMP_LOGIN, new RequestCreateRequest(WITHIN, null)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ---- Cancelacion: BOLA + estado ----

    @Test
    void shouldCancelRequest_whenOwnerCancelsPending() {
        // Arrange
        requestRepository.seed(pending());
        givenActor(EMP_LOGIN, EMP_ID);

        // Act
        RequestResponse result = newService().cancel(REQUEST_ID, EMP_LOGIN);

        // Assert
        assertThat(result.status()).isEqualTo(RequestStatus.CANCELLED);
    }

    @Test
    void shouldThrowForbidden_whenCancellingOtherEmployeeRequest() {
        // Arrange: la solicitud es del empleado 15; el solicitante resuelve a 99
        requestRepository.seed(pending());
        givenActor(OTHER_LOGIN, OTHER_ID);

        // Act / Assert (BOLA)
        assertThatThrownBy(() -> newService().cancel(REQUEST_ID, OTHER_LOGIN))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(requestRepository.saves()).isZero();
    }

    @Test
    void shouldCancelApprovedFutureRequest_andRecordRelease() {
        // Arrange: APPROVED propia con fecha futura (WITHIN > TODAY) -> cancelable, libera recurso
        Request approved = pending();
        approved.approve(SPACE_ID, ADMIN_ID, "auto", NOW);
        requestRepository.seed(approved);
        givenActor(EMP_LOGIN, EMP_ID);

        // Act
        RequestResponse result = newService().cancel(REQUEST_ID, EMP_LOGIN);

        // Assert: CANCELLED y auditoria de liberacion registrada con actor, accion y entidad
        assertThat(result.status()).isEqualTo(RequestStatus.CANCELLED);
        verify(auditRecorder).record(auditCaptor.capture());
        AuditEntry entry = auditCaptor.getValue();
        assertThat(entry.actorEmployeeId()).isEqualTo(EMP_ID);
        assertThat(entry.action()).isEqualTo("CANCEL_APPROVED_REQUEST");
        assertThat(entry.entityType()).isEqualTo("Request");
        assertThat(entry.entityId()).isEqualTo(REQUEST_ID);
    }

    @Test
    void shouldNotRecordRelease_whenCancellingPending() {
        // Arrange: cancelar una PENDING no libera recurso -> no se audita liberacion
        requestRepository.seed(pending());
        givenActor(EMP_LOGIN, EMP_ID);

        // Act
        newService().cancel(REQUEST_ID, EMP_LOGIN);

        // Assert
        verifyNoInteractions(auditRecorder);
    }

    @Test
    void shouldThrowState_whenCancellingApprovedPastRequest() {
        // Arrange: APPROVED propia con fecha pasada -> no cancelable (recurso ya transcurrido)
        Request approvedPast = Request.restore(
                REQUEST_ID, EMP_ID, BEFORE, RequestStatus.APPROVED, SPACE_ID, ResourceType.PARKING,
                "auto", null, null, ADMIN_ID, NOW, NOW);
        requestRepository.seed(approvedPast);
        givenActor(EMP_LOGIN, EMP_ID);

        // Act / Assert
        assertThatThrownBy(() -> newService().cancel(REQUEST_ID, EMP_LOGIN))
                .isInstanceOf(RequestStateException.class);
        assertThat(requestRepository.saves()).isZero();
        verifyNoInteractions(auditRecorder);
    }

    @Test
    void shouldThrowState_whenCancellingTerminalRequest() {
        // Arrange: solicitud ya cancelada (estado terminal)
        Request cancelled = pending();
        cancelled.cancel();
        requestRepository.seed(cancelled);
        givenActor(EMP_LOGIN, EMP_ID);

        // Act / Assert
        assertThatThrownBy(() -> newService().cancel(REQUEST_ID, EMP_LOGIN))
                .isInstanceOf(RequestStateException.class);
        assertThat(requestRepository.saves()).isZero();
    }

    @Test
    void shouldThrowNotFound_whenCancellingUnknownRequest() {
        // Arrange (store vacio)

        // Act / Assert
        assertThatThrownBy(() -> newService().cancel(REQUEST_ID, EMP_LOGIN))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ---- Aprobacion: disponibilidad + estado ----

    @Test
    void shouldApproveRequest_whenSpaceAvailable() {
        // Arrange: la disponibilidad consolidada declara la plaza libre para la fecha
        requestRepository.seed(pending());
        given(resourceResolvers.exists(SPACE_ID, ResourceType.PARKING)).willReturn(true);
        given(availabilityService.isSpaceTakenForDate(SPACE_ID, ResourceType.PARKING, WITHIN))
                .willReturn(false);
        givenActor(ADMIN_LOGIN, ADMIN_ID);

        // Act
        RequestResponse result = newService()
                .approve(REQUEST_ID, new RequestApproveRequest(SPACE_ID, "Bienvenido"), ADMIN_LOGIN);

        // Assert: APPROVED con plaza, resolutor, nota
        assertThat(result.status()).isEqualTo(RequestStatus.APPROVED);
        assertThat(result.parkingSpaceId()).isEqualTo(SPACE_ID);
        assertThat(result.resolvedById()).isEqualTo(ADMIN_ID);
        assertThat(result.approvalNote()).isEqualTo("Bienvenido");
        verifyEventPublished(RequestApprovedEvent.class);
    }

    @Test
    void shouldApproveRequest_whenFixedAssignmentIsReleasedForThatDate() {
        // Arrange (issue #43): plaza con asignacion fija liberada esa fecha -> disponible.
        requestRepository.seed(pending());
        given(resourceResolvers.exists(SPACE_ID, ResourceType.PARKING)).willReturn(true);
        given(availabilityService.isSpaceTakenForDate(SPACE_ID, ResourceType.PARKING, WITHIN))
                .willReturn(false);
        givenActor(ADMIN_LOGIN, ADMIN_ID);

        // Act / Assert: aprobable pese a existir una asignacion fija (por estar liberada)
        assertThat(newService()
                .approve(REQUEST_ID, new RequestApproveRequest(SPACE_ID, null), ADMIN_LOGIN).status())
                .isEqualTo(RequestStatus.APPROVED);
    }

    @Test
    void shouldThrowUnavailable_whenSpaceReservedByVisitor() {
        // Arrange (issue #43): la disponibilidad consolidada incluye las reservas de visitante.
        requestRepository.seed(pending());
        given(resourceResolvers.exists(SPACE_ID, ResourceType.PARKING)).willReturn(true);
        given(availabilityService.isSpaceTakenForDate(SPACE_ID, ResourceType.PARKING, WITHIN))
                .willReturn(true);

        // Act / Assert
        assertThatThrownBy(() -> newService()
                .approve(REQUEST_ID, new RequestApproveRequest(SPACE_ID, null), ADMIN_LOGIN))
                .isInstanceOf(SpaceUnavailableException.class);
        assertThat(requestRepository.saves()).isZero();
    }

    @Test
    void shouldThrowUnavailable_whenSpaceTakenByConsolidatedRule() {
        // Arrange: la plaza esta ocupada segun la regla consolidada (fija sin liberar o APPROVED)
        requestRepository.seed(pending());
        given(resourceResolvers.exists(SPACE_ID, ResourceType.PARKING)).willReturn(true);
        given(availabilityService.isSpaceTakenForDate(SPACE_ID, ResourceType.PARKING, WITHIN))
                .willReturn(true);

        // Act / Assert
        assertThatThrownBy(() -> newService()
                .approve(REQUEST_ID, new RequestApproveRequest(SPACE_ID, null), ADMIN_LOGIN))
                .isInstanceOf(SpaceUnavailableException.class);
        assertThat(requestRepository.saves()).isZero();
    }

    @Test
    void shouldThrowNotFound_whenApprovingWithUnknownSpace() {
        // Arrange
        requestRepository.seed(pending());
        given(resourceResolvers.exists(SPACE_ID, ResourceType.PARKING)).willReturn(false);

        // Act / Assert
        assertThatThrownBy(() -> newService()
                .approve(REQUEST_ID, new RequestApproveRequest(SPACE_ID, null), ADMIN_LOGIN))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void shouldThrowState_whenApprovingNonPendingRequest() {
        // Arrange: solicitud ya cancelada
        Request cancelled = pending();
        cancelled.cancel();
        requestRepository.seed(cancelled);

        // Act / Assert
        assertThatThrownBy(() -> newService()
                .approve(REQUEST_ID, new RequestApproveRequest(SPACE_ID, null), ADMIN_LOGIN))
                .isInstanceOf(RequestStateException.class);
    }

    // ---- Rechazo: catalogo + validacion OTHER ----

    @Test
    void shouldRejectRequest_whenReasonCodeFromCatalog() {
        // Arrange
        requestRepository.seed(pending());
        givenActor(ADMIN_LOGIN, ADMIN_ID);

        // Act
        RequestResponse result = newService().reject(
                REQUEST_ID, new RequestRejectRequest(RejectionReasonCode.NO_AVAILABILITY, null), ADMIN_LOGIN);

        // Assert
        assertThat(result.status()).isEqualTo(RequestStatus.REJECTED);
        assertThat(result.rejectionReasonCode()).isEqualTo(RejectionReasonCode.NO_AVAILABILITY);
        assertThat(result.resolvedById()).isEqualTo(ADMIN_ID);
        verifyEventPublished(RequestRejectedEvent.class);
    }

    @Test
    void shouldRejectRequest_whenOtherWithValidFreeText() {
        // Arrange
        requestRepository.seed(pending());
        givenActor(ADMIN_LOGIN, ADMIN_ID);

        // Act
        RequestResponse result = newService().reject(
                REQUEST_ID, new RequestRejectRequest(RejectionReasonCode.OTHER, VALID_REASON), ADMIN_LOGIN);

        // Assert
        assertThat(result.status()).isEqualTo(RequestStatus.REJECTED);
        assertThat(result.rejectionReason()).isEqualTo(VALID_REASON);
    }

    @Test
    void shouldThrowReasonRequired_whenOtherWithoutFreeText() {
        // Arrange
        requestRepository.seed(pending());

        // Act / Assert
        assertThatThrownBy(() -> newService().reject(
                REQUEST_ID, new RequestRejectRequest(RejectionReasonCode.OTHER, null), ADMIN_LOGIN))
                .isInstanceOf(RejectionReasonRequiredException.class);
        assertThat(requestRepository.saves()).isZero();
    }

    @Test
    void shouldThrowReasonRequired_whenOtherWithTooShortFreeText() {
        // Arrange: texto de menos de 5 caracteres
        requestRepository.seed(pending());

        // Act / Assert
        assertThatThrownBy(() -> newService().reject(
                REQUEST_ID, new RequestRejectRequest(RejectionReasonCode.OTHER, "abc"), ADMIN_LOGIN))
                .isInstanceOf(RejectionReasonRequiredException.class);
    }

    @Test
    void shouldRejectApprovedRequest_whenAdminRejectsAfterApproval() {
        // Arrange (change request-auto-assignment §D5): rechazar una APPROVED la libera
        Request approved = pending();
        approved.approve(SPACE_ID, ADMIN_ID, null, NOW);
        requestRepository.seed(approved);
        givenActor(ADMIN_LOGIN, ADMIN_ID);

        // Act
        RequestResponse result = newService().reject(
                REQUEST_ID, new RequestRejectRequest(RejectionReasonCode.OUTSIDE_POLICY, null), ADMIN_LOGIN);

        // Assert: la solicitud queda REJECTED (deja de contar como APPROVED -> recurso libre)
        assertThat(result.status()).isEqualTo(RequestStatus.REJECTED);
        verifyEventPublished(RequestRejectedEvent.class);
    }

    @Test
    void shouldThrowState_whenRejectingTerminalRequest() {
        // Arrange: solicitud ya cancelada (estado terminal, no admite rechazo)
        Request cancelled = pending();
        cancelled.cancel();
        requestRepository.seed(cancelled);

        // Act / Assert
        assertThatThrownBy(() -> newService().reject(
                REQUEST_ID, new RequestRejectRequest(RejectionReasonCode.OUTSIDE_POLICY, null), ADMIN_LOGIN))
                .isInstanceOf(RequestStateException.class);
        assertThat(requestRepository.saves()).isZero();
    }

    // ---- Detalle ----

    @Test
    void shouldReturnRequest_whenGettingExisting() {
        // Arrange
        requestRepository.seed(pending());

        // Act / Assert
        assertThat(newService().get(REQUEST_ID).employeeId()).isEqualTo(EMP_ID);
    }

    @Test
    void shouldThrowNotFound_whenGettingUnknownRequest() {
        // Arrange (store vacio)

        // Act / Assert
        assertThatThrownBy(() -> newService().get(REQUEST_ID))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ---- Listado propio: numero humano del recurso (no el resource_id interno) ----

    @Test
    void shouldExposeParkingNumberAndFloor_whenListingApprovedParkingRequest() {
        // Arrange: una solicitud APPROVED de plaza; el recurso 8 (id interno) es la plaza 3005
        requestRepository.seed(approvedWithResource(REQUEST_ID, ResourceType.PARKING, SPACE_ID));
        givenActor(EMP_LOGIN, EMP_ID);
        BookableResource space = mock(BookableResource.class);
        given(space.getNumber()).willReturn(3005);
        given(space.getFloor()).willReturn(3);
        // lenient: resolveApprovedResources consulta AMBOS tipos; la llamada del tipo hermano
        // (DESK, lista vacia) no debe activar el strict-stubbing sobre este stub de PARKING.
        lenient().when(resourceResolvers.resolveAll(any(), eq(ResourceType.PARKING)))
                .thenReturn(Map.of(SPACE_ID, space));

        // Act
        RequestResponse result = newService()
                .listMine(EMP_LOGIN, null, Pageable.unpaged()).content().get(0);

        // Assert: se muestra el NUMERO real de la plaza (3005) y su planta, no el id interno (8)
        assertThat(result.resourceNumber()).isEqualTo(3005);
        assertThat(result.floor()).isEqualTo(3);
        assertThat(result.parkingSpaceId()).isEqualTo(SPACE_ID);
    }

    @Test
    void shouldExposeDeskNumberWithoutFloor_whenListingApprovedDeskRequest() {
        // Arrange: una solicitud APPROVED de puesto; el recurso 8 es el puesto numero 12
        requestRepository.seed(approvedWithResource(REQUEST_ID, ResourceType.DESK, SPACE_ID));
        givenActor(EMP_LOGIN, EMP_ID);
        BookableResource desk = mock(BookableResource.class);
        given(desk.getNumber()).willReturn(12);
        given(desk.getFloor()).willReturn(null);
        lenient().when(resourceResolvers.resolveAll(any(), eq(ResourceType.DESK)))
                .thenReturn(Map.of(SPACE_ID, desk));

        // Act
        RequestResponse result = newService()
                .listMine(EMP_LOGIN, null, Pageable.unpaged()).content().get(0);

        // Assert: numero del puesto sin planta (los puestos no tienen planta derivada)
        assertThat(result.resourceNumber()).isEqualTo(12);
        assertThat(result.floor()).isNull();
    }

    @Test
    void shouldNotExposeResourceNumber_whenRequestNotApproved() {
        // Arrange: una PENDING no tiene recurso asignado -> no se resuelve numero
        requestRepository.seed(pending());
        givenActor(EMP_LOGIN, EMP_ID);

        // Act
        RequestResponse result = newService()
                .listMine(EMP_LOGIN, null, Pageable.unpaged()).content().get(0);

        // Assert: sin numero (el frontend degrada a "—")
        assertThat(result.resourceNumber()).isNull();
        assertThat(result.floor()).isNull();
    }

    @Test
    void shouldNotExposeResourceNumber_whenApprovedResourceNoLongerResolves() {
        // Arrange: APPROVED con recurso que ya no se resuelve (mapa vacio) -> degrada a null
        requestRepository.seed(approvedWithResource(REQUEST_ID, ResourceType.PARKING, SPACE_ID));
        givenActor(EMP_LOGIN, EMP_ID);
        lenient().when(resourceResolvers.resolveAll(any(), eq(ResourceType.PARKING)))
                .thenReturn(Map.of());

        // Act
        RequestResponse result = newService()
                .listMine(EMP_LOGIN, null, Pageable.unpaged()).content().get(0);

        // Assert
        assertThat(result.resourceNumber()).isNull();
    }

    // ---- Listado admin por estado (aprobadas / rechazadas / todas) ----

    @Test
    void shouldListOnlyApproved_whenAdminListsByApprovedStatus() {
        // Arrange: una APPROVED, una PENDING y una REJECTED en el store
        requestRepository.seed(approvedWithResource(601L, ResourceType.PARKING, SPACE_ID));
        requestRepository.seed(pendingWithId(602L, EMP_ID, WITHIN));
        requestRepository.seed(rejectedWithId(603L));
        lenient().when(resourceResolvers.resolveAll(any(), eq(ResourceType.PARKING))).thenReturn(Map.of());

        // Act / Assert: solo la solicitud APPROVED
        assertThat(newService().listByStatus(RequestStatus.APPROVED, Pageable.unpaged()).content())
                .singleElement()
                .satisfies(r -> assertThat(r.status()).isEqualTo(RequestStatus.APPROVED));
    }

    @Test
    void shouldListAllStatuses_whenAdminListsWithoutStatusFilter() {
        // Arrange: tres solicitudes de estados distintos
        requestRepository.seed(approvedWithResource(601L, ResourceType.PARKING, SPACE_ID));
        requestRepository.seed(pendingWithId(602L, EMP_ID, WITHIN));
        requestRepository.seed(rejectedWithId(603L));
        lenient().when(resourceResolvers.resolveAll(any(), eq(ResourceType.PARKING))).thenReturn(Map.of());

        // Act / Assert: status nulo devuelve todas
        assertThat(newService().listByStatus(null, Pageable.unpaged()).content()).hasSize(3);
    }

    @Test
    void shouldExposeApprovedResourceNumber_whenAdminListsApproved() {
        // Arrange: la APPROVED (recurso interno 8) es la plaza 3005 planta 3
        requestRepository.seed(approvedWithResource(601L, ResourceType.PARKING, SPACE_ID));
        BookableResource space = mock(BookableResource.class);
        given(space.getNumber()).willReturn(3005);
        given(space.getFloor()).willReturn(3);
        lenient().when(resourceResolvers.resolveAll(any(), eq(ResourceType.PARKING)))
                .thenReturn(Map.of(SPACE_ID, space));

        // Act
        RequestResponse result = newService()
                .listByStatus(RequestStatus.APPROVED, Pageable.unpaged()).content().get(0);

        // Assert: numero humano de la plaza (3005) y planta, no el id interno (8)
        assertThat(result.resourceNumber()).isEqualTo(3005);
        assertThat(result.floor()).isEqualTo(3);
    }

    private Request pending() {
        return pendingWithId(REQUEST_ID, EMP_ID, WITHIN);
    }

    private static Request rejectedWithId(Long id) {
        return Request.restore(
                id, EMP_ID, WITHIN, RequestStatus.REJECTED, null, ResourceType.PARKING,
                null, RejectionReasonCode.NO_AVAILABILITY, null, ADMIN_ID, NOW, NOW);
    }

    private static Request approvedWithResource(Long id, ResourceType type, Long resourceId) {
        return Request.restore(
                id, EMP_ID, WITHIN, RequestStatus.APPROVED, resourceId, type,
                "nota", null, null, ADMIN_ID, NOW, NOW);
    }

    private static Request pendingWithId(Long id, Long employeeId, LocalDate date) {
        return Request.restore(
                id, employeeId, date, RequestStatus.PENDING, null, ResourceType.PARKING,
                null, null, null, null, null, NOW);
    }

    private void givenActor(String login, Long id) {
        Employee actor = mock(Employee.class);
        given(actor.getId()).willReturn(id);
        given(employeeRepository.findByLogin(login)).willReturn(Optional.of(actor));
    }

    private void verifyEventPublished(Class<?> eventType) {
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(eventType);
    }

    /**
     * Fake in-memory del puerto de persistencia de solicitudes: sustituye a un mock de Spring
     * Data en los tests unitarios del servicio. {@link #seed(Request)} precarga estado sin
     * contar como escritura; {@link #save}/{@link #saveAndFlush} asignan id (si falta) y cuentan
     * la invocacion para verificar los caminos que NO deben persistir.
     */
    private static final class InMemoryRequestRepository implements RequestRepositoryPort {

        private final Map<Long, Request> store = new HashMap<>();
        private long sequence = 1000L;
        private int saves;

        void seed(Request request) {
            store.put(request.getId(), request);
        }

        int saves() {
            return saves;
        }

        @Override
        public Optional<Request> findById(Long id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Request save(Request request) {
            return persist(request);
        }

        @Override
        public Request saveAndFlush(Request request) {
            return persist(request);
        }

        private Request persist(Request request) {
            saves++;
            Long id = request.getId() != null ? request.getId() : ++sequence;
            Request stored = Request.restore(
                    id, request.getEmployeeId(), request.getRequestedDate(), request.getStatus(),
                    request.getResourceId(), request.getResourceType(), request.getApprovalNote(),
                    request.getRejectionReasonCode(), request.getRejectionReason(),
                    request.getResolvedById(), request.getResolvedAt(), request.getCreatedAt());
            store.put(id, stored);
            return stored;
        }

        @Override
        public boolean existsByEmployeeIdAndResourceTypeAndRequestedDateAndStatus(
                Long employeeId, ResourceType resourceType, LocalDate requestedDate, RequestStatus status) {
            return store.values().stream().anyMatch(r ->
                    employeeId.equals(r.getEmployeeId())
                            && resourceType == r.getResourceType()
                            && requestedDate.equals(r.getRequestedDate())
                            && status == r.getStatus());
        }

        @Override
        public Page<Request> findByEmployeeId(Long employeeId, Pageable pageable) {
            return new PageImpl<>(store.values().stream()
                    .filter(r -> employeeId.equals(r.getEmployeeId()))
                    .toList());
        }

        @Override
        public Page<Request> findByEmployeeIdAndStatus(
                Long employeeId, RequestStatus status, Pageable pageable) {
            return new PageImpl<>(store.values().stream()
                    .filter(r -> employeeId.equals(r.getEmployeeId()) && status == r.getStatus())
                    .toList());
        }

        @Override
        public Page<Request> findByStatusOrderByCreatedAtAsc(RequestStatus status, Pageable pageable) {
            return new PageImpl<>(store.values().stream()
                    .filter(r -> status == r.getStatus())
                    .sorted(Comparator.comparing(Request::getCreatedAt))
                    .collect(java.util.stream.Collectors.toList()));
        }

        @Override
        public Page<Request> findByStatusOrderByCreatedAtDesc(RequestStatus status, Pageable pageable) {
            return new PageImpl<>(store.values().stream()
                    .filter(r -> status == r.getStatus())
                    .sorted(Comparator.comparing(Request::getCreatedAt).reversed())
                    .collect(java.util.stream.Collectors.toList()));
        }

        @Override
        public Page<Request> findAllByOrderByCreatedAtDesc(Pageable pageable) {
            return new PageImpl<>(store.values().stream()
                    .sorted(Comparator.comparing(Request::getCreatedAt).reversed())
                    .collect(java.util.stream.Collectors.toList()));
        }
    }
}
