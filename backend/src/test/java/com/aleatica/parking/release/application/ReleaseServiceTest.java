package com.aleatica.parking.release.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.aleatica.parking.auth.domain.ClockPort;
import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.fixedassignment.infrastructure.FixedAssignmentEntity;
import com.aleatica.parking.fixedassignment.infrastructure.FixedAssignmentJpaRepository;
import com.aleatica.parking.release.domain.Release;
import com.aleatica.parking.release.domain.ReleaseRepositoryPort;
import com.aleatica.parking.release.domain.ReleaseType;
import com.aleatica.parking.release.dto.AdministrativeReleaseRequest;
import com.aleatica.parking.release.dto.ReleaseCreateRequest;
import com.aleatica.parking.release.dto.ReleaseResponse;
import com.aleatica.parking.resource.ResourceResolvers;
import com.aleatica.parking.resource.ResourceType;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
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
 * Tests unitarios de {@link ReleaseService} con un <strong>fake in-memory del puerto</strong>
 * {@link ReleaseRepositoryPort} (no un mock de Spring Data): ventana temporal, resolucion de
 * plaza fija, unicidad recurso+fecha, verificacion de pertenencia (BOLA) en cancelacion y
 * validacion de la liberacion administrativa. El servicio opera con el modelo de dominio; el
 * resto de colaboradores (repositorios de empleado, resolutor de recursos, publicador de eventos)
 * se mockean. No toca la base de datos.
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

    private final InMemoryReleaseRepository releaseRepository = new InMemoryReleaseRepository();

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ResourceResolvers resourceResolvers;

    @Mock
    private FixedAssignmentJpaRepository fixedAssignmentRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<ReleaseAuditEvent> eventCaptor;

    private final ClockPort clock = () -> NOW;

    private ReleaseService newService() {
        return new ReleaseService(
                releaseRepository, employeeRepository, resourceResolvers,
                fixedAssignmentRepository, eventPublisher, clock);
    }

    // ---- Liberacion voluntaria: ventana + resolucion de plaza + unicidad ----

    @Test
    void shouldCreateVoluntaryRelease_whenOwnerAndFutureDate() {
        // Arrange: una sola asignacion fija activa ese dia -> plaza resuelta implicitamente
        givenActor(EMP_LOGIN, EMP_ID);
        givenAssignmentsFor(EMP_ID, FUTURE_DOW, assignment(SPACE_ID));

        // Act
        ReleaseResponse result =
                newService().createRelease(EMP_LOGIN, new ReleaseCreateRequest(FUTURE, null, null));

        // Assert: VOLUNTARY, titular = ejecutor, reason nulo, plaza resuelta, marca de tiempo
        assertThat(result.type()).isEqualTo(ReleaseType.VOLUNTARY);
        assertThat(result.employeeId()).isEqualTo(EMP_ID);
        assertThat(result.releasedById()).isEqualTo(EMP_ID);
        assertThat(result.reason()).isNull();
        assertThat(result.parkingSpaceId()).isEqualTo(SPACE_ID);
        assertThat(result.createdAt()).isEqualTo(NOW);
        assertThat(releaseRepository.saves()).isEqualTo(1);
        verifyEventKind(ReleaseAuditEvent.Kind.VOLUNTARY_RELEASED);
    }

    @Test
    void shouldCreateVoluntaryRelease_whenReleaseDateIsToday() {
        // Arrange (frontera inferior inclusive: hoy cuenta como futuro inmediato)
        givenActor(EMP_LOGIN, EMP_ID);
        givenAssignmentsFor(EMP_ID, TODAY.getDayOfWeek().getValue(), assignment(SPACE_ID));

        // Act / Assert
        assertThat(newService().createRelease(EMP_LOGIN, new ReleaseCreateRequest(TODAY, null, null)).type())
                .isEqualTo(ReleaseType.VOLUNTARY);
    }

    @Test
    void shouldResolveExplicitSpace_whenOwnedByEmployeeThatDay() {
        // Arrange: dos asignaciones ese dia; se libera la explicita
        givenActor(EMP_LOGIN, EMP_ID);
        givenAssignmentsFor(EMP_ID, FUTURE_DOW, assignment(SPACE_ID), assignment(OTHER_SPACE_ID));

        // Act
        ReleaseResponse result =
                newService().createRelease(EMP_LOGIN, new ReleaseCreateRequest(FUTURE, OTHER_SPACE_ID, null));

        // Assert
        assertThat(result.parkingSpaceId()).isEqualTo(OTHER_SPACE_ID);
    }

    @Test
    void shouldRejectWith400_whenReleaseDateInPast() {
        // Arrange
        givenActor(EMP_LOGIN, EMP_ID);

        // Act / Assert
        assertThatThrownBy(() -> newService().createRelease(EMP_LOGIN, new ReleaseCreateRequest(PAST, null, null)))
                .isInstanceOf(ReleaseDateInPastException.class);
        assertThat(releaseRepository.saves()).isZero();
    }

    @Test
    void shouldRejectWith409_whenNoFixedAssignmentForThatDay() {
        // Arrange: sin asignaciones activas ese dia
        givenActor(EMP_LOGIN, EMP_ID);
        givenAssignmentsFor(EMP_ID, FUTURE_DOW);

        // Act / Assert
        assertThatThrownBy(() -> newService().createRelease(EMP_LOGIN, new ReleaseCreateRequest(FUTURE, null, null)))
                .isInstanceOf(NoFixedAssignmentException.class);
        assertThat(releaseRepository.saves()).isZero();
    }

    @Test
    void shouldRejectWith409_whenAmbiguousImplicitResolution() {
        // Arrange: dos asignaciones ese dia y no se indica la plaza -> ambiguo (la plaza
        // concreta no se llega a leer: la ambiguedad se detecta por el numero de filas)
        givenActor(EMP_LOGIN, EMP_ID);
        givenAssignmentsFor(EMP_ID, FUTURE_DOW,
                mock(FixedAssignmentEntity.class),
                mock(FixedAssignmentEntity.class));

        // Act / Assert
        assertThatThrownBy(() -> newService().createRelease(EMP_LOGIN, new ReleaseCreateRequest(FUTURE, null, null)))
                .isInstanceOf(NoFixedAssignmentException.class);
        assertThat(releaseRepository.saves()).isZero();
    }

    @Test
    void shouldRejectWith409_whenExplicitSpaceNotOwnedThatDay() {
        // Arrange: la plaza explicita no pertenece a ninguna asignacion del empleado ese dia
        givenActor(EMP_LOGIN, EMP_ID);
        givenAssignmentsFor(EMP_ID, FUTURE_DOW, assignment(SPACE_ID));

        // Act / Assert
        assertThatThrownBy(() -> newService()
                .createRelease(EMP_LOGIN, new ReleaseCreateRequest(FUTURE, OTHER_SPACE_ID, null)))
                .isInstanceOf(NoFixedAssignmentException.class);
        assertThat(releaseRepository.saves()).isZero();
    }

    @Test
    void shouldRejectWith409_whenResourceAlreadyReleasedForDate() {
        // Arrange: el recurso ya tiene una liberacion para esa fecha (comprobacion previa)
        givenActor(EMP_LOGIN, EMP_ID);
        givenAssignmentsFor(EMP_ID, FUTURE_DOW, assignment(SPACE_ID));
        releaseRepository.seed(releaseWithId(RELEASE_ID, SPACE_ID, EMP_ID, FUTURE));

        // Act / Assert
        assertThatThrownBy(() -> newService().createRelease(EMP_LOGIN, new ReleaseCreateRequest(FUTURE, null, null)))
                .isInstanceOf(ResourceAlreadyReleasedException.class);
        assertThat(releaseRepository.saves()).isZero();
    }

    @Test
    void shouldThrowNotFound_whenSessionUserNoLongerExists() {
        // Arrange: el principal de la sesion ya no resuelve a un empleado
        given(employeeRepository.findByLogin(EMP_LOGIN)).willReturn(Optional.empty());

        // Act / Assert
        assertThatThrownBy(() -> newService().createRelease(EMP_LOGIN, new ReleaseCreateRequest(FUTURE, null, null)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ---- Liberacion administrativa ----

    @Test
    void shouldCreateAdministrativeRelease_whenAdminAndReasonPresent() {
        // Arrange
        givenActor(ADMIN_LOGIN, ADMIN_ID);
        given(employeeRepository.existsById(EMP_ID)).willReturn(true);
        given(resourceResolvers.exists(SPACE_ID, ResourceType.PARKING)).willReturn(true);
        givenAssignmentsFor(EMP_ID, FUTURE_DOW, assignment(SPACE_ID));

        // Act
        ReleaseResponse result = newService().createAdministrativeRelease(
                ADMIN_LOGIN, new AdministrativeReleaseRequest(EMP_ID, SPACE_ID, FUTURE, REASON, null));

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
                ADMIN_LOGIN, new AdministrativeReleaseRequest(EMP_ID, SPACE_ID, FUTURE, REASON, null)))
                .isInstanceOf(EntityNotFoundException.class);
        assertThat(releaseRepository.saves()).isZero();
    }

    @Test
    void shouldThrowNotFound_whenAdministrativeSpaceUnknown() {
        // Arrange
        givenActor(ADMIN_LOGIN, ADMIN_ID);
        given(employeeRepository.existsById(EMP_ID)).willReturn(true);
        given(resourceResolvers.exists(SPACE_ID, ResourceType.PARKING)).willReturn(false);

        // Act / Assert
        assertThatThrownBy(() -> newService().createAdministrativeRelease(
                ADMIN_LOGIN, new AdministrativeReleaseRequest(EMP_ID, SPACE_ID, FUTURE, REASON, null)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void shouldThrowNoFixedAssignment_whenAdministrativeSpaceNotAssignedThatDay() {
        // Arrange: el recurso no tiene una asignacion fija activa del empleado ese dia
        givenActor(ADMIN_LOGIN, ADMIN_ID);
        given(employeeRepository.existsById(EMP_ID)).willReturn(true);
        given(resourceResolvers.exists(SPACE_ID, ResourceType.PARKING)).willReturn(true);
        givenAssignmentsFor(EMP_ID, FUTURE_DOW, assignment(OTHER_SPACE_ID));

        // Act / Assert
        assertThatThrownBy(() -> newService().createAdministrativeRelease(
                ADMIN_LOGIN, new AdministrativeReleaseRequest(EMP_ID, SPACE_ID, FUTURE, REASON, null)))
                .isInstanceOf(NoFixedAssignmentException.class);
    }

    @Test
    void shouldThrowAlreadyReleased_whenAdministrativeDuplicate() {
        // Arrange
        givenActor(ADMIN_LOGIN, ADMIN_ID);
        given(employeeRepository.existsById(EMP_ID)).willReturn(true);
        given(resourceResolvers.exists(SPACE_ID, ResourceType.PARKING)).willReturn(true);
        givenAssignmentsFor(EMP_ID, FUTURE_DOW, assignment(SPACE_ID));
        releaseRepository.seed(releaseWithId(RELEASE_ID, SPACE_ID, EMP_ID, FUTURE));

        // Act / Assert
        assertThatThrownBy(() -> newService().createAdministrativeRelease(
                ADMIN_LOGIN, new AdministrativeReleaseRequest(EMP_ID, SPACE_ID, FUTURE, REASON, null)))
                .isInstanceOf(ResourceAlreadyReleasedException.class);
    }

    @Test
    void shouldThrowDateInPast_whenAdministrativeReleaseDateBeforeToday() {
        // Arrange
        givenActor(ADMIN_LOGIN, ADMIN_ID);

        // Act / Assert
        assertThatThrownBy(() -> newService().createAdministrativeRelease(
                ADMIN_LOGIN, new AdministrativeReleaseRequest(EMP_ID, SPACE_ID, PAST, REASON, null)))
                .isInstanceOf(ReleaseDateInPastException.class);
        assertThat(releaseRepository.saves()).isZero();
    }

    // ---- Cancelacion: BOLA + fecha ----

    @Test
    void shouldCancelRelease_whenFutureAndOwn() {
        // Arrange
        releaseRepository.seed(releaseWithId(RELEASE_ID, SPACE_ID, EMP_ID, FUTURE));
        givenActor(EMP_LOGIN, EMP_ID);

        // Act
        newService().cancelRelease(RELEASE_ID, EMP_LOGIN);

        // Assert: borrado fisico + evento de cancelacion
        assertThat(releaseRepository.wasDeleted(RELEASE_ID)).isTrue();
        verifyEventKind(ReleaseAuditEvent.Kind.CANCELLED);
    }

    @Test
    void shouldRejectWith403_whenCancellingReleaseOfAnotherEmployee() {
        // Arrange: la liberacion es del empleado 15; el solicitante resuelve a 99 (BOLA)
        releaseRepository.seed(releaseWithId(RELEASE_ID, SPACE_ID, EMP_ID, FUTURE));
        givenActor(OTHER_LOGIN, OTHER_ID);

        // Act / Assert
        assertThatThrownBy(() -> newService().cancelRelease(RELEASE_ID, OTHER_LOGIN))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(releaseRepository.wasDeleted(RELEASE_ID)).isFalse();
    }

    @Test
    void shouldRejectWith409_whenCancellingPastRelease() {
        // Arrange: liberacion propia pero de fecha pasada
        releaseRepository.seed(releaseWithId(RELEASE_ID, SPACE_ID, EMP_ID, PAST));
        givenActor(EMP_LOGIN, EMP_ID);

        // Act / Assert
        assertThatThrownBy(() -> newService().cancelRelease(RELEASE_ID, EMP_LOGIN))
                .isInstanceOf(PastReleaseCancellationException.class);
        assertThat(releaseRepository.wasDeleted(RELEASE_ID)).isFalse();
    }

    @Test
    void shouldThrowNotFound_whenCancellingUnknownRelease() {
        // Arrange (store vacio)

        // Act / Assert
        assertThatThrownBy(() -> newService().cancelRelease(RELEASE_ID, EMP_LOGIN))
                .isInstanceOf(EntityNotFoundException.class);
        assertThat(releaseRepository.wasDeleted(RELEASE_ID)).isFalse();
    }

    @Test
    void shouldCancelTodayRelease_whenReleaseDateIsToday() {
        // Arrange (frontera: hoy no es pasado, se puede cancelar)
        releaseRepository.seed(releaseWithId(RELEASE_ID, SPACE_ID, EMP_ID, TODAY));
        givenActor(EMP_LOGIN, EMP_ID);

        // Act
        newService().cancelRelease(RELEASE_ID, EMP_LOGIN);

        // Assert
        assertThat(releaseRepository.wasDeleted(RELEASE_ID)).isTrue();
    }

    // ---- Listado propio ----

    @Test
    void shouldListOnlyOwnReleases_whenEmployeeRequestsMine() {
        // Arrange: dos liberaciones propias y una ajena en el store
        givenActor(EMP_LOGIN, EMP_ID);
        releaseRepository.seed(releaseWithId(1L, SPACE_ID, EMP_ID, FUTURE));
        releaseRepository.seed(releaseWithId(2L, OTHER_SPACE_ID, EMP_ID, TODAY));
        releaseRepository.seed(releaseWithId(3L, SPACE_ID, OTHER_ID, FUTURE));

        // Act
        var page = newService().listMyReleases(EMP_LOGIN, Pageable.unpaged());

        // Assert: la consulta se restringe al empleado de la sesion (BOLA implicita)
        assertThat(page.content()).extracting(ReleaseResponse::employeeId).containsOnly(EMP_ID);
        assertThat(page.content()).hasSize(2);
    }

    // ---- Helpers ----

    private FixedAssignmentEntity assignment(Long resourceId) {
        FixedAssignmentEntity assignment = mock(FixedAssignmentEntity.class);
        given(assignment.getResourceId()).willReturn(resourceId);
        return assignment;
    }

    private void givenAssignmentsFor(Long employeeId, int dayOfWeek, FixedAssignmentEntity... assignments) {
        given(fixedAssignmentRepository.findByEmployeeIdAndResourceTypeAndDayOfWeekAndActiveTrue(employeeId, ResourceType.PARKING, dayOfWeek))
                .willReturn(List.of(assignments));
    }

    private void givenActor(String login, Long id) {
        Employee actor = mock(Employee.class);
        given(actor.getId()).willReturn(id);
        given(employeeRepository.findByLogin(login)).willReturn(Optional.of(actor));
    }

    private static Release releaseWithId(Long id, Long resourceId, Long employeeId, LocalDate date) {
        return Release.restore(
                id, resourceId, ResourceType.PARKING, employeeId, date, ReleaseType.VOLUNTARY,
                null, employeeId, NOW);
    }

    private void verifyEventKind(ReleaseAuditEvent.Kind kind) {
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().kind()).isEqualTo(kind);
    }

    /**
     * Fake in-memory del puerto de persistencia de liberaciones: sustituye a un mock de Spring
     * Data en los tests unitarios del servicio. {@link #seed(Release)} precarga estado sin contar
     * como escritura; {@link #saveAndFlush} asigna id (si falta) y cuenta la invocacion para
     * verificar los caminos que NO deben persistir; {@link #delete} registra el borrado y
     * {@link #existsByResourceIdAndResourceTypeAndReleaseDate} deriva de las filas del store.
     */
    private static final class InMemoryReleaseRepository implements ReleaseRepositoryPort {

        private final Map<Long, Release> store = new HashMap<>();
        private final List<Long> deletedIds = new ArrayList<>();
        private long sequence = 1000L;
        private int saves;

        void seed(Release release) {
            store.put(release.getId(), release);
        }

        int saves() {
            return saves;
        }

        boolean wasDeleted(Long id) {
            return deletedIds.contains(id);
        }

        @Override
        public Optional<Release> findById(Long id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Release saveAndFlush(Release release) {
            saves++;
            Long id = release.getId() != null ? release.getId() : ++sequence;
            Release stored = Release.restore(
                    id, release.getResourceId(), release.getResourceType(), release.getEmployeeId(),
                    release.getReleaseDate(), release.getType(), release.getReason(),
                    release.getReleasedById(), release.getCreatedAt());
            store.put(id, stored);
            return stored;
        }

        @Override
        public void delete(Release release) {
            deletedIds.add(release.getId());
            store.remove(release.getId());
        }

        @Override
        public boolean existsByResourceIdAndResourceTypeAndReleaseDate(
                Long resourceId, ResourceType resourceType, LocalDate releaseDate) {
            return store.values().stream().anyMatch(r ->
                    resourceId.equals(r.getResourceId())
                            && resourceType == r.getResourceType()
                            && releaseDate.equals(r.getReleaseDate()));
        }

        @Override
        public Page<Release> findByEmployeeId(Long employeeId, Pageable pageable) {
            return new PageImpl<>(store.values().stream()
                    .filter(r -> employeeId.equals(r.getEmployeeId()))
                    .toList());
        }
    }
}
