package com.aleatica.parking.request.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.aleatica.parking.auth.domain.ClockPort;
import com.aleatica.parking.availability.application.AvailabilityService;
import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.notification.event.RequestApprovedEvent;
import com.aleatica.parking.notification.event.RequestCreatedEvent;
import com.aleatica.parking.notification.event.RequestRejectedEvent;
import com.aleatica.parking.request.RejectionReasonCode;
import com.aleatica.parking.request.Request;
import com.aleatica.parking.request.RequestRepository;
import com.aleatica.parking.request.RequestStatus;
import com.aleatica.parking.request.dto.RequestApproveRequest;
import com.aleatica.parking.request.dto.RequestCreateRequest;
import com.aleatica.parking.request.dto.RequestRejectRequest;
import com.aleatica.parking.request.dto.RequestResponse;
import com.aleatica.parking.resource.ResourceResolvers;
import com.aleatica.parking.resource.ResourceType;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

/**
 * Tests unitarios de {@link RequestService} con repositorios y publicador de eventos
 * mockeados: ventana temporal, unicidad {@code PENDING}, maquina de estados,
 * verificacion de pertenencia (BOLA), disponibilidad al aprobar y validacion del
 * catalogo de rechazo. No toca la base de datos.
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

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ResourceResolvers resourceResolvers;

    @Mock
    private AvailabilityService availabilityService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<Request> savedCaptor;

    @Captor
    private ArgumentCaptor<Object> eventCaptor;

    private final ClockPort clock = () -> NOW;

    private RequestService newService() {
        return new RequestService(
                requestRepository, employeeRepository, resourceResolvers,
                availabilityService, eventPublisher, clock);
    }

    // ---- Creacion: ventana + unicidad ----

    @Test
    void shouldCreatePendingRequest_whenDateWithinWindow() {
        // Arrange
        givenActor(EMP_LOGIN, EMP_ID);
        given(requestRepository.existsByEmployeeIdAndResourceTypeAndRequestedDateAndStatus(
                EMP_ID, ResourceType.PARKING,WITHIN, RequestStatus.PENDING)).willReturn(false);
        given(requestRepository.saveAndFlush(any(Request.class)))
                .willAnswer(inv -> inv.getArgument(0));

        // Act
        RequestResponse result = newService().create(EMP_LOGIN, new RequestCreateRequest(WITHIN, null));

        // Assert: estado inicial PENDING, sin plaza, con la fecha y el empleado
        assertThat(result.status()).isEqualTo(RequestStatus.PENDING);
        assertThat(result.parkingSpaceId()).isNull();
        assertThat(result.requestedDate()).isEqualTo(WITHIN);
        verify(requestRepository).saveAndFlush(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getCreatedAt()).isEqualTo(NOW);
        verifyEventPublished(RequestCreatedEvent.class);
    }

    @Test
    void shouldCreatePendingRequest_whenDateExactlyToday() {
        // Arrange (frontera inferior inclusive)
        givenActor(EMP_LOGIN, EMP_ID);
        given(requestRepository.existsByEmployeeIdAndResourceTypeAndRequestedDateAndStatus(
                EMP_ID, ResourceType.PARKING,TODAY, RequestStatus.PENDING)).willReturn(false);
        given(requestRepository.saveAndFlush(any(Request.class)))
                .willAnswer(inv -> inv.getArgument(0));

        // Act / Assert
        assertThat(newService().create(EMP_LOGIN, new RequestCreateRequest(TODAY, null)).status())
                .isEqualTo(RequestStatus.PENDING);
    }

    @Test
    void shouldCreatePendingRequest_whenDateExactlyMaxDay() {
        // Arrange (frontera superior inclusive: hoy+14)
        givenActor(EMP_LOGIN, EMP_ID);
        given(requestRepository.existsByEmployeeIdAndResourceTypeAndRequestedDateAndStatus(
                EMP_ID, ResourceType.PARKING,MAX_DAY, RequestStatus.PENDING)).willReturn(false);
        given(requestRepository.saveAndFlush(any(Request.class)))
                .willAnswer(inv -> inv.getArgument(0));

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
        verify(requestRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldThrowOutsideWindow_whenDateAfterMaxDay() {
        // Arrange
        givenActor(EMP_LOGIN, EMP_ID);

        // Act / Assert
        assertThatThrownBy(() -> newService().create(EMP_LOGIN, new RequestCreateRequest(AFTER, null)))
                .isInstanceOf(OutsideRequestWindowException.class);
        verify(requestRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldThrowDuplicatePending_whenRequestForSameDateExists() {
        // Arrange
        givenActor(EMP_LOGIN, EMP_ID);
        given(requestRepository.existsByEmployeeIdAndResourceTypeAndRequestedDateAndStatus(
                EMP_ID, ResourceType.PARKING,WITHIN, RequestStatus.PENDING)).willReturn(true);

        // Act / Assert
        assertThatThrownBy(() -> newService().create(EMP_LOGIN, new RequestCreateRequest(WITHIN, null)))
                .isInstanceOf(DuplicatePendingRequestException.class);
        verify(requestRepository, never()).saveAndFlush(any());
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
        Request pending = pending();
        given(requestRepository.findById(REQUEST_ID)).willReturn(Optional.of(pending));
        givenActor(EMP_LOGIN, EMP_ID);
        given(requestRepository.save(any(Request.class))).willAnswer(inv -> inv.getArgument(0));

        // Act
        RequestResponse result = newService().cancel(REQUEST_ID, EMP_LOGIN);

        // Assert
        assertThat(result.status()).isEqualTo(RequestStatus.CANCELLED);
    }

    @Test
    void shouldThrowForbidden_whenCancellingOtherEmployeeRequest() {
        // Arrange: la solicitud es del empleado 15; el solicitante resuelve a 99
        given(requestRepository.findById(REQUEST_ID)).willReturn(Optional.of(pending()));
        givenActor(OTHER_LOGIN, OTHER_ID);

        // Act / Assert (BOLA)
        assertThatThrownBy(() -> newService().cancel(REQUEST_ID, OTHER_LOGIN))
                .isInstanceOf(AccessDeniedException.class);
        verify(requestRepository, never()).save(any());
    }

    @Test
    void shouldThrowState_whenCancellingResolvedRequest() {
        // Arrange: solicitud ya aprobada (estado terminal)
        Request approved = pending();
        approved.approve(SPACE_ID, ADMIN_ID, null, NOW);
        given(requestRepository.findById(REQUEST_ID)).willReturn(Optional.of(approved));
        givenActor(EMP_LOGIN, EMP_ID);

        // Act / Assert
        assertThatThrownBy(() -> newService().cancel(REQUEST_ID, EMP_LOGIN))
                .isInstanceOf(RequestStateException.class);
        verify(requestRepository, never()).save(any());
    }

    @Test
    void shouldThrowNotFound_whenCancellingUnknownRequest() {
        // Arrange
        given(requestRepository.findById(REQUEST_ID)).willReturn(Optional.empty());

        // Act / Assert
        assertThatThrownBy(() -> newService().cancel(REQUEST_ID, EMP_LOGIN))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ---- Aprobacion: disponibilidad + estado ----

    @Test
    void shouldApproveRequest_whenSpaceAvailable() {
        // Arrange: la disponibilidad consolidada declara la plaza libre para la fecha
        Request pending = pending();
        given(requestRepository.findById(REQUEST_ID)).willReturn(Optional.of(pending));
        given(resourceResolvers.exists(SPACE_ID, ResourceType.PARKING)).willReturn(true);
        given(availabilityService.isSpaceTakenForDate(SPACE_ID, ResourceType.PARKING, WITHIN))
                .willReturn(false);
        givenActor(ADMIN_LOGIN, ADMIN_ID);
        given(requestRepository.saveAndFlush(any(Request.class)))
                .willAnswer(inv -> inv.getArgument(0));

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
        // La disponibilidad consolidada aplica el release y la declara NO ocupada.
        Request pending = pending();
        given(requestRepository.findById(REQUEST_ID)).willReturn(Optional.of(pending));
        given(resourceResolvers.exists(SPACE_ID, ResourceType.PARKING)).willReturn(true);
        given(availabilityService.isSpaceTakenForDate(SPACE_ID, ResourceType.PARKING, WITHIN))
                .willReturn(false);
        givenActor(ADMIN_LOGIN, ADMIN_ID);
        given(requestRepository.saveAndFlush(any(Request.class)))
                .willAnswer(inv -> inv.getArgument(0));

        // Act / Assert: aprobable pese a existir una asignacion fija (por estar liberada)
        assertThat(newService()
                .approve(REQUEST_ID, new RequestApproveRequest(SPACE_ID, null), ADMIN_LOGIN).status())
                .isEqualTo(RequestStatus.APPROVED);
    }

    @Test
    void shouldThrowUnavailable_whenSpaceReservedByVisitor() {
        // Arrange (issue #43): la disponibilidad consolidada incluye las reservas de visitante;
        // aprobar sobre una plaza ya reservada por un visitante debe rechazarse (no doble reserva).
        Request pending = pending();
        given(requestRepository.findById(REQUEST_ID)).willReturn(Optional.of(pending));
        given(resourceResolvers.exists(SPACE_ID, ResourceType.PARKING)).willReturn(true);
        given(availabilityService.isSpaceTakenForDate(SPACE_ID, ResourceType.PARKING, WITHIN))
                .willReturn(true);

        // Act / Assert
        assertThatThrownBy(() -> newService()
                .approve(REQUEST_ID, new RequestApproveRequest(SPACE_ID, null), ADMIN_LOGIN))
                .isInstanceOf(SpaceUnavailableException.class);
        verify(requestRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldThrowUnavailable_whenSpaceTakenByConsolidatedRule() {
        // Arrange: la plaza esta ocupada segun la regla consolidada (fija sin liberar o APPROVED)
        Request pending = pending();
        given(requestRepository.findById(REQUEST_ID)).willReturn(Optional.of(pending));
        given(resourceResolvers.exists(SPACE_ID, ResourceType.PARKING)).willReturn(true);
        given(availabilityService.isSpaceTakenForDate(SPACE_ID, ResourceType.PARKING, WITHIN))
                .willReturn(true);

        // Act / Assert
        assertThatThrownBy(() -> newService()
                .approve(REQUEST_ID, new RequestApproveRequest(SPACE_ID, null), ADMIN_LOGIN))
                .isInstanceOf(SpaceUnavailableException.class);
        verify(requestRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldThrowNotFound_whenApprovingWithUnknownSpace() {
        // Arrange
        given(requestRepository.findById(REQUEST_ID)).willReturn(Optional.of(pending()));
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
        given(requestRepository.findById(REQUEST_ID)).willReturn(Optional.of(cancelled));

        // Act / Assert
        assertThatThrownBy(() -> newService()
                .approve(REQUEST_ID, new RequestApproveRequest(SPACE_ID, null), ADMIN_LOGIN))
                .isInstanceOf(RequestStateException.class);
    }

    // ---- Rechazo: catalogo + validacion OTHER ----

    @Test
    void shouldRejectRequest_whenReasonCodeFromCatalog() {
        // Arrange
        Request pending = pending();
        given(requestRepository.findById(REQUEST_ID)).willReturn(Optional.of(pending));
        givenActor(ADMIN_LOGIN, ADMIN_ID);
        given(requestRepository.save(any(Request.class))).willAnswer(inv -> inv.getArgument(0));

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
        Request pending = pending();
        given(requestRepository.findById(REQUEST_ID)).willReturn(Optional.of(pending));
        givenActor(ADMIN_LOGIN, ADMIN_ID);
        given(requestRepository.save(any(Request.class))).willAnswer(inv -> inv.getArgument(0));

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
        given(requestRepository.findById(REQUEST_ID)).willReturn(Optional.of(pending()));

        // Act / Assert
        assertThatThrownBy(() -> newService().reject(
                REQUEST_ID, new RequestRejectRequest(RejectionReasonCode.OTHER, null), ADMIN_LOGIN))
                .isInstanceOf(RejectionReasonRequiredException.class);
        verify(requestRepository, never()).save(any());
    }

    @Test
    void shouldThrowReasonRequired_whenOtherWithTooShortFreeText() {
        // Arrange: texto de menos de 5 caracteres
        given(requestRepository.findById(REQUEST_ID)).willReturn(Optional.of(pending()));

        // Act / Assert
        assertThatThrownBy(() -> newService().reject(
                REQUEST_ID, new RequestRejectRequest(RejectionReasonCode.OTHER, "abc"), ADMIN_LOGIN))
                .isInstanceOf(RejectionReasonRequiredException.class);
    }

    @Test
    void shouldThrowState_whenRejectingNonPendingRequest() {
        // Arrange
        Request approved = pending();
        approved.approve(SPACE_ID, ADMIN_ID, null, NOW);
        given(requestRepository.findById(REQUEST_ID)).willReturn(Optional.of(approved));

        // Act / Assert
        assertThatThrownBy(() -> newService().reject(
                REQUEST_ID, new RequestRejectRequest(RejectionReasonCode.OUTSIDE_POLICY, null), ADMIN_LOGIN))
                .isInstanceOf(RequestStateException.class);
    }

    // ---- Detalle ----

    @Test
    void shouldReturnRequest_whenGettingExisting() {
        // Arrange
        given(requestRepository.findById(REQUEST_ID)).willReturn(Optional.of(pending()));

        // Act / Assert
        assertThat(newService().get(REQUEST_ID).employeeId()).isEqualTo(EMP_ID);
    }

    @Test
    void shouldThrowNotFound_whenGettingUnknownRequest() {
        // Arrange
        given(requestRepository.findById(REQUEST_ID)).willReturn(Optional.empty());

        // Act / Assert
        assertThatThrownBy(() -> newService().get(REQUEST_ID))
                .isInstanceOf(EntityNotFoundException.class);
    }

    private Request pending() {
        return Request.create(EMP_ID, WITHIN, NOW);
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
}
