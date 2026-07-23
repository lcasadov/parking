package com.aleatica.parking.floorplan.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.aleatica.parking.auth.domain.ClockPort;
import com.aleatica.parking.availability.application.AvailabilityService;
import com.aleatica.parking.desk.Desk;
import com.aleatica.parking.desk.DeskRepository;
import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.floorplan.FloorPlanDeskState;
import com.aleatica.parking.floorplan.dto.DeskRequestResponse;
import com.aleatica.parking.notification.event.RequestApprovedEvent;
import com.aleatica.parking.notification.event.RequestCreatedEvent;
import com.aleatica.parking.request.infrastructure.RequestEntity;
import com.aleatica.parking.request.infrastructure.RequestJpaRepository;
import com.aleatica.parking.request.domain.RequestStatus;
import com.aleatica.parking.request.application.DuplicatePendingRequestException;
import com.aleatica.parking.request.application.OutsideRequestWindowException;
import com.aleatica.parking.request.application.SpaceUnavailableException;
import com.aleatica.parking.resource.ResourceType;
import com.aleatica.parking.systemsettings.application.SystemSettingsService;
import com.aleatica.parking.systemsettings.domain.ApprovalMode;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Tests unitarios del caso de uso de escritura del plano ({@link FloorPlanCommandService}):
 * alta puesto-especifica de la solicitud, comprobacion de disponibilidad, unicidad de
 * pendiente, ventana temporal y persistencia de coordenadas. Las dependencias se mockean; se
 * verifican ramas y colaboraciones (no BD real: eso lo cubre {@code FloorPlanIT}).
 */
class FloorPlanCommandServiceTest {

    private static final String LOGIN = "empleado.uno";
    private static final long EMPLOYEE_ID = 7L;
    private static final long DESK_ID = 42L;
    private static final Instant NOW = Instant.parse("2026-07-04T00:00:00Z");
    private static final LocalDate TODAY = LocalDate.ofInstant(NOW, ZoneOffset.UTC);
    private static final LocalDate WITHIN = TODAY.plusDays(3);

    private DeskRepository deskRepository;
    private RequestJpaRepository requestRepository;
    private EmployeeRepository employeeRepository;
    private AvailabilityService availabilityService;
    private SystemSettingsService systemSettingsService;
    private ApplicationEventPublisher eventPublisher;
    private ClockPort clock;

    private FloorPlanCommandService service;

    @BeforeEach
    void setUp() {
        deskRepository = mock(DeskRepository.class);
        requestRepository = mock(RequestJpaRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        availabilityService = mock(AvailabilityService.class);
        systemSettingsService = mock(SystemSettingsService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        clock = mock(ClockPort.class);
        given(clock.now()).willReturn(NOW);
        // Modo por defecto MANUAL (retrocompatible); los tests de AUTOMATIC lo sobreescriben.
        given(systemSettingsService.approvalMode()).willReturn(ApprovalMode.MANUAL);
        service = new FloorPlanCommandService(deskRepository, requestRepository,
                employeeRepository, availabilityService, systemSettingsService, eventPublisher, clock);
    }

    @Test
    void shouldCreatePendingRequestAndReturnMine_whenDeskIsFreeInManualMode() {
        // Arrange
        stubFreeRequestableDesk();
        RequestEntity saved = mock(RequestEntity.class);
        given(saved.getId()).willReturn(128L);
        given(saved.getStatus()).willReturn(RequestStatus.PENDING);
        given(requestRepository.saveAndFlush(any(RequestEntity.class))).willReturn(saved);

        // Act
        DeskRequestResponse response = service.requestDesk(LOGIN, DESK_ID, WITHIN);

        // Assert: nace PENDING con evento de creacion (comportamiento clasico intacto)
        assertThat(response.requestId()).isEqualTo(128L);
        assertThat(response.deskId()).isEqualTo(DESK_ID);
        assertThat(response.state()).isEqualTo(FloorPlanDeskState.MINE);
        assertThat(response.status()).isEqualTo(RequestStatus.PENDING);

        ArgumentCaptor<RequestEntity> captor = forClass(RequestEntity.class);
        verify(requestRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(captor.getValue().getResourceId()).isEqualTo(DESK_ID);
        verify(eventPublisher).publishEvent(any(RequestCreatedEvent.class));
        verify(eventPublisher, never()).publishEvent(any(RequestApprovedEvent.class));
    }

    @Test
    void shouldAutoApproveDeskAndReturnApproved_whenDeskIsFreeInAutomaticMode() {
        // Arrange: modo AUTOMATIC + puesto libre y solicitable
        given(systemSettingsService.approvalMode()).willReturn(ApprovalMode.AUTOMATIC);
        stubFreeRequestableDesk();
        RequestEntity saved = mock(RequestEntity.class);
        given(saved.getId()).willReturn(200L);
        given(saved.getStatus()).willReturn(RequestStatus.APPROVED);
        given(requestRepository.saveAndFlush(any(RequestEntity.class))).willReturn(saved);

        // Act
        DeskRequestResponse response = service.requestDesk(LOGIN, DESK_ID, WITHIN);

        // Assert: el puesto pinchado se auto-aprueba (nace APPROVED, actor null, nota "auto")
        assertThat(response.requestId()).isEqualTo(200L);
        assertThat(response.deskId()).isEqualTo(DESK_ID);
        assertThat(response.state()).isEqualTo(FloorPlanDeskState.MINE);
        assertThat(response.status()).isEqualTo(RequestStatus.APPROVED);

        ArgumentCaptor<RequestEntity> captor = forClass(RequestEntity.class);
        verify(requestRepository).saveAndFlush(captor.capture());
        RequestEntity persisted = captor.getValue();
        assertThat(persisted.getStatus()).isEqualTo(RequestStatus.APPROVED);
        assertThat(persisted.getResourceId()).isEqualTo(DESK_ID);
        assertThat(persisted.getResolvedById()).isNull();
        assertThat(persisted.getApprovalNote()).isEqualTo("auto");
        assertThat(persisted.getResolvedAt()).isEqualTo(NOW);
        // Mismo evento y semantica que RequestService.autoApprove (paridad)
        verify(eventPublisher).publishEvent(any(RequestApprovedEvent.class));
        verify(eventPublisher, never()).publishEvent(any(RequestCreatedEvent.class));
    }

    @Test
    void shouldThrowUnavailableAndNotPersist_whenDeskTakenInAutomaticMode() {
        // Arrange: la disponibilidad se valida igual en ambos modos (antes de ramificar)
        given(systemSettingsService.approvalMode()).willReturn(ApprovalMode.AUTOMATIC);
        stubEmployee();
        stubActiveDesk();
        given(availabilityService.isSpaceTakenForDate(DESK_ID, ResourceType.DESK, WITHIN))
                .willReturn(true);

        // Act / Assert
        assertThatThrownBy(() -> service.requestDesk(LOGIN, DESK_ID, WITHIN))
                .isInstanceOf(SpaceUnavailableException.class);
        verify(requestRepository, never()).saveAndFlush(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowDuplicatePendingAndNotPersist_whenEmployeeHasPendingInAutomaticMode() {
        // Arrange: el no-duplicado se valida igual en ambos modos (antes de ramificar)
        given(systemSettingsService.approvalMode()).willReturn(ApprovalMode.AUTOMATIC);
        stubEmployee();
        stubActiveDesk();
        given(availabilityService.isSpaceTakenForDate(DESK_ID, ResourceType.DESK, WITHIN))
                .willReturn(false);
        given(requestRepository.existsByResourceIdAndResourceTypeAndRequestedDateAndStatus(
                DESK_ID, ResourceType.DESK, WITHIN, RequestStatus.PENDING)).willReturn(false);
        given(requestRepository.existsByEmployeeIdAndResourceTypeAndRequestedDateAndStatus(
                EMPLOYEE_ID, ResourceType.DESK, WITHIN, RequestStatus.PENDING)).willReturn(true);

        // Act / Assert
        assertThatThrownBy(() -> service.requestDesk(LOGIN, DESK_ID, WITHIN))
                .isInstanceOf(DuplicatePendingRequestException.class);
        verify(requestRepository, never()).saveAndFlush(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowOutsideWindow_whenDateIsInThePast() {
        // Act / Assert: fecha pasada rechazada antes de tocar repositorios
        // (ya no hay tope superior: cualquier fecha futura es valida)
        assertThatThrownBy(() -> service.requestDesk(LOGIN, DESK_ID, TODAY.minusDays(1)))
                .isInstanceOf(OutsideRequestWindowException.class);
        verify(requestRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldThrowNotFound_whenDeskDoesNotExist() {
        // Arrange
        stubEmployee();
        given(deskRepository.findById(DESK_ID)).willReturn(Optional.empty());

        // Act / Assert
        assertThatThrownBy(() -> service.requestDesk(LOGIN, DESK_ID, WITHIN))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void shouldThrowUnavailable_whenDeskInactive() {
        // Arrange
        stubEmployee();
        Desk desk = mock(Desk.class);
        given(desk.getId()).willReturn(DESK_ID);
        given(desk.isActive()).willReturn(false);
        given(deskRepository.findById(DESK_ID)).willReturn(Optional.of(desk));

        // Act / Assert
        assertThatThrownBy(() -> service.requestDesk(LOGIN, DESK_ID, WITHIN))
                .isInstanceOf(SpaceUnavailableException.class);
        verify(requestRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldThrowUnavailable_whenDeskTakenForDate() {
        // Arrange
        stubEmployee();
        stubActiveDesk();
        given(availabilityService.isSpaceTakenForDate(DESK_ID, ResourceType.DESK, WITHIN))
                .willReturn(true);

        // Act / Assert
        assertThatThrownBy(() -> service.requestDesk(LOGIN, DESK_ID, WITHIN))
                .isInstanceOf(SpaceUnavailableException.class);
    }

    @Test
    void shouldThrowUnavailable_whenDeskAlreadyHasPendingRequest() {
        // Arrange
        stubEmployee();
        stubActiveDesk();
        given(availabilityService.isSpaceTakenForDate(DESK_ID, ResourceType.DESK, WITHIN))
                .willReturn(false);
        given(requestRepository.existsByResourceIdAndResourceTypeAndRequestedDateAndStatus(
                DESK_ID, ResourceType.DESK, WITHIN, RequestStatus.PENDING)).willReturn(true);

        // Act / Assert
        assertThatThrownBy(() -> service.requestDesk(LOGIN, DESK_ID, WITHIN))
                .isInstanceOf(SpaceUnavailableException.class);
    }

    @Test
    void shouldThrowDuplicatePending_whenEmployeeAlreadyHasPendingDeskThatDate() {
        // Arrange
        stubEmployee();
        stubActiveDesk();
        given(availabilityService.isSpaceTakenForDate(DESK_ID, ResourceType.DESK, WITHIN))
                .willReturn(false);
        given(requestRepository.existsByResourceIdAndResourceTypeAndRequestedDateAndStatus(
                DESK_ID, ResourceType.DESK, WITHIN, RequestStatus.PENDING)).willReturn(false);
        given(requestRepository.existsByEmployeeIdAndResourceTypeAndRequestedDateAndStatus(
                EMPLOYEE_ID, ResourceType.DESK, WITHIN, RequestStatus.PENDING)).willReturn(true);

        // Act / Assert
        assertThatThrownBy(() -> service.requestDesk(LOGIN, DESK_ID, WITHIN))
                .isInstanceOf(DuplicatePendingRequestException.class);
        verify(requestRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldPersistCoordinates_whenUpdatingPosition() {
        // Arrange
        Desk desk = mock(Desk.class);
        given(deskRepository.findById(DESK_ID)).willReturn(Optional.of(desk));
        BigDecimal x = new BigDecimal("30.5");
        BigDecimal y = new BigDecimal("47.0");

        // Act
        service.updatePosition(DESK_ID, x, y);

        // Assert
        verify(desk).setCoordX(x);
        verify(desk).setCoordY(y);
        verify(deskRepository).save(desk);
    }

    @Test
    void shouldThrowNotFound_whenUpdatingPositionOfUnknownDesk() {
        // Arrange
        given(deskRepository.findById(DESK_ID)).willReturn(Optional.empty());

        // Act / Assert
        assertThatThrownBy(() -> service.updatePosition(DESK_ID, BigDecimal.ONE, BigDecimal.TEN))
                .isInstanceOf(EntityNotFoundException.class);
        verify(deskRepository, never()).save(any());
    }

    /** Puesto libre, activo y solicitable por el empleado (sin ocupacion ni duplicado pendiente). */
    private void stubFreeRequestableDesk() {
        stubEmployee();
        stubActiveDesk();
        given(availabilityService.isSpaceTakenForDate(DESK_ID, ResourceType.DESK, WITHIN))
                .willReturn(false);
        given(requestRepository.existsByResourceIdAndResourceTypeAndRequestedDateAndStatus(
                DESK_ID, ResourceType.DESK, WITHIN, RequestStatus.PENDING)).willReturn(false);
        given(requestRepository.existsByEmployeeIdAndResourceTypeAndRequestedDateAndStatus(
                EMPLOYEE_ID, ResourceType.DESK, WITHIN, RequestStatus.PENDING)).willReturn(false);
    }

    private void stubEmployee() {
        Employee employee = mock(Employee.class);
        given(employee.getId()).willReturn(EMPLOYEE_ID);
        given(employeeRepository.findByLogin(LOGIN)).willReturn(Optional.of(employee));
    }

    private void stubActiveDesk() {
        Desk desk = mock(Desk.class);
        given(desk.getId()).willReturn(DESK_ID);
        given(desk.isActive()).willReturn(true);
        given(deskRepository.findById(DESK_ID)).willReturn(Optional.of(desk));
    }
}
