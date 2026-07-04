package com.aleatica.parking.visitor.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.aleatica.parking.auth.domain.ClockPort;
import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.fixedassignment.FixedAssignmentRepository;
import com.aleatica.parking.parkingspace.ParkingSpace;
import com.aleatica.parking.parkingspace.ParkingSpaceRepository;
import com.aleatica.parking.release.ReleaseRepository;
import com.aleatica.parking.request.RequestRepository;
import com.aleatica.parking.request.RequestStatus;
import com.aleatica.parking.visitor.VisitorRepository;
import com.aleatica.parking.visitor.VisitorReservation;
import com.aleatica.parking.visitor.VisitorReservationRepository;
import com.aleatica.parking.visitor.dto.VisitorReservationCreateRequest;
import com.aleatica.parking.visitor.dto.VisitorReservationResponse;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Tests unitarios de {@link VisitorReservationService} con repositorios, publicador de
 * eventos y {@code ClockPort} mockeados: control de disponibilidad transaccional (plaza
 * inactiva, asignacion fija no liberada, solicitud aprobada u otra reserva), y anulacion
 * de reservas solo futuras. No toca la base de datos.
 */
@ExtendWith(MockitoExtension.class)
class VisitorReservationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-04T10:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 4);
    private static final LocalDate FUTURE = LocalDate.of(2026, 7, 10);
    private static final LocalDate PAST = LocalDate.of(2026, 7, 3);
    private static final int FUTURE_DOW = FUTURE.getDayOfWeek().getValue();

    private static final String ADMIN_LOGIN = "admin";
    private static final Long ADMIN_ID = 1L;
    private static final Long VISITOR_ID = 42L;
    private static final Long SPACE_ID = 8L;
    private static final Long RESERVATION_ID = 7L;

    @Mock
    private VisitorReservationRepository reservationRepository;
    @Mock
    private VisitorRepository visitorRepository;
    @Mock
    private ParkingSpaceRepository parkingSpaceRepository;
    @Mock
    private FixedAssignmentRepository fixedAssignmentRepository;
    @Mock
    private RequestRepository requestRepository;
    @Mock
    private ReleaseRepository releaseRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private ClockPort clock;

    private VisitorReservationService service() {
        return new VisitorReservationService(reservationRepository, visitorRepository,
                parkingSpaceRepository, fixedAssignmentRepository, requestRepository, releaseRepository,
                employeeRepository, eventPublisher, clock);
    }

    @Test
    void shouldCreateReservation_whenSpaceIsAvailableForDate() {
        // Arrange: plaza activa sin ninguna ocupacion para FUTURE
        givenActor();
        given(visitorRepository.existsById(VISITOR_ID)).willReturn(true);
        ParkingSpace space = activeSpace();
        given(parkingSpaceRepository.findById(SPACE_ID)).willReturn(Optional.of(space));
        given(fixedAssignmentRepository.existsByParkingSpaceIdAndDayOfWeekAndActiveTrue(SPACE_ID, FUTURE_DOW))
                .willReturn(false);
        given(requestRepository.existsByParkingSpaceIdAndRequestedDateAndStatus(
                SPACE_ID, FUTURE, RequestStatus.APPROVED)).willReturn(false);
        given(reservationRepository.existsByParkingSpaceIdAndReservationDate(SPACE_ID, FUTURE))
                .willReturn(false);
        given(clock.now()).willReturn(NOW);
        given(reservationRepository.saveAndFlush(any(VisitorReservation.class)))
                .willAnswer(inv -> inv.getArgument(0));

        // Act
        VisitorReservationResponse created = service().create(ADMIN_LOGIN, request());

        // Assert
        assertThat(created.parkingSpaceId()).isEqualTo(SPACE_ID);
        assertThat(created.createdById()).isEqualTo(ADMIN_ID);
        verify(eventPublisher).publishEvent(any(VisitorReservationAuditEvent.class));
    }

    @Test
    void shouldThrowUnavailable_whenSpaceIsInactive() {
        // Arrange
        givenActor();
        given(visitorRepository.existsById(VISITOR_ID)).willReturn(true);
        ParkingSpace space = inactiveSpace();
        given(parkingSpaceRepository.findById(SPACE_ID)).willReturn(Optional.of(space));

        // Act / Assert
        assertThatThrownBy(() -> service().create(ADMIN_LOGIN, request()))
                .isInstanceOf(SpaceNotAvailableForReservationException.class);
        verify(reservationRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldThrowUnavailable_whenFixedAssignmentNotReleased() {
        // Arrange: asignacion fija activa ese dia y NO liberada para FUTURE
        givenActor();
        given(visitorRepository.existsById(VISITOR_ID)).willReturn(true);
        ParkingSpace space = activeSpace();
        given(parkingSpaceRepository.findById(SPACE_ID)).willReturn(Optional.of(space));
        given(fixedAssignmentRepository.existsByParkingSpaceIdAndDayOfWeekAndActiveTrue(SPACE_ID, FUTURE_DOW))
                .willReturn(true);
        given(releaseRepository.existsByParkingSpaceIdAndReleaseDate(SPACE_ID, FUTURE)).willReturn(false);

        // Act / Assert
        assertThatThrownBy(() -> service().create(ADMIN_LOGIN, request()))
                .isInstanceOf(SpaceNotAvailableForReservationException.class);
        verify(reservationRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldThrowUnavailable_whenApprovedRequestExists() {
        // Arrange: solicitud APPROVED para SPACE/FUTURE
        givenActor();
        given(visitorRepository.existsById(VISITOR_ID)).willReturn(true);
        ParkingSpace space = activeSpace();
        given(parkingSpaceRepository.findById(SPACE_ID)).willReturn(Optional.of(space));
        given(fixedAssignmentRepository.existsByParkingSpaceIdAndDayOfWeekAndActiveTrue(SPACE_ID, FUTURE_DOW))
                .willReturn(false);
        given(requestRepository.existsByParkingSpaceIdAndRequestedDateAndStatus(
                SPACE_ID, FUTURE, RequestStatus.APPROVED)).willReturn(true);

        // Act / Assert
        assertThatThrownBy(() -> service().create(ADMIN_LOGIN, request()))
                .isInstanceOf(SpaceNotAvailableForReservationException.class);
        verify(reservationRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldThrowUnavailable_whenAnotherVisitorReservationExists() {
        // Arrange: otra reserva de visitante ya ocupa SPACE/FUTURE
        givenActor();
        given(visitorRepository.existsById(VISITOR_ID)).willReturn(true);
        ParkingSpace space = activeSpace();
        given(parkingSpaceRepository.findById(SPACE_ID)).willReturn(Optional.of(space));
        given(fixedAssignmentRepository.existsByParkingSpaceIdAndDayOfWeekAndActiveTrue(SPACE_ID, FUTURE_DOW))
                .willReturn(false);
        given(requestRepository.existsByParkingSpaceIdAndRequestedDateAndStatus(
                SPACE_ID, FUTURE, RequestStatus.APPROVED)).willReturn(false);
        given(reservationRepository.existsByParkingSpaceIdAndReservationDate(SPACE_ID, FUTURE))
                .willReturn(true);

        // Act / Assert
        assertThatThrownBy(() -> service().create(ADMIN_LOGIN, request()))
                .isInstanceOf(SpaceNotAvailableForReservationException.class);
        verify(reservationRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldCreateReservation_whenFixedAssignmentIsReleasedForDate() {
        // Arrange: asignacion fija ese dia PERO liberada para FUTURE -> disponible
        givenActor();
        given(visitorRepository.existsById(VISITOR_ID)).willReturn(true);
        ParkingSpace space = activeSpace();
        given(parkingSpaceRepository.findById(SPACE_ID)).willReturn(Optional.of(space));
        given(fixedAssignmentRepository.existsByParkingSpaceIdAndDayOfWeekAndActiveTrue(SPACE_ID, FUTURE_DOW))
                .willReturn(true);
        given(releaseRepository.existsByParkingSpaceIdAndReleaseDate(SPACE_ID, FUTURE)).willReturn(true);
        given(requestRepository.existsByParkingSpaceIdAndRequestedDateAndStatus(
                SPACE_ID, FUTURE, RequestStatus.APPROVED)).willReturn(false);
        given(reservationRepository.existsByParkingSpaceIdAndReservationDate(SPACE_ID, FUTURE))
                .willReturn(false);
        given(clock.now()).willReturn(NOW);
        given(reservationRepository.saveAndFlush(any(VisitorReservation.class)))
                .willAnswer(inv -> inv.getArgument(0));

        // Act
        VisitorReservationResponse created = service().create(ADMIN_LOGIN, request());

        // Assert
        assertThat(created.parkingSpaceId()).isEqualTo(SPACE_ID);
    }

    @Test
    void shouldThrowNotFound_whenVisitorDoesNotExist() {
        // Arrange
        givenActor();
        given(visitorRepository.existsById(VISITOR_ID)).willReturn(false);

        // Act / Assert
        assertThatThrownBy(() -> service().create(ADMIN_LOGIN, request()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void shouldCancelReservation_whenReservationIsInFuture() {
        // Arrange
        given(reservationRepository.findById(RESERVATION_ID))
                .willReturn(Optional.of(reservation(FUTURE)));
        given(clock.now()).willReturn(NOW);

        // Act
        service().cancel(RESERVATION_ID);

        // Assert
        verify(reservationRepository).delete(any(VisitorReservation.class));
        verify(eventPublisher).publishEvent(any(VisitorReservationAuditEvent.class));
    }

    @Test
    void shouldThrowPast_whenCancelingPastReservation() {
        // Arrange
        given(reservationRepository.findById(RESERVATION_ID))
                .willReturn(Optional.of(reservation(PAST)));
        given(clock.now()).willReturn(NOW);

        // Act / Assert
        assertThatThrownBy(() -> service().cancel(RESERVATION_ID))
                .isInstanceOf(PastVisitorReservationCancellationException.class);
        verify(reservationRepository, never()).delete(any());
    }

    @Test
    void shouldThrowNotFound_whenCancelingUnknownReservation() {
        // Arrange
        given(reservationRepository.findById(RESERVATION_ID)).willReturn(Optional.empty());

        // Act / Assert
        assertThatThrownBy(() -> service().cancel(RESERVATION_ID))
                .isInstanceOf(EntityNotFoundException.class);
    }

    private void givenActor() {
        Employee admin = mock(Employee.class);
        given(admin.getId()).willReturn(ADMIN_ID);
        given(employeeRepository.findByLogin(ADMIN_LOGIN)).willReturn(Optional.of(admin));
    }

    private VisitorReservationCreateRequest request() {
        return new VisitorReservationCreateRequest(VISITOR_ID, SPACE_ID, FUTURE, "Puerta norte");
    }

    private VisitorReservation reservation(LocalDate date) {
        return VisitorReservation.create(VISITOR_ID, SPACE_ID, date, null, ADMIN_ID, NOW);
    }

    private ParkingSpace activeSpace() {
        ParkingSpace space = mock(ParkingSpace.class);
        given(space.getId()).willReturn(SPACE_ID);
        given(space.isActive()).willReturn(true);
        return space;
    }

    private ParkingSpace inactiveSpace() {
        ParkingSpace space = mock(ParkingSpace.class);
        given(space.isActive()).willReturn(false);
        return space;
    }
}
