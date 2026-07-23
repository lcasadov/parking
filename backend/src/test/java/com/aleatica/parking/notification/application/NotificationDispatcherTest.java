package com.aleatica.parking.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.employee.Role;
import com.aleatica.parking.notification.NotificationEventType;
import com.aleatica.parking.request.domain.RequestStatus;
import com.aleatica.parking.request.dto.RequestResponse;
import com.aleatica.parking.resource.ResourceType;
import com.aleatica.parking.support.EmployeeTestFactory;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests unitarios de {@link NotificationDispatcher}: traduce cada evento de dominio en las
 * {@link NotificationCommand} correctas (tipo de evento + destinatario + solicitud) y las
 * entrega al servicio de entrega. La resolucion del recurso y el renderizado se prueban en
 * {@link NotificationRendererTest}; aqui solo se verifica el conjunto de destinatarios y el
 * mapeo evento -> orden. Con {@link EmployeeRepository} y {@link NotificationDeliveryService}
 * mockeados.
 */
@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    private static final Long EMP_ID = 15L;
    private static final Long REQUEST_ID = 42L;
    private static final LocalDate REQUESTED_DATE = LocalDate.of(2026, 7, 10);
    private static final Instant NOW = Instant.parse("2026-07-04T10:00:00Z");

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private NotificationDeliveryService deliveryService;

    private NotificationDispatcher dispatcher() {
        return new NotificationDispatcher(employeeRepository, deliveryService);
    }

    @Test
    void shouldDispatchOneCommandPerActiveAdmin_whenRequestCreated() {
        // Arrange: la query ya filtra active = true (excluye admins inactivos por construccion)
        Employee admin1 = EmployeeTestFactory.active(1L, "admin1", "a1@aleatica.com", null, Role.ADMIN);
        Employee admin2 = EmployeeTestFactory.active(2L, "admin2", "a2@aleatica.com", null, Role.ADMIN);
        given(employeeRepository.findByRoleAndActiveTrue(Role.ADMIN)).willReturn(List.of(admin1, admin2));
        RequestResponse request = createdRequest();

        // Act
        dispatcher().requestCreated(request);

        // Assert: una orden REQUEST_CREATED por admin activo, con su id de destinatario
        List<NotificationCommand> commands = captureCommands(2);
        assertThat(commands).allSatisfy(command -> {
            assertThat(command.eventType()).isEqualTo(NotificationEventType.REQUEST_CREATED);
            assertThat(command.request()).isEqualTo(request);
        });
        assertThat(commands).extracting(NotificationCommand::recipientEmployeeId)
                .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void shouldDispatchNothing_whenNoActiveAdminsExistOnRequestCreated() {
        // Arrange: no hay admins activos
        given(employeeRepository.findByRoleAndActiveTrue(Role.ADMIN)).willReturn(List.of());

        // Act
        dispatcher().requestCreated(createdRequest());

        // Assert: el flujo no falla y no se emite ninguna orden
        verify(deliveryService, never()).dispatch(any());
    }

    @Test
    void shouldDispatchOneCommandPerActiveAdmin_whenRequestCancelled() {
        // Arrange: la query ya filtra active = true (excluye admins inactivos por construccion)
        Employee admin1 = EmployeeTestFactory.active(1L, "admin1", "a1@aleatica.com", null, Role.ADMIN);
        Employee admin2 = EmployeeTestFactory.active(2L, "admin2", "a2@aleatica.com", null, Role.ADMIN);
        given(employeeRepository.findByRoleAndActiveTrue(Role.ADMIN)).willReturn(List.of(admin1, admin2));
        RequestResponse request = approvedParking();

        // Act
        dispatcher().requestCancelled(request);

        // Assert: una orden REQUEST_CANCELLED por admin activo, con su id de destinatario
        List<NotificationCommand> commands = captureCommands(2);
        assertThat(commands).allSatisfy(command -> {
            assertThat(command.eventType()).isEqualTo(NotificationEventType.REQUEST_CANCELLED);
            assertThat(command.request()).isEqualTo(request);
        });
        assertThat(commands).extracting(NotificationCommand::recipientEmployeeId)
                .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void shouldDispatchNothing_whenNoActiveAdminsExistOnRequestCancelled() {
        // Arrange: no hay admins activos
        given(employeeRepository.findByRoleAndActiveTrue(Role.ADMIN)).willReturn(List.of());

        // Act
        dispatcher().requestCancelled(approvedParking());

        // Assert: el flujo no falla y no se emite ninguna orden
        verify(deliveryService, never()).dispatch(any());
    }

    @Test
    void shouldDispatchApprovedCommandToRequester_whenRequestApproved() {
        // Arrange
        RequestResponse request = approvedParking();

        // Act
        dispatcher().requestApproved(request);

        // Assert
        NotificationCommand command = captureCommand();
        assertThat(command.eventType()).isEqualTo(NotificationEventType.REQUEST_APPROVED);
        assertThat(command.recipientEmployeeId()).isEqualTo(EMP_ID);
        assertThat(command.request()).isEqualTo(request);
    }

    @Test
    void shouldDispatchRejectedCommandToRequester_whenRequestRejected() {
        // Arrange
        RequestResponse request = approvedParking();

        // Act
        dispatcher().requestRejected(request);

        // Assert
        NotificationCommand command = captureCommand();
        assertThat(command.eventType()).isEqualTo(NotificationEventType.REQUEST_REJECTED);
        assertThat(command.recipientEmployeeId()).isEqualTo(EMP_ID);
    }

    @Test
    void shouldDispatchAdminAssignedCommandToTargetEmployee_whenRequestAdminAssigned() {
        // Arrange
        RequestResponse request = approvedParking();

        // Act
        dispatcher().requestAdminAssigned(request);

        // Assert: la orden va al empleado destino (employeeId de la asignacion), no al admin actuante
        NotificationCommand command = captureCommand();
        assertThat(command.eventType()).isEqualTo(NotificationEventType.REQUEST_ADMIN_ASSIGNED);
        assertThat(command.recipientEmployeeId()).isEqualTo(EMP_ID);
        assertThat(command.request()).isEqualTo(request);
    }

    @Test
    void shouldDispatchRevokedCommandWithoutRequest_whenAssignmentRevoked() {
        // Act
        dispatcher().assignmentRevoked(EMP_ID);

        // Assert: la revocacion no deriva de una solicitud -> request null
        NotificationCommand command = captureCommand();
        assertThat(command.eventType()).isEqualTo(NotificationEventType.ASSIGNMENT_REVOKED);
        assertThat(command.recipientEmployeeId()).isEqualTo(EMP_ID);
        assertThat(command.request()).isNull();
    }

    private NotificationCommand captureCommand() {
        return captureCommands(1).get(0);
    }

    private List<NotificationCommand> captureCommands(int times) {
        ArgumentCaptor<NotificationCommand> captor = ArgumentCaptor.forClass(NotificationCommand.class);
        verify(deliveryService, times(times)).dispatch(captor.capture());
        return captor.getAllValues();
    }

    private RequestResponse createdRequest() {
        return new RequestResponse(REQUEST_ID, EMP_ID, REQUESTED_DATE, RequestStatus.PENDING,
                null, null, null, null, null, null, NOW, ResourceType.PARKING, null, null);
    }

    private RequestResponse approvedParking() {
        return new RequestResponse(REQUEST_ID, EMP_ID, REQUESTED_DATE, RequestStatus.APPROVED,
                3005L, "nota", null, null, 1L, NOW, NOW, ResourceType.PARKING, null, null);
    }
}
