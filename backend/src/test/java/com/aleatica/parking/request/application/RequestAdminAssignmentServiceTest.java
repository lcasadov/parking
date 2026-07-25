package com.aleatica.parking.request.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.aleatica.parking.audit.AuditEntry;
import com.aleatica.parking.audit.AuditRecorder;
import com.aleatica.parking.auth.domain.ClockPort;
import com.aleatica.parking.availability.application.AvailabilityService;
import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeCategory;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.notification.event.RequestAdminAssignedEvent;
import com.aleatica.parking.parkingspace.ParkingSpace;
import com.aleatica.parking.request.domain.Request;
import com.aleatica.parking.request.domain.RequestRepositoryPort;
import com.aleatica.parking.request.domain.RequestStatus;
import com.aleatica.parking.request.dto.RequestAdminAssignRequest;
import com.aleatica.parking.request.dto.RequestResponse;
import com.aleatica.parking.resource.ResourceResolvers;
import com.aleatica.parking.resource.ResourceType;
import com.aleatica.parking.systemsettings.application.SystemSettingsService;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.time.LocalDate;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/**
 * Tests unitarios de {@link RequestService#adminAssign(String, RequestAdminAssignRequest)}
 * (change {@code restructure-admin-workflows}, capability {@code admin-punctual-assignment}):
 * caminos felices con plaza/puesto elegido, auto-asignacion de plaza, 409 por recurso ocupado,
 * 409 {@code NO_AVAILABILITY} en auto-asignacion, 400 cuando falta el puesto en {@code DESK}, y
 * la traza de auditoria con el admin como actor. Usa un fake in-memory del puerto y mockea el
 * resto de colaboradores; no toca la base de datos.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RequestAdminAssignmentServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-04T10:00:00Z");
    private static final LocalDate DATE = LocalDate.of(2026, 7, 10);
    private static final LocalDate PAST_DATE = LocalDate.of(2026, 7, 3);

    private static final String ADMIN_LOGIN = "admin";
    private static final Long ADMIN_ID = 1L;
    private static final String EMP_LOGIN = "employee";
    private static final Long EMP_ID = 15L;
    private static final Long SPACE_ID = 8L;
    private static final Long DESK_ID = 55L;
    private static final Long UNKNOWN_EMPLOYEE_ID = 999L;

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

    // ---- Camino feliz: plaza elegida ----

    @Test
    void shouldAssignParkingApproved_whenResourceIdChosenAndAvailable() {
        // Arrange
        givenAdmin();
        givenTargetEmployee(EMP_ID, EmployeeCategory.EMPLEADO);
        given(resourceResolvers.exists(SPACE_ID, ResourceType.PARKING)).willReturn(true);
        given(availabilityService.isSpaceTakenForDate(SPACE_ID, ResourceType.PARKING, DATE))
                .willReturn(false);

        // Act
        RequestResponse result = newService().adminAssign(
                ADMIN_LOGIN, new RequestAdminAssignRequest(EMP_ID, DATE, ResourceType.PARKING, SPACE_ID));

        // Assert: nace APPROVED con el admin como resolutor y la nota de asignacion puntual
        assertThat(result.status()).isEqualTo(RequestStatus.APPROVED);
        assertThat(result.parkingSpaceId()).isEqualTo(SPACE_ID);
        assertThat(result.resolvedById()).isEqualTo(ADMIN_ID);
        assertThat(result.approvalNote()).isEqualTo(Request.ADMIN_ASSIGNMENT_NOTE);
        assertThat(result.employeeId()).isEqualTo(EMP_ID);
        verifyEventPublished(RequestAdminAssignedEvent.class);
    }

    // ---- Camino feliz: puesto elegido ----

    @Test
    void shouldAssignDeskApproved_whenResourceIdChosenAndAvailable() {
        // Arrange
        givenAdmin();
        givenTargetEmployee(EMP_ID, EmployeeCategory.EMPLEADO);
        given(resourceResolvers.exists(DESK_ID, ResourceType.DESK)).willReturn(true);
        given(availabilityService.isSpaceTakenForDate(DESK_ID, ResourceType.DESK, DATE))
                .willReturn(false);

        // Act
        RequestResponse result = newService().adminAssign(
                ADMIN_LOGIN, new RequestAdminAssignRequest(EMP_ID, DATE, ResourceType.DESK, DESK_ID));

        // Assert
        assertThat(result.status()).isEqualTo(RequestStatus.APPROVED);
        assertThat(result.parkingSpaceId()).isEqualTo(DESK_ID);
        assertThat(result.resourceType()).isEqualTo(ResourceType.DESK);
        verifyEventPublished(RequestAdminAssignedEvent.class);
    }

    // ---- Auto-asignacion (PARKING sin resourceId) ----

    @Test
    void shouldAutoAssignParking_whenResourceIdOmitted() {
        // Arrange
        givenAdmin();
        givenTargetEmployee(EMP_ID, EmployeeCategory.EMPLEADO);
        ParkingSpace assigned = mock(ParkingSpace.class);
        given(assigned.getId()).willReturn(SPACE_ID);
        given(assigned.getNumber()).willReturn(5001);
        given(assigned.floor()).willReturn(5);
        given(availabilityService.freeParkingSpacesForDate(DATE)).willReturn(List.of(assigned));

        // Act
        RequestResponse result = newService().adminAssign(
                ADMIN_LOGIN, new RequestAdminAssignRequest(EMP_ID, DATE, null, null));

        // Assert: auto-asigna la plaza libre y nace APPROVED
        assertThat(result.status()).isEqualTo(RequestStatus.APPROVED);
        assertThat(result.parkingSpaceId()).isEqualTo(SPACE_ID);
        assertThat(result.resourceType()).isEqualTo(ResourceType.PARKING);
        verifyEventPublished(RequestAdminAssignedEvent.class);
    }

    @Test
    void shouldThrowNoAvailability_whenAutoAssignAndNoFreeSpace() {
        // Arrange
        givenAdmin();
        givenTargetEmployee(EMP_ID, EmployeeCategory.EMPLEADO);
        given(availabilityService.freeParkingSpacesForDate(DATE)).willReturn(List.of());

        // Act / Assert: 409 NO_AVAILABILITY y no se crea la asignacion
        assertThatThrownBy(() -> newService().adminAssign(
                ADMIN_LOGIN, new RequestAdminAssignRequest(EMP_ID, DATE, ResourceType.PARKING, null)))
                .isInstanceOf(NoAvailabilityException.class);
        assertThat(requestRepository.saves()).isZero();
        verifyNoInteractions(auditRecorder);
    }

    // ---- 409 recurso ya ocupado ----

    @Test
    void shouldThrowUnavailable_whenChosenResourceAlreadyOccupied() {
        // Arrange
        givenAdmin();
        givenTargetEmployee(EMP_ID, EmployeeCategory.EMPLEADO);
        given(resourceResolvers.exists(SPACE_ID, ResourceType.PARKING)).willReturn(true);
        given(availabilityService.isSpaceTakenForDate(SPACE_ID, ResourceType.PARKING, DATE))
                .willReturn(true);

        // Act / Assert
        assertThatThrownBy(() -> newService().adminAssign(
                ADMIN_LOGIN, new RequestAdminAssignRequest(EMP_ID, DATE, ResourceType.PARKING, SPACE_ID)))
                .isInstanceOf(SpaceUnavailableException.class);
        assertThat(requestRepository.saves()).isZero();
        verifyNoInteractions(auditRecorder);
    }

    @Test
    void shouldThrowNotFound_whenChosenResourceDoesNotExist() {
        // Arrange
        givenAdmin();
        givenTargetEmployee(EMP_ID, EmployeeCategory.EMPLEADO);
        given(resourceResolvers.exists(SPACE_ID, ResourceType.PARKING)).willReturn(false);

        // Act / Assert
        assertThatThrownBy(() -> newService().adminAssign(
                ADMIN_LOGIN, new RequestAdminAssignRequest(EMP_ID, DATE, ResourceType.PARKING, SPACE_ID)))
                .isInstanceOf(EntityNotFoundException.class);
        assertThat(requestRepository.saves()).isZero();
    }

    // ---- DESK sin resourceId: no hay auto-asignacion de puestos ----

    @Test
    void shouldThrowResourceSelectionRequired_whenDeskWithoutResourceId() {
        // Arrange
        givenAdmin();
        givenTargetEmployee(EMP_ID, EmployeeCategory.EMPLEADO);

        // Act / Assert
        assertThatThrownBy(() -> newService().adminAssign(
                ADMIN_LOGIN, new RequestAdminAssignRequest(EMP_ID, DATE, ResourceType.DESK, null)))
                .isInstanceOf(ResourceSelectionRequiredException.class);
        assertThat(requestRepository.saves()).isZero();
        verifyNoInteractions(auditRecorder);
    }

    // ---- Empleado destino inexistente ----

    @Test
    void shouldThrowNotFound_whenTargetEmployeeDoesNotExist() {
        // Arrange
        givenAdmin();
        given(employeeRepository.findById(UNKNOWN_EMPLOYEE_ID)).willReturn(Optional.empty());

        // Act / Assert
        assertThatThrownBy(() -> newService().adminAssign(
                ADMIN_LOGIN, new RequestAdminAssignRequest(UNKNOWN_EMPLOYEE_ID, DATE, ResourceType.PARKING, SPACE_ID)))
                .isInstanceOf(EntityNotFoundException.class);
        assertThat(requestRepository.saves()).isZero();
    }

    // ---- Fecha pasada ----

    @Test
    void shouldThrowOutsideWindow_whenDateIsInThePast() {
        // Arrange
        givenAdmin();
        givenTargetEmployee(EMP_ID, EmployeeCategory.EMPLEADO);

        // Act / Assert
        assertThatThrownBy(() -> newService().adminAssign(
                ADMIN_LOGIN, new RequestAdminAssignRequest(EMP_ID, PAST_DATE, ResourceType.PARKING, SPACE_ID)))
                .isInstanceOf(OutsideRequestWindowException.class);
        assertThat(requestRepository.saves()).isZero();
    }

    // ---- Auditoria ----

    @Test
    void shouldRecordAudit_withAdminAsActorAndTargetEmployeeInDetails() {
        // Arrange
        givenAdmin();
        givenTargetEmployee(EMP_ID, EmployeeCategory.EMPLEADO);
        given(resourceResolvers.exists(SPACE_ID, ResourceType.PARKING)).willReturn(true);
        given(availabilityService.isSpaceTakenForDate(SPACE_ID, ResourceType.PARKING, DATE))
                .willReturn(false);

        // Act
        RequestResponse result = newService().adminAssign(
                ADMIN_LOGIN, new RequestAdminAssignRequest(EMP_ID, DATE, ResourceType.PARKING, SPACE_ID));

        // Assert: el admin actuante es el actor; el detalle referencia al empleado destino
        verify(auditRecorder).record(auditCaptor.capture());
        AuditEntry entry = auditCaptor.getValue();
        assertThat(entry.actorEmployeeId()).isEqualTo(ADMIN_ID);
        assertThat(entry.action()).isEqualTo("ADMIN_PUNCTUAL_ASSIGNMENT");
        assertThat(entry.entityType()).isEqualTo("Request");
        assertThat(entry.entityId()).isEqualTo(result.id());
        assertThat(entry.details()).contains("\"employeeId\":" + EMP_ID);
    }

    // ---- Helpers ----

    private void givenAdmin() {
        Employee admin = mock(Employee.class);
        given(admin.getId()).willReturn(ADMIN_ID);
        given(employeeRepository.findByLogin(ADMIN_LOGIN)).willReturn(Optional.of(admin));
    }

    private void givenTargetEmployee(Long id, EmployeeCategory category) {
        Employee employee = mock(Employee.class);
        given(employee.getId()).willReturn(id);
        given(employee.getCategory()).willReturn(category);
        given(employeeRepository.findById(id)).willReturn(Optional.of(employee));
    }

    private void verifyEventPublished(Class<?> eventType) {
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(eventType);
    }

    /**
     * Fake in-memory del puerto de persistencia de solicitudes (mismo patron que
     * {@code RequestServiceTest}/{@code RequestAutoAssignmentServiceTest}): {@link #seed(Request)}
     * precarga sin contar como escritura; {@code save}/{@code saveAndFlush} cuentan la invocacion.
     */
    private static final class InMemoryRequestRepository implements RequestRepositoryPort {

        private final Map<Long, Request> store = new HashMap<>();
        private long sequence = 1000L;
        private int saves;

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
                    request.getResolvedById(), request.getResolvedAt(), request.getCreatedAt(),
                    request.getLastRemindedAt(), request.isWaitlisted());
            store.put(id, stored);
            return stored;
        }

        @Override
        public boolean existsByEmployeeIdAndResourceTypeAndRequestedDateAndStatus(
                Long employeeId, ResourceType resourceType, LocalDate requestedDate, RequestStatus status) {
            return false;
        }

        @Override
        public Page<Request> findByEmployeeId(Long employeeId, Pageable pageable) {
            return new PageImpl<>(List.of());
        }

        @Override
        public Page<Request> findByEmployeeIdAndStatus(
                Long employeeId, RequestStatus status, Pageable pageable) {
            return new PageImpl<>(List.of());
        }

        @Override
        public Page<Request> findByStatusOrderByCreatedAtAsc(RequestStatus status, Pageable pageable) {
            return new PageImpl<>(List.of());
        }

        @Override
        public Page<Request> findByStatusOrderByCreatedAtDesc(RequestStatus status, Pageable pageable) {
            return new PageImpl<>(List.of());
        }

        @Override
        public Page<Request> findAllByOrderByCreatedAtDesc(Pageable pageable) {
            return new PageImpl<>(List.of());
        }

        @Override
        public List<Request> findByStatusAndWaitlistedTrueAndResourceTypeAndRequestedDateOrderByCreatedAtAsc(
                RequestStatus status, ResourceType resourceType, LocalDate requestedDate) {
            return List.of();
        }
    }
}
