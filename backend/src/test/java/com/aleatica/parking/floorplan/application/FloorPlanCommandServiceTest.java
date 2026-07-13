package com.aleatica.parking.floorplan.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
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
import com.aleatica.parking.notification.event.RequestCreatedEvent;
import com.aleatica.parking.request.infrastructure.RequestEntity;
import com.aleatica.parking.request.infrastructure.RequestJpaRepository;
import com.aleatica.parking.request.domain.RequestStatus;
import com.aleatica.parking.request.application.DuplicatePendingRequestException;
import com.aleatica.parking.request.application.OutsideRequestWindowException;
import com.aleatica.parking.request.application.SpaceUnavailableException;
import com.aleatica.parking.resource.ResourceType;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
    private ApplicationEventPublisher eventPublisher;
    private ClockPort clock;

    private FloorPlanCommandService service;

    @BeforeEach
    void setUp() {
        deskRepository = mock(DeskRepository.class);
        requestRepository = mock(RequestJpaRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        availabilityService = mock(AvailabilityService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        clock = mock(ClockPort.class);
        given(clock.now()).willReturn(NOW);
        service = new FloorPlanCommandService(deskRepository, requestRepository,
                employeeRepository, availabilityService, eventPublisher, clock);
    }

    @Test
    void shouldCreatePendingRequestAndReturnMine_whenDeskIsFree() {
        // Arrange
        stubEmployee();
        stubActiveDesk();
        given(availabilityService.isSpaceTakenForDate(DESK_ID, ResourceType.DESK, WITHIN))
                .willReturn(false);
        given(requestRepository.existsByResourceIdAndResourceTypeAndRequestedDateAndStatus(
                DESK_ID, ResourceType.DESK, WITHIN, RequestStatus.PENDING)).willReturn(false);
        given(requestRepository.existsByEmployeeIdAndResourceTypeAndRequestedDateAndStatus(
                EMPLOYEE_ID, ResourceType.DESK, WITHIN, RequestStatus.PENDING)).willReturn(false);
        RequestEntity saved = mock(RequestEntity.class);
        given(saved.getId()).willReturn(128L);
        given(requestRepository.saveAndFlush(any(RequestEntity.class))).willReturn(saved);

        // Act
        DeskRequestResponse response = service.requestDesk(LOGIN, DESK_ID, WITHIN);

        // Assert
        assertThat(response.requestId()).isEqualTo(128L);
        assertThat(response.deskId()).isEqualTo(DESK_ID);
        assertThat(response.state()).isEqualTo(FloorPlanDeskState.MINE);
        verify(requestRepository).saveAndFlush(any(RequestEntity.class));
        verify(eventPublisher).publishEvent(any(RequestCreatedEvent.class));
    }

    @Test
    void shouldThrowOutsideWindow_whenDateBeyondFourteenDays() {
        // Act / Assert: fuera de ventana antes de tocar repositorios
        assertThatThrownBy(() -> service.requestDesk(LOGIN, DESK_ID, TODAY.plusDays(15)))
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
