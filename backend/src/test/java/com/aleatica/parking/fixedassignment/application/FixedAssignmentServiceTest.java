package com.aleatica.parking.fixedassignment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.aleatica.parking.auth.domain.ClockPort;
import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.fixedassignment.FixedAssignment;
import com.aleatica.parking.fixedassignment.FixedAssignmentRepository;
import com.aleatica.parking.resource.ResourceResolvers;
import com.aleatica.parking.resource.ResourceType;
import com.aleatica.parking.fixedassignment.dto.FixedAssignmentPutRequest;
import com.aleatica.parking.fixedassignment.dto.FixedAssignmentResponse;
import com.aleatica.parking.notification.event.FixedAssignmentRevokedEvent;
import com.aleatica.parking.parkingspace.ParkingSpaceRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
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
 * Tests unitarios de {@link FixedAssignmentService} con repositorios mockeados:
 * reemplazo del conjunto de dias (alta/revocacion/idempotencia), validacion de rango,
 * revocacion con 404, y verificacion de pertenencia (BOLA). No toca la base de datos.
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

    @Mock
    private FixedAssignmentRepository fixedAssignmentRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ResourceResolvers resourceResolvers;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<List<FixedAssignment>> savedCaptor;

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
        given(fixedAssignmentRepository
                .findByEmployeeIdAndResourceIdAndResourceTypeAndActiveTrue(EMP_ID, SPACE_ID, ResourceType.PARKING))
                .willReturn(List.of());
        given(fixedAssignmentRepository.findByEmployeeIdAndActiveTrueOrderByDayOfWeekAsc(EMP_ID))
                .willReturn(List.of(
                        active(1), active(2), active(3)));

        // Act
        List<FixedAssignmentResponse> result = newService()
                .setAssignments(EMP_ID, request(1, 2, 3), ADMIN_LOGIN);

        // Assert: se crean 3 filas nuevas (dias 1,2,3) con el actor y el reloj fijo.
        // saveAll se invoca dos veces (revocaciones, luego altas); la 2a es el alta.
        assertThat(result).hasSize(3);
        verify(fixedAssignmentRepository, org.mockito.Mockito.times(2)).saveAll(savedCaptor.capture());
        List<FixedAssignment> created = savedCaptor.getAllValues().get(1);
        assertThat(created).hasSize(3)
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
        verify(fixedAssignmentRepository, never()).saveAll(any());
    }

    @Test
    void shouldThrow_whenDayOfWeekBelowRange() {
        // Act / Assert: dia 0 por debajo del rango minimo (1)
        assertThatThrownBy(() -> newService().setAssignments(EMP_ID, request(0), ADMIN_LOGIN))
                .isInstanceOf(InvalidDayOfWeekException.class);
        verify(fixedAssignmentRepository, never()).saveAll(any());
    }

    @Test
    void shouldRevokeRemovedDay_whenPutShrinksTheSet() {
        // Arrange: el empleado ya tiene los dias 1,2,3 en la plaza; el PUT deja 1,2
        givenEmployeeAndSpaceExist();
        givenActor(ADMIN_LOGIN, ADMIN_ID);
        FixedAssignment day3 = active(3);
        given(fixedAssignmentRepository
                .findByEmployeeIdAndResourceIdAndResourceTypeAndActiveTrue(EMP_ID, SPACE_ID, ResourceType.PARKING))
                .willReturn(List.of(active(1), active(2), day3));
        given(fixedAssignmentRepository.findByEmployeeIdAndActiveTrueOrderByDayOfWeekAsc(EMP_ID))
                .willReturn(List.of(active(1), active(2)));

        // Act
        newService().setAssignments(EMP_ID, request(1, 2), ADMIN_LOGIN);

        // Assert: el dia 3 queda revocado logicamente (active=false + autor/fecha)
        assertThat(day3.isActive()).isFalse();
        assertThat(day3.getRevokedById()).isEqualTo(ADMIN_ID);
        assertThat(day3.getRevokedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldBeIdempotent_whenPutRepeatsSameSpaceAndDays() {
        // Arrange: el conjunto solicitado coincide con el vigente
        givenEmployeeAndSpaceExist();
        givenActor(ADMIN_LOGIN, ADMIN_ID);
        given(fixedAssignmentRepository
                .findByEmployeeIdAndResourceIdAndResourceTypeAndActiveTrue(EMP_ID, SPACE_ID, ResourceType.PARKING))
                .willReturn(List.of(active(1), active(2)));
        given(fixedAssignmentRepository.findByEmployeeIdAndActiveTrueOrderByDayOfWeekAsc(EMP_ID))
                .willReturn(List.of(active(1), active(2)));

        // Act
        newService().setAssignments(EMP_ID, request(1, 2), ADMIN_LOGIN);

        // Assert: no se crea ninguna fila nueva (la 2a invocacion de saveAll es vacia)
        verify(fixedAssignmentRepository, org.mockito.Mockito.times(2)).saveAll(savedCaptor.capture());
        assertThat(savedCaptor.getAllValues().get(1)).isEmpty();
    }

    @Test
    void shouldRevokeAll_whenAdminDeletesActiveAssignments() {
        // Arrange
        givenActor(ADMIN_LOGIN, ADMIN_ID);
        FixedAssignment a1 = active(1);
        FixedAssignment a2 = active(2);
        given(fixedAssignmentRepository.findByEmployeeIdAndActiveTrueOrderByDayOfWeekAsc(EMP_ID))
                .willReturn(List.of(a1, a2));

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
        given(fixedAssignmentRepository.findByEmployeeIdAndActiveTrueOrderByDayOfWeekAsc(EMP_ID))
                .willReturn(List.of(active(1)));

        // Act
        newService().revoke(EMP_ID, ADMIN_LOGIN);

        // Assert: se notifica al empleado afectado (un unico evento con su id)
        verify(eventPublisher).publishEvent(new FixedAssignmentRevokedEvent(EMP_ID));
    }

    @Test
    void shouldRevokeOnlyRequestedType_whenResourceTypeGiven() {
        // Arrange: el empleado tiene puesto (DESK) activo; se revoca solo DESK
        givenActor(ADMIN_LOGIN, ADMIN_ID);
        FixedAssignment desk = activeDesk(1);
        given(fixedAssignmentRepository
                .findByEmployeeIdAndResourceTypeAndActiveTrueOrderByDayOfWeekAsc(EMP_ID, ResourceType.DESK))
                .willReturn(List.of(desk));

        // Act
        newService().revoke(EMP_ID, ResourceType.DESK, ADMIN_LOGIN);

        // Assert: solo se revoca el DESK; NUNCA se consulta la vista de todos los tipos
        // (la plaza del empleado, si la tiene, queda intacta) y se notifica una vez.
        assertThat(desk.isActive()).isFalse();
        assertThat(desk.getRevokedById()).isEqualTo(ADMIN_ID);
        verify(fixedAssignmentRepository, never())
                .findByEmployeeIdAndActiveTrueOrderByDayOfWeekAsc(anyLong());
        verify(eventPublisher).publishEvent(new FixedAssignmentRevokedEvent(EMP_ID));
    }

    @Test
    void shouldThrowNotFound_whenRevokingTypeWithoutActiveOfThatType() {
        // Arrange: el empleado no tiene ninguna asignacion activa del tipo solicitado
        given(fixedAssignmentRepository
                .findByEmployeeIdAndResourceTypeAndActiveTrueOrderByDayOfWeekAsc(EMP_ID, ResourceType.DESK))
                .willReturn(List.of());

        // Act / Assert
        assertThatThrownBy(() -> newService().revoke(EMP_ID, ResourceType.DESK, ADMIN_LOGIN))
                .isInstanceOf(EntityNotFoundException.class);

        // Assert: sin filas del tipo no se revoca ni se notifica nada
        verify(fixedAssignmentRepository, never()).saveAll(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowNotFound_whenRevokingEmployeeWithoutActiveAssignment() {
        // Arrange
        given(fixedAssignmentRepository.findByEmployeeIdAndActiveTrueOrderByDayOfWeekAsc(EMP_ID))
                .willReturn(List.of());

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
        given(fixedAssignmentRepository.findByEmployeeIdAndActiveTrueOrderByDayOfWeekAsc(EMP_ID))
                .willReturn(List.of(active(1)));

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
        verify(fixedAssignmentRepository, never())
                .findByEmployeeIdAndActiveTrueOrderByDayOfWeekAsc(anyLong());
    }

    @Test
    void shouldThrowNotFound_whenSettingForUnknownEmployee() {
        // Arrange: el empleado del path no existe
        given(employeeRepository.existsById(EMP_ID)).willReturn(false);

        // Act / Assert
        assertThatThrownBy(() -> newService().setAssignments(EMP_ID, request(1), ADMIN_LOGIN))
                .isInstanceOf(EntityNotFoundException.class);
        verify(fixedAssignmentRepository, never()).saveAll(any());
    }

    @Test
    void shouldThrowNotFound_whenSettingWithUnknownSpace() {
        // Arrange: empleado valido, plaza inexistente
        given(employeeRepository.existsById(EMP_ID)).willReturn(true);
        given(resourceResolvers.exists(SPACE_ID, ResourceType.PARKING)).willReturn(false);

        // Act / Assert
        assertThatThrownBy(() -> newService().setAssignments(EMP_ID, request(1), ADMIN_LOGIN))
                .isInstanceOf(EntityNotFoundException.class);
        verify(fixedAssignmentRepository, never()).saveAll(any());
    }

    @Test
    void shouldThrow_whenDayOfWeekIsNull() {
        // Act / Assert: un elemento nulo en la lista de dias
        assertThatThrownBy(() -> newService().setAssignments(
                EMP_ID, new FixedAssignmentPutRequest(SPACE_ID, java.util.Arrays.asList(1, null), null), ADMIN_LOGIN))
                .isInstanceOf(InvalidDayOfWeekException.class);
        verify(fixedAssignmentRepository, never()).saveAll(any());
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
        given(fixedAssignmentRepository.findByEmployeeIdAndActiveTrueOrderByDayOfWeekAsc(OTHER_ID))
                .willReturn(List.of(active(1)));

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

    private FixedAssignment active(int day) {
        return FixedAssignment.create(SPACE_ID, EMP_ID, day, ADMIN_ID, NOW);
    }

    private FixedAssignment activeDesk(int day) {
        return FixedAssignment.create(SPACE_ID, ResourceType.DESK, EMP_ID, day, ADMIN_ID, NOW);
    }

    private FixedAssignmentPutRequest request(Integer... days) {
        return new FixedAssignmentPutRequest(SPACE_ID, List.of(days), null);
    }
}
