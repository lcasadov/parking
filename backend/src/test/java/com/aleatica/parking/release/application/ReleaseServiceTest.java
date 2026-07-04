package com.aleatica.parking.release.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.aleatica.parking.auth.domain.ClockPort;
import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.fixedassignment.FixedAssignment;
import com.aleatica.parking.fixedassignment.FixedAssignmentRepository;
import com.aleatica.parking.parkingspace.ParkingSpaceRepository;
import com.aleatica.parking.release.Release;
import com.aleatica.parking.release.ReleaseRepository;
import com.aleatica.parking.resource.ResourceType;
import com.aleatica.parking.release.ReleaseType;
import com.aleatica.parking.release.dto.AdministrativeReleaseRequest;
import com.aleatica.parking.release.dto.ReleaseCreateRequest;
import com.aleatica.parking.release.dto.ReleaseResponse;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
 * Tests unitarios de {@link ReleaseService} con repositorios y publicador de eventos
 * mockeados: ventana temporal, resolucion de plaza fija, unicidad recurso+fecha,
 * verificacion de pertenencia (BOLA) en cancelacion y validacion de la liberacion
 * administrativa. No toca la base de datos.
 */
@ExtendWith(MockitoExtension.class)
class ReleaseServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-04T10:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 4);
    private static final LocalDate FUTURE = LocalDate.of(2026, 7, 10);
    private static final LocalDate PAST = LocalDate.of(2026, 7, 3);
    private static final int FUTURE_DOW = FUTURE.getDayOfWeek().getValue();

    private static final String EMP_LOGIN = "employee";
    private static final String OTHER_LOGIN = "otheremployee";
    private static final String ADMIN_LOGIN = "admin";
    private static final Long EMP_ID = 15L;
    private static final Long OTHER_ID = 99L;
    private static final Long ADMIN_ID = 1L;
    private static final Long SPACE_ID = 8L;
    private static final Long OTHER_SPACE_ID = 9L;
    private static final Long RELEASE_ID = 42L;
    private static final String REASON = "Ausencia justificada";

    @Mock
    private ReleaseRepository releaseRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ParkingSpaceRepository parkingSpaceRepository;

    @Mock
    private FixedAssignmentRepository fixedAssignmentRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<Release> savedCaptor;

    @Captor
    private ArgumentCaptor<ReleaseAuditEvent> eventCaptor;

    private final ClockPort clock = () -> NOW;

    private ReleaseService newService() {
        return new ReleaseService(
                releaseRepository, employeeRepository, parkingSpaceRepository,
                fixedAssignmentRepository, eventPublisher, clock);
    }

    // ---- Liberacion voluntaria: ventana + resolucion de plaza + unicidad ----

    @Test
    void shouldCreateVoluntaryRelease_whenOwnerAndFutureDate() {
        // Arrange: una sola asignacion fija activa ese dia -> plaza resuelta implicitamente
        givenActor(EMP_LOGIN, EMP_ID);
        givenAssignmentsFor(EMP_ID, FUTURE_DOW, assignment(SPACE_ID));
        given(releaseRepository.existsByResourceIdAndResourceTypeAndReleaseDate(SPACE_ID, ResourceType.PARKING, FUTURE)).willReturn(false);
        given(releaseRepository.saveAndFlush(any(Release.class))).willAnswer(inv -> inv.getArgument(0));

        // Act
        ReleaseResponse result =
                newService().createRelease(EMP_LOGIN, new ReleaseCreateRequest(FUTURE, null));

        // Assert: VOLUNTARY, titular = ejecutor, reason nulo, plaza resuelta
        assertThat(result.type()).isEqualTo(ReleaseType.VOLUNTARY);
        assertThat(result.employeeId()).isEqualTo(EMP_ID);
        assertThat(result.releasedById()).isEqualTo(EMP_ID);
        assertThat(result.reason()).isNull();
        assertThat(result.parkingSpaceId()).isEqualTo(SPACE_ID);
        verify(releaseRepository).saveAndFlush(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getCreatedAt()).isEqualTo(NOW);
        verifyEventKind(ReleaseAuditEvent.Kind.VOLUNTARY_RELEASED);
    }

    @Test
    void shouldCreateVoluntaryRelease_whenReleaseDateIsToday() {
        // Arrange (frontera inferior inclusive: hoy cuenta como futuro inmediato)
        givenActor(EMP_LOGIN, EMP_ID);
        givenAssignmentsFor(EMP_ID, TODAY.getDayOfWeek().getValue(), assignment(SPACE_ID));
        given(releaseRepository.existsByResourceIdAndResourceTypeAndReleaseDate(SPACE_ID, ResourceType.PARKING, TODAY)).willReturn(false);
        given(releaseRepository.saveAndFlush(any(Release.class))).willAnswer(inv -> inv.getArgument(0));

        // Act / Assert
        assertThat(newService().createRelease(EMP_LOGIN, new ReleaseCreateRequest(TODAY, null)).type())
                .isEqualTo(ReleaseType.VOLUNTARY);
    }

    @Test
    void shouldResolveExplicitSpace_whenOwnedByEmployeeThatDay() {
        // Arrange: dos asignaciones ese dia; se libera la explicita
        givenActor(EMP_LOGIN, EMP_ID);
        givenAssignmentsFor(EMP_ID, FUTURE_DOW, assignment(SPACE_ID), assignment(OTHER_SPACE_ID));
        given(releaseRepository.existsByResourceIdAndResourceTypeAndReleaseDate(
                OTHER_SPACE_ID, ResourceType.PARKING, FUTURE))
                .willReturn(false);
        given(releaseRepository.saveAndFlush(any(Release.class))).willAnswer(inv -> inv.getArgument(0));

        // Act
        ReleaseResponse result =
                newService().createRelease(EMP_LOGIN, new ReleaseCreateRequest(FUTURE, OTHER_SPACE_ID));

        // Assert
        assertThat(result.parkingSpaceId()).isEqualTo(OTHER_SPACE_ID);
    }

    @Test
    void shouldRejectWith400_whenReleaseDateInPast() {
        // Arrange
        givenActor(EMP_LOGIN, EMP_ID);

        // Act / Assert
        assertThatThrownBy(() -> newService().createRelease(EMP_LOGIN, new ReleaseCreateRequest(PAST, null)))
                .isInstanceOf(ReleaseDateInPastException.class);
        verify(releaseRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldRejectWith409_whenNoFixedAssignmentForThatDay() {
        // Arrange: sin asignaciones activas ese dia
        givenActor(EMP_LOGIN, EMP_ID);
        givenAssignmentsFor(EMP_ID, FUTURE_DOW);

        // Act / Assert
        assertThatThrownBy(() -> newService().createRelease(EMP_LOGIN, new ReleaseCreateRequest(FUTURE, null)))
                .isInstanceOf(NoFixedAssignmentException.class);
        verify(releaseRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldRejectWith409_whenAmbiguousImplicitResolution() {
        // Arrange: dos asignaciones ese dia y no se indica la plaza -> ambiguo (la plaza
        // concreta no se llega a leer: la ambiguedad se detecta por el numero de filas)
        givenActor(EMP_LOGIN, EMP_ID);
        givenAssignmentsFor(EMP_ID, FUTURE_DOW,
                mock(com.aleatica.parking.fixedassignment.FixedAssignment.class),
                mock(com.aleatica.parking.fixedassignment.FixedAssignment.class));

        // Act / Assert
        assertThatThrownBy(() -> newService().createRelease(EMP_LOGIN, new ReleaseCreateRequest(FUTURE, null)))
                .isInstanceOf(NoFixedAssignmentException.class);
        verify(releaseRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldRejectWith409_whenExplicitSpaceNotOwnedThatDay() {
        // Arrange: la plaza explicita no pertenece a ninguna asignacion del empleado ese dia
        givenActor(EMP_LOGIN, EMP_ID);
        givenAssignmentsFor(EMP_ID, FUTURE_DOW, assignment(SPACE_ID));

        // Act / Assert
        assertThatThrownBy(() -> newService()
                .createRelease(EMP_LOGIN, new ReleaseCreateRequest(FUTURE, OTHER_SPACE_ID)))
                .isInstanceOf(NoFixedAssignmentException.class);
        verify(releaseRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldRejectWith409_whenResourceAlreadyReleasedForDate() {
        // Arrange: el recurso ya tiene una liberacion para esa fecha (comprobacion previa)
        givenActor(EMP_LOGIN, EMP_ID);
        givenAssignmentsFor(EMP_ID, FUTURE_DOW, assignment(SPACE_ID));
        given(releaseRepository.existsByResourceIdAndResourceTypeAndReleaseDate(SPACE_ID, ResourceType.PARKING, FUTURE)).willReturn(true);

        // Act / Assert
        assertThatThrownBy(() -> newService().createRelease(EMP_LOGIN, new ReleaseCreateRequest(FUTURE, null)))
                .isInstanceOf(ResourceAlreadyReleasedException.class);
        verify(releaseRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldThrowNotFound_whenSessionUserNoLongerExists() {
        // Arrange: el principal de la sesion ya no resuelve a un empleado
        given(employeeRepository.findByLogin(EMP_LOGIN)).willReturn(Optional.empty());

        // Act / Assert
        assertThatThrownBy(() -> newService().createRelease(EMP_LOGIN, new ReleaseCreateRequest(FUTURE, null)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ---- Liberacion administrativa ----

    @Test
    void shouldCreateAdministrativeRelease_whenAdminAndReasonPresent() {
        // Arrange
        givenActor(ADMIN_LOGIN, ADMIN_ID);
        given(employeeRepository.existsById(EMP_ID)).willReturn(true);
        given(parkingSpaceRepository.existsById(SPACE_ID)).willReturn(true);
        givenAssignmentsFor(EMP_ID, FUTURE_DOW, assignment(SPACE_ID));
        given(releaseRepository.existsByResourceIdAndResourceTypeAndReleaseDate(SPACE_ID, ResourceType.PARKING, FUTURE)).willReturn(false);
        given(releaseRepository.saveAndFlush(any(Release.class))).willAnswer(inv -> inv.getArgument(0));

        // Act
        ReleaseResponse result = newService().createAdministrativeRelease(
                ADMIN_LOGIN, new AdministrativeReleaseRequest(EMP_ID, SPACE_ID, FUTURE, REASON));

        // Assert: ADMINISTRATIVE, titular != ejecutor, reason presente
        assertThat(result.type()).isEqualTo(ReleaseType.ADMINISTRATIVE);
        assertThat(result.employeeId()).isEqualTo(EMP_ID);
        assertThat(result.releasedById()).isEqualTo(ADMIN_ID);
        assertThat(result.reason()).isEqualTo(REASON);
        verifyEventKind(ReleaseAuditEvent.Kind.ADMINISTRATIVE_RELEASED);
    }

    @Test
    void shouldThrowNotFound_whenAdministrativeEmployeeUnknown() {
        // Arrange
        givenActor(ADMIN_LOGIN, ADMIN_ID);
        given(employeeRepository.existsById(EMP_ID)).willReturn(false);

        // Act / Assert
        assertThatThrownBy(() -> newService().createAdministrativeRelease(
                ADMIN_LOGIN, new AdministrativeReleaseRequest(EMP_ID, SPACE_ID, FUTURE, REASON)))
                .isInstanceOf(EntityNotFoundException.class);
        verify(releaseRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldThrowNotFound_whenAdministrativeSpaceUnknown() {
        // Arrange
        givenActor(ADMIN_LOGIN, ADMIN_ID);
        given(employeeRepository.existsById(EMP_ID)).willReturn(true);
        given(parkingSpaceRepository.existsById(SPACE_ID)).willReturn(false);

        // Act / Assert
        assertThatThrownBy(() -> newService().createAdministrativeRelease(
                ADMIN_LOGIN, new AdministrativeReleaseRequest(EMP_ID, SPACE_ID, FUTURE, REASON)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void shouldThrowNoFixedAssignment_whenAdministrativeSpaceNotAssignedThatDay() {
        // Arrange: el recurso no tiene una asignacion fija activa del empleado ese dia
        givenActor(ADMIN_LOGIN, ADMIN_ID);
        given(employeeRepository.existsById(EMP_ID)).willReturn(true);
        given(parkingSpaceRepository.existsById(SPACE_ID)).willReturn(true);
        givenAssignmentsFor(EMP_ID, FUTURE_DOW, assignment(OTHER_SPACE_ID));

        // Act / Assert
        assertThatThrownBy(() -> newService().createAdministrativeRelease(
                ADMIN_LOGIN, new AdministrativeReleaseRequest(EMP_ID, SPACE_ID, FUTURE, REASON)))
                .isInstanceOf(NoFixedAssignmentException.class);
    }

    @Test
    void shouldThrowAlreadyReleased_whenAdministrativeDuplicate() {
        // Arrange
        givenActor(ADMIN_LOGIN, ADMIN_ID);
        given(employeeRepository.existsById(EMP_ID)).willReturn(true);
        given(parkingSpaceRepository.existsById(SPACE_ID)).willReturn(true);
        givenAssignmentsFor(EMP_ID, FUTURE_DOW, assignment(SPACE_ID));
        given(releaseRepository.existsByResourceIdAndResourceTypeAndReleaseDate(SPACE_ID, ResourceType.PARKING, FUTURE)).willReturn(true);

        // Act / Assert
        assertThatThrownBy(() -> newService().createAdministrativeRelease(
                ADMIN_LOGIN, new AdministrativeReleaseRequest(EMP_ID, SPACE_ID, FUTURE, REASON)))
                .isInstanceOf(ResourceAlreadyReleasedException.class);
    }

    @Test
    void shouldThrowDateInPast_whenAdministrativeReleaseDateBeforeToday() {
        // Arrange
        givenActor(ADMIN_LOGIN, ADMIN_ID);

        // Act / Assert
        assertThatThrownBy(() -> newService().createAdministrativeRelease(
                ADMIN_LOGIN, new AdministrativeReleaseRequest(EMP_ID, SPACE_ID, PAST, REASON)))
                .isInstanceOf(ReleaseDateInPastException.class);
        verify(releaseRepository, never()).saveAndFlush(any());
    }

    // ---- Cancelacion: BOLA + fecha ----

    @Test
    void shouldCancelRelease_whenFutureAndOwn() {
        // Arrange
        Release future = Release.voluntary(SPACE_ID, EMP_ID, FUTURE, NOW);
        given(releaseRepository.findById(RELEASE_ID)).willReturn(Optional.of(future));
        givenActor(EMP_LOGIN, EMP_ID);

        // Act
        newService().cancelRelease(RELEASE_ID, EMP_LOGIN);

        // Assert: borrado fisico + evento de cancelacion
        verify(releaseRepository).delete(future);
        verifyEventKind(ReleaseAuditEvent.Kind.CANCELLED);
    }

    @Test
    void shouldRejectWith403_whenCancellingReleaseOfAnotherEmployee() {
        // Arrange: la liberacion es del empleado 15; el solicitante resuelve a 99 (BOLA)
        given(releaseRepository.findById(RELEASE_ID))
                .willReturn(Optional.of(Release.voluntary(SPACE_ID, EMP_ID, FUTURE, NOW)));
        givenActor(OTHER_LOGIN, OTHER_ID);

        // Act / Assert
        assertThatThrownBy(() -> newService().cancelRelease(RELEASE_ID, OTHER_LOGIN))
                .isInstanceOf(AccessDeniedException.class);
        verify(releaseRepository, never()).delete(any());
    }

    @Test
    void shouldRejectWith409_whenCancellingPastRelease() {
        // Arrange: liberacion propia pero de fecha pasada
        given(releaseRepository.findById(RELEASE_ID))
                .willReturn(Optional.of(Release.voluntary(SPACE_ID, EMP_ID, PAST, NOW)));
        givenActor(EMP_LOGIN, EMP_ID);

        // Act / Assert
        assertThatThrownBy(() -> newService().cancelRelease(RELEASE_ID, EMP_LOGIN))
                .isInstanceOf(PastReleaseCancellationException.class);
        verify(releaseRepository, never()).delete(any());
    }

    @Test
    void shouldThrowNotFound_whenCancellingUnknownRelease() {
        // Arrange
        given(releaseRepository.findById(RELEASE_ID)).willReturn(Optional.empty());

        // Act / Assert
        assertThatThrownBy(() -> newService().cancelRelease(RELEASE_ID, EMP_LOGIN))
                .isInstanceOf(EntityNotFoundException.class);
        verify(releaseRepository, never()).delete(any());
    }

    @Test
    void shouldCancelTodayRelease_whenReleaseDateIsToday() {
        // Arrange (frontera: hoy no es pasado, se puede cancelar)
        Release todayRelease = Release.voluntary(SPACE_ID, EMP_ID, TODAY, NOW);
        given(releaseRepository.findById(RELEASE_ID)).willReturn(Optional.of(todayRelease));
        givenActor(EMP_LOGIN, EMP_ID);

        // Act
        newService().cancelRelease(RELEASE_ID, EMP_LOGIN);

        // Assert
        verify(releaseRepository).delete(todayRelease);
    }

    // ---- Listado propio ----

    @Test
    void shouldListOnlyOwnReleases_whenEmployeeRequestsMine() {
        // Arrange
        givenActor(EMP_LOGIN, EMP_ID);
        given(releaseRepository.findByEmployeeId(org.mockito.ArgumentMatchers.eq(EMP_ID), any()))
                .willReturn(org.springframework.data.domain.Page.empty());

        // Act
        newService().listMyReleases(EMP_LOGIN, org.springframework.data.domain.Pageable.unpaged());

        // Assert: la consulta se restringe al empleado de la sesion (BOLA implicita)
        verify(releaseRepository).findByEmployeeId(org.mockito.ArgumentMatchers.eq(EMP_ID), any());
    }

    // ---- Helpers ----

    private FixedAssignment assignment(Long resourceId) {
        FixedAssignment assignment = mock(FixedAssignment.class);
        given(assignment.getResourceId()).willReturn(resourceId);
        return assignment;
    }

    private void givenAssignmentsFor(Long employeeId, int dayOfWeek, FixedAssignment... assignments) {
        given(fixedAssignmentRepository.findByEmployeeIdAndDayOfWeekAndActiveTrue(employeeId, dayOfWeek))
                .willReturn(List.of(assignments));
    }

    private void givenActor(String login, Long id) {
        Employee actor = mock(Employee.class);
        given(actor.getId()).willReturn(id);
        given(employeeRepository.findByLogin(login)).willReturn(Optional.of(actor));
    }

    private void verifyEventKind(ReleaseAuditEvent.Kind kind) {
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().kind()).isEqualTo(kind);
    }
}
