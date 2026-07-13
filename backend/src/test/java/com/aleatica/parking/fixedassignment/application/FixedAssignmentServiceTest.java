package com.aleatica.parking.fixedassignment.application;

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
import com.aleatica.parking.fixedassignment.domain.FixedAssignment;
import com.aleatica.parking.fixedassignment.domain.FixedAssignmentRepositoryPort;
import com.aleatica.parking.fixedassignment.dto.FixedAssignmentPutRequest;
import com.aleatica.parking.fixedassignment.dto.FixedAssignmentResponse;
import com.aleatica.parking.notification.event.FixedAssignmentRevokedEvent;
import com.aleatica.parking.resource.ResourceResolvers;
import com.aleatica.parking.resource.ResourceType;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

/**
 * Tests unitarios de {@link FixedAssignmentService} con un <strong>fake in-memory del puerto</strong>
 * {@link FixedAssignmentRepositoryPort} (no un mock de Spring Data): reemplazo del conjunto de dias
 * (alta/revocacion/idempotencia), validacion de rango, revocacion con 404 y acotada por tipo, y
 * verificacion de pertenencia (BOLA). El servicio opera con el modelo de dominio; el resto de
 * colaboradores (repositorio de empleado, resolutor de recursos, publicador de eventos) se mockean.
 * No toca la base de datos.
 */
@ExtendWith(MockitoExtension.class)
class FixedAssignmentServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-20T10:15:30Z");
    private static final String ADMIN_LOGIN = "admin";
    private static final String EMP_LOGIN = "employee";
    private static final Long ADMIN_ID = 1L;
    private static final Long EMP_ID = 15L;
    private static final Long OTHER_ID = 99L;
    private static final Long SPACE_ID = 8L;

    private final InMemoryFixedAssignmentRepository fixedAssignmentRepository =
            new InMemoryFixedAssignmentRepository();

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ResourceResolvers resourceResolvers;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private final ClockPort clock = () -> NOW;

    private FixedAssignmentService newService() {
        return new FixedAssignmentService(
                fixedAssignmentRepository, employeeRepository, resourceResolvers,
                eventPublisher, clock);
    }

    @Test
    void shouldCreateAssignments_whenAdminSetsValidDays() {
        // Arrange
        givenEmployeeAndSpaceExist();
        givenActor(ADMIN_LOGIN, ADMIN_ID);

        // Act
        List<FixedAssignmentResponse> result = newService()
                .setAssignments(EMP_ID, request(1, 2, 3), ADMIN_LOGIN);

        // Assert: se crean 3 filas nuevas (dias 1,2,3) con el actor y el reloj fijo.
        assertThat(result).hasSize(3);
        assertThat(fixedAssignmentRepository.insertedCount()).isEqualTo(3);
        assertThat(fixedAssignmentRepository.activeFor(EMP_ID))
                .allSatisfy(a -> {
                    assertThat(a.getCreatedById()).isEqualTo(ADMIN_ID);
                    assertThat(a.getCreatedAt()).isEqualTo(NOW);
                    assertThat(a.getResourceId()).isEqualTo(SPACE_ID);
                    assertThat(a.isActive()).isTrue();
                });
    }

    @Test
    void shouldThrow_whenDayOfWeekOutOfRange() {
        // Act / Assert: dia 8 fuera de rango -> excepcion de validacion, sin tocar BD
        assertThatThrownBy(() -> newService().setAssignments(EMP_ID, request(1, 8), ADMIN_LOGIN))
                .isInstanceOf(InvalidDayOfWeekException.class);
        assertThat(fixedAssignmentRepository.saveInvocations()).isZero();
    }

    @Test
    void shouldThrow_whenDayOfWeekBelowRange() {
        // Act / Assert: dia 0 por debajo del rango minimo (1)
        assertThatThrownBy(() -> newService().setAssignments(EMP_ID, request(0), ADMIN_LOGIN))
                .isInstanceOf(InvalidDayOfWeekException.class);
        assertThat(fixedAssignmentRepository.saveInvocations()).isZero();
    }

    @Test
    void shouldRevokeRemovedDay_whenPutShrinksTheSet() {
        // Arrange: el empleado ya tiene los dias 1,2,3 en la plaza; el PUT deja 1,2
        givenEmployeeAndSpaceExist();
        givenActor(ADMIN_LOGIN, ADMIN_ID);
        seededActive(101L, 1, ResourceType.PARKING);
        seededActive(102L, 2, ResourceType.PARKING);
        FixedAssignment day3 = seededActive(103L, 3, ResourceType.PARKING);

        // Act
        newService().setAssignments(EMP_ID, request(1, 2), ADMIN_LOGIN);

        // Assert: el dia 3 queda revocado logicamente (active=false + autor/fecha)
        assertThat(day3.isActive()).isFalse();
        assertThat(day3.getRevokedById()).isEqualTo(ADMIN_ID);
        assertThat(day3.getRevokedAt()).isEqualTo(NOW);
        assertThat(fixedAssignmentRepository.insertedCount()).isZero();
    }

    @Test
    void shouldBeIdempotent_whenPutRepeatsSameSpaceAndDays() {
        // Arrange: el conjunto solicitado coincide con el vigente
        givenEmployeeAndSpaceExist();
        givenActor(ADMIN_LOGIN, ADMIN_ID);
        seededActive(101L, 1, ResourceType.PARKING);
        seededActive(102L, 2, ResourceType.PARKING);

        // Act
        newService().setAssignments(EMP_ID, request(1, 2), ADMIN_LOGIN);

        // Assert: no se crea ninguna fila nueva y siguen activos los dos dias
        assertThat(fixedAssignmentRepository.insertedCount()).isZero();
        assertThat(fixedAssignmentRepository.activeFor(EMP_ID)).hasSize(2);
    }

    @Test
    void shouldRevokeAll_whenAdminDeletesActiveAssignments() {
        // Arrange
        givenActor(ADMIN_LOGIN, ADMIN_ID);
        FixedAssignment a1 = seededActive(101L, 1, ResourceType.PARKING);
        FixedAssignment a2 = seededActive(102L, 2, ResourceType.PARKING);

        // Act
        newService().revoke(EMP_ID, ADMIN_LOGIN);

        // Assert
        assertThat(a1.isActive()).isFalse();
        assertThat(a2.isActive()).isFalse();
        assertThat(a1.getRevokedById()).isEqualTo(ADMIN_ID);
    }

    @Test
    void shouldPublishRevokedEvent_whenAdminRevokesAssignments() {
        // Arrange
        givenActor(ADMIN_LOGIN, ADMIN_ID);
        seededActive(101L, 1, ResourceType.PARKING);

        // Act
        newService().revoke(EMP_ID, ADMIN_LOGIN);

        // Assert: se notifica al empleado afectado (un unico evento con su id)
        verify(eventPublisher).publishEvent(new FixedAssignmentRevokedEvent(EMP_ID));
    }

    @Test
    void shouldRevokeOnlyRequestedType_whenResourceTypeGiven() {
        // Arrange: el empleado tiene puesto (DESK) y plaza (PARKING) activos; se revoca solo DESK
        givenActor(ADMIN_LOGIN, ADMIN_ID);
        FixedAssignment desk = seededActive(201L, 1, ResourceType.DESK);
        FixedAssignment parking = seededActive(202L, 1, ResourceType.PARKING);

        // Act
        newService().revoke(EMP_ID, ResourceType.DESK, ADMIN_LOGIN);

        // Assert: solo se revoca el DESK; NUNCA se consulta la vista de todos los tipos
        // (la plaza del empleado queda intacta) y se notifica una vez.
        assertThat(desk.isActive()).isFalse();
        assertThat(desk.getRevokedById()).isEqualTo(ADMIN_ID);
        assertThat(parking.isActive()).isTrue();
        assertThat(fixedAssignmentRepository.allTypesQueried()).isFalse();
        verify(eventPublisher).publishEvent(new FixedAssignmentRevokedEvent(EMP_ID));
    }

    @Test
    void shouldThrowNotFound_whenRevokingTypeWithoutActiveOfThatType() {
        // Arrange: el empleado no tiene ninguna asignacion activa del tipo solicitado
        // (store vacio de DESK)

        // Act / Assert
        assertThatThrownBy(() -> newService().revoke(EMP_ID, ResourceType.DESK, ADMIN_LOGIN))
                .isInstanceOf(EntityNotFoundException.class);

        // Assert: sin filas del tipo no se revoca ni se notifica nada
        assertThat(fixedAssignmentRepository.saveInvocations()).isZero();
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowNotFound_whenRevokingEmployeeWithoutActiveAssignment() {
        // Arrange: store vacio

        // Act / Assert
        assertThatThrownBy(() -> newService().revoke(EMP_ID, ADMIN_LOGIN))
                .isInstanceOf(EntityNotFoundException.class);

        // Assert: sin asignacion activa no se notifica nada
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldReturnOwnAssignments_whenEmployeeQueriesSelf() {
        // Arrange
        givenActor(EMP_LOGIN, EMP_ID);
        seededActive(101L, 1, ResourceType.PARKING);

        // Act
        List<FixedAssignmentResponse> result =
                newService().getEmployeeAssignments(EMP_ID, EMP_LOGIN, false);

        // Assert
        assertThat(result).hasSize(1);
    }

    @Test
    void shouldThrowForbidden_whenEmployeeQueriesOtherEmployee() {
        // Arrange: el solicitante (id 15) intenta ver las del empleado 99
        givenActor(EMP_LOGIN, EMP_ID);

        // Act / Assert (BOLA)
        assertThatThrownBy(() -> newService().getEmployeeAssignments(OTHER_ID, EMP_LOGIN, false))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(fixedAssignmentRepository.allTypesQueried()).isFalse();
    }

    @Test
    void shouldThrowNotFound_whenSettingForUnknownEmployee() {
        // Arrange: el empleado del path no existe
        given(employeeRepository.existsById(EMP_ID)).willReturn(false);

        // Act / Assert
        assertThatThrownBy(() -> newService().setAssignments(EMP_ID, request(1), ADMIN_LOGIN))
                .isInstanceOf(EntityNotFoundException.class);
        assertThat(fixedAssignmentRepository.saveInvocations()).isZero();
    }

    @Test
    void shouldThrowNotFound_whenSettingWithUnknownSpace() {
        // Arrange: empleado valido, plaza inexistente
        given(employeeRepository.existsById(EMP_ID)).willReturn(true);
        given(resourceResolvers.exists(SPACE_ID, ResourceType.PARKING)).willReturn(false);

        // Act / Assert
        assertThatThrownBy(() -> newService().setAssignments(EMP_ID, request(1), ADMIN_LOGIN))
                .isInstanceOf(EntityNotFoundException.class);
        assertThat(fixedAssignmentRepository.saveInvocations()).isZero();
    }

    @Test
    void shouldThrow_whenDayOfWeekIsNull() {
        // Act / Assert: un elemento nulo en la lista de dias
        assertThatThrownBy(() -> newService().setAssignments(
                EMP_ID, new FixedAssignmentPutRequest(SPACE_ID, java.util.Arrays.asList(1, null), null), ADMIN_LOGIN))
                .isInstanceOf(InvalidDayOfWeekException.class);
        assertThat(fixedAssignmentRepository.saveInvocations()).isZero();
    }

    @Test
    void shouldThrowNotFound_whenSessionUserNoLongerExists() {
        // Arrange: el principal de la sesion ya no resuelve a un empleado
        given(employeeRepository.findByLogin(EMP_LOGIN)).willReturn(Optional.empty());

        // Act / Assert (verificacion de pertenencia con actor inexistente)
        assertThatThrownBy(() -> newService().getEmployeeAssignments(EMP_ID, EMP_LOGIN, false))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void shouldReturnAnyAssignments_whenAdminQueriesWithoutOwnershipCheck() {
        // Arrange: ADMIN no resuelve pertenencia; consulta directa
        seededActiveFor(OTHER_ID, 301L, 1, ResourceType.PARKING);

        // Act
        List<FixedAssignmentResponse> result =
                newService().getEmployeeAssignments(OTHER_ID, ADMIN_LOGIN, true);

        // Assert: no consulta el repositorio de empleados para resolver pertenencia
        assertThat(result).hasSize(1);
        verify(employeeRepository, never()).findByLogin(any());
    }

    private void givenEmployeeAndSpaceExist() {
        given(employeeRepository.existsById(EMP_ID)).willReturn(true);
        given(resourceResolvers.exists(SPACE_ID, ResourceType.PARKING)).willReturn(true);
    }

    private void givenActor(String login, Long id) {
        Employee actor = mock(Employee.class);
        given(actor.getId()).willReturn(id);
        given(employeeRepository.findByLogin(login)).willReturn(Optional.of(actor));
    }

    private FixedAssignment seededActive(long id, int day, ResourceType type) {
        return seededActiveFor(EMP_ID, id, day, type);
    }

    private FixedAssignment seededActiveFor(Long employeeId, long id, int day, ResourceType type) {
        FixedAssignment fa = FixedAssignment.restore(
                id, SPACE_ID, type, employeeId, day, true, ADMIN_ID, NOW, null, null);
        fixedAssignmentRepository.seed(fa);
        return fa;
    }

    private FixedAssignmentPutRequest request(Integer... days) {
        return new FixedAssignmentPutRequest(SPACE_ID, List.of(days), null);
    }

    /**
     * Fake in-memory del puerto de persistencia de asignaciones fijas: sustituye a un mock de
     * Spring Data en los tests unitarios del servicio. {@link #seed(FixedAssignment)} precarga
     * estado (con id) sin contar como escritura; los finder derivan de las filas del store y
     * devuelven las referencias vivas (de modo que la revocacion en memoria del servicio se
     * observa en el objeto sembrado); {@code saveAll}/{@code saveAllAndFlush} asignan id a las
     * altas (id nulo) y registran la invocacion para verificar los caminos que NO deben persistir.
     */
    private static final class InMemoryFixedAssignmentRepository
            implements FixedAssignmentRepositoryPort {

        private final List<FixedAssignment> store = new ArrayList<>();
        private long sequence = 1000L;
        private int saveInvocations;
        private int insertedCount;
        private boolean allTypesQueried;

        void seed(FixedAssignment assignment) {
            store.add(assignment);
        }

        int saveInvocations() {
            return saveInvocations;
        }

        int insertedCount() {
            return insertedCount;
        }

        boolean allTypesQueried() {
            return allTypesQueried;
        }

        List<FixedAssignment> activeFor(Long employeeId) {
            return store.stream()
                    .filter(FixedAssignment::isActive)
                    .filter(a -> employeeId.equals(a.getEmployeeId()))
                    .toList();
        }

        @Override
        public Page<FixedAssignment> findByActiveTrue(Pageable pageable) {
            return new PageImpl<>(store.stream().filter(FixedAssignment::isActive).toList());
        }

        @Override
        public List<FixedAssignment> findByEmployeeIdAndActiveTrueOrderByDayOfWeekAsc(Long employeeId) {
            allTypesQueried = true;
            return store.stream()
                    .filter(FixedAssignment::isActive)
                    .filter(a -> employeeId.equals(a.getEmployeeId()))
                    .sorted(java.util.Comparator.comparing(FixedAssignment::getDayOfWeek))
                    .toList();
        }

        @Override
        public List<FixedAssignment> findByEmployeeIdAndResourceTypeAndActiveTrueOrderByDayOfWeekAsc(
                Long employeeId, ResourceType resourceType) {
            return store.stream()
                    .filter(FixedAssignment::isActive)
                    .filter(a -> employeeId.equals(a.getEmployeeId()))
                    .filter(a -> resourceType == a.getResourceType())
                    .sorted(java.util.Comparator.comparing(FixedAssignment::getDayOfWeek))
                    .toList();
        }

        @Override
        public List<FixedAssignment> findByEmployeeIdAndResourceIdAndResourceTypeAndActiveTrue(
                Long employeeId, Long resourceId, ResourceType resourceType) {
            return store.stream()
                    .filter(FixedAssignment::isActive)
                    .filter(a -> employeeId.equals(a.getEmployeeId()))
                    .filter(a -> resourceId.equals(a.getResourceId()))
                    .filter(a -> resourceType == a.getResourceType())
                    .toList();
        }

        @Override
        public List<FixedAssignment> saveAll(List<FixedAssignment> assignments) {
            return upsert(assignments);
        }

        @Override
        public List<FixedAssignment> saveAllAndFlush(List<FixedAssignment> assignments) {
            return upsert(assignments);
        }

        private List<FixedAssignment> upsert(List<FixedAssignment> assignments) {
            saveInvocations++;
            List<FixedAssignment> result = new ArrayList<>();
            for (FixedAssignment assignment : assignments) {
                FixedAssignment persisted = assignment;
                if (assignment.getId() == null) {
                    persisted = withId(assignment, ++sequence);
                    insertedCount++;
                    store.add(persisted);
                }
                result.add(persisted);
            }
            return result;
        }

        private static FixedAssignment withId(FixedAssignment assignment, long id) {
            return FixedAssignment.restore(
                    id, assignment.getResourceId(), assignment.getResourceType(),
                    assignment.getEmployeeId(), assignment.getDayOfWeek(), assignment.isActive(),
                    assignment.getCreatedById(), assignment.getCreatedAt(),
                    assignment.getRevokedById(), assignment.getRevokedAt());
        }
    }
}
