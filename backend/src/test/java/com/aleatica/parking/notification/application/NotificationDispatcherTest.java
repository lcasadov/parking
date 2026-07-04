package com.aleatica.parking.notification.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.employee.Role;
import com.aleatica.parking.request.RequestStatus;
import com.aleatica.parking.request.dto.RequestResponse;
import com.aleatica.parking.support.EmployeeTestFactory;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests unitarios de {@link NotificationDispatcher}: resolucion de destinatarios y
 * delegacion en el servicio de entrega, con repositorio y renderizador mockeados. Verifica
 * las reglas de la spec: "nueva solicitud" -> admins activos (excluye inactivos; nada si no
 * hay); resto -> empleado afectado.
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
    private EmailContentRenderer renderer;

    @Mock
    private NotificationDeliveryService deliveryService;

    private NotificationDispatcher dispatcher() {
        return new NotificationDispatcher(employeeRepository, renderer, deliveryService);
    }

    @Test
    void shouldQueueOneEmailPerActiveAdmin_whenRequestCreated() {
        // Arrange: la query ya filtra active = true (excluye admins inactivos por construccion)
        Employee admin1 = EmployeeTestFactory.active(1L, "admin1", "a1@aleatica.com", null, Role.ADMIN);
        Employee admin2 = EmployeeTestFactory.active(2L, "admin2", "a2@aleatica.com", null, Role.ADMIN);
        given(employeeRepository.findByRoleAndActiveTrue(Role.ADMIN)).willReturn(List.of(admin1, admin2));
        given(renderer.renderRequestCreated(any(), any()))
                .willReturn(new EmailMessage("x@aleatica.com", "s", "b"));

        // Act
        dispatcher().requestCreated(createdRequest());

        // Assert: un email por admin activo
        verify(deliveryService, times(2)).sendOrQueue(any());
    }

    @Test
    void shouldQueueNothing_whenNoActiveAdminsExistOnRequestCreated() {
        // Arrange: no hay admins activos
        given(employeeRepository.findByRoleAndActiveTrue(Role.ADMIN)).willReturn(List.of());

        // Act
        dispatcher().requestCreated(createdRequest());

        // Assert: el flujo no falla y no se encola nada
        verify(deliveryService, never()).sendOrQueue(any());
    }

    @Test
    void shouldQueueEmailToRequester_whenRequestApproved() {
        // Arrange
        Employee employee = EmployeeTestFactory.active(EMP_ID, "emp", "emp@aleatica.com", null, Role.EMPLOYEE);
        given(employeeRepository.findById(EMP_ID)).willReturn(Optional.of(employee));
        given(renderer.renderRequestApproved(any(), any()))
                .willReturn(new EmailMessage("emp@aleatica.com", "s", "b"));

        // Act
        dispatcher().requestApproved(createdRequest());

        // Assert
        verify(deliveryService, times(1)).sendOrQueue(any());
    }

    @Test
    void shouldQueueEmailToAffectedEmployee_whenAssignmentRevoked() {
        // Arrange
        Employee employee = EmployeeTestFactory.active(EMP_ID, "emp", "emp@aleatica.com", null, Role.EMPLOYEE);
        given(employeeRepository.findById(EMP_ID)).willReturn(Optional.of(employee));
        given(renderer.renderAssignmentRevoked(any()))
                .willReturn(new EmailMessage("emp@aleatica.com", "s", "b"));

        // Act
        dispatcher().assignmentRevoked(EMP_ID);

        // Assert
        verify(deliveryService, times(1)).sendOrQueue(any());
    }

    @Test
    void shouldQueueNothing_whenRecipientEmployeeNotFound() {
        // Arrange
        given(employeeRepository.findById(EMP_ID)).willReturn(Optional.empty());

        // Act
        dispatcher().assignmentRevoked(EMP_ID);

        // Assert
        verify(deliveryService, never()).sendOrQueue(any());
    }

    private RequestResponse createdRequest() {
        return new RequestResponse(REQUEST_ID, EMP_ID, REQUESTED_DATE, RequestStatus.PENDING,
                null, null, null, null, null, null, NOW);
    }
}
