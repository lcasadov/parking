package com.aleatica.parking.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.aleatica.parking.desk.Desk;
import com.aleatica.parking.desk.DeskCategory;
import com.aleatica.parking.desk.DeskRepository;
import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.employee.Role;
import com.aleatica.parking.parkingspace.ParkingSpace;
import com.aleatica.parking.parkingspace.ParkingSpaceRepository;
import com.aleatica.parking.request.domain.RequestStatus;
import com.aleatica.parking.request.dto.RequestResponse;
import com.aleatica.parking.resource.ResourceType;
import com.aleatica.parking.support.EmployeeTestFactory;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests unitarios de {@link NotificationDispatcher}: resolucion de destinatarios y del NUMERO
 * real del recurso aprobado, carga del plano adjunto y delegacion en el servicio de entrega,
 * con repositorios y renderizador mockeados. Verifica las reglas de la spec: "nueva solicitud"
 * -> admins activos; aprobacion -> empleado solicitante con numero+planta+plano (misma via para
 * aprobacion manual y auto-aprobacion); recurso no encontrado degrada sin lanzar excepcion.
 */
@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    private static final Long EMP_ID = 15L;
    private static final Long REQUEST_ID = 42L;
    private static final Long PARKING_RESOURCE_ID = 3005L;
    private static final Long DESK_RESOURCE_ID = 7L;
    private static final LocalDate REQUESTED_DATE = LocalDate.of(2026, 7, 10);
    private static final Instant NOW = Instant.parse("2026-07-04T10:00:00Z");
    private static final EmailMessage RENDERED = new EmailMessage("emp@aleatica.com", "s", "b");

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private EmailContentRenderer renderer;

    @Mock
    private NotificationDeliveryService deliveryService;

    @Mock
    private ParkingSpaceRepository parkingSpaceRepository;

    @Mock
    private DeskRepository deskRepository;

    private NotificationDispatcher dispatcher() {
        return new NotificationDispatcher(
                employeeRepository, renderer, deliveryService, parkingSpaceRepository, deskRepository);
    }

    @Test
    void shouldQueueOneEmailPerActiveAdmin_whenRequestCreated() {
        // Arrange: la query ya filtra active = true (excluye admins inactivos por construccion)
        Employee admin1 = EmployeeTestFactory.active(1L, "admin1", "a1@aleatica.com", null, Role.ADMIN);
        Employee admin2 = EmployeeTestFactory.active(2L, "admin2", "a2@aleatica.com", null, Role.ADMIN);
        given(employeeRepository.findByRoleAndActiveTrue(Role.ADMIN)).willReturn(List.of(admin1, admin2));
        given(renderer.renderRequestCreated(any(), any())).willReturn(RENDERED);

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
    void shouldResolveParkingNumberAndFloorWithPlan_whenApprovingManualAdminRequest() {
        // Arrange: aprobacion manual del admin de una plaza nº 3005
        Employee employee = EmployeeTestFactory.active(EMP_ID, "emp", "emp@aleatica.com", null, Role.EMPLOYEE);
        given(employeeRepository.findById(EMP_ID)).willReturn(Optional.of(employee));
        given(parkingSpaceRepository.findById(PARKING_RESOURCE_ID))
                .willReturn(Optional.of(ParkingSpace.create(3005)));
        given(renderer.renderRequestApproved(any(), any(), any(), any())).willReturn(RENDERED);

        // Act
        dispatcher().requestApproved(approvedParking());

        // Assert: el renderer recibe numero 3005, planta 3 y el plano adjunto
        ResolvedResource resolved = captureResolved();
        assertThat(resolved.type()).isEqualTo(ResourceType.PARKING);
        assertThat(resolved.number()).isEqualTo(3005);
        assertThat(resolved.floor()).isEqualTo(3);
        verify(deliveryService, times(1)).sendOrQueue(RENDERED);
    }

    @Test
    void shouldResolveSameFormalEmailWithNumberAndPlan_whenAutoApproved() {
        // Arrange: auto-aprobacion automatica (mismo camino RequestApprovedEvent -> requestApproved)
        Employee employee = EmployeeTestFactory.active(EMP_ID, "emp", "emp@aleatica.com", null, Role.EMPLOYEE);
        given(employeeRepository.findById(EMP_ID)).willReturn(Optional.of(employee));
        given(parkingSpaceRepository.findById(PARKING_RESOURCE_ID))
                .willReturn(Optional.of(ParkingSpace.create(3005)));
        given(renderer.renderRequestApproved(any(), any(), any(), any())).willReturn(RENDERED);

        // Act
        dispatcher().requestApproved(approvedParking());

        // Assert: identico al manual — numero 3005, planta 3, plano adjunto
        ResolvedResource resolved = captureResolved();
        assertThat(resolved.number()).isEqualTo(3005);
        assertThat(resolved.floor()).isEqualTo(3);
        EmailAttachment attachment = captureAttachment();
        assertThat(attachment).isNotNull();
        assertThat(attachment.filename()).isEqualTo("floor-plan.png");
        assertThat(attachment.contentType()).isEqualTo("image/png");
        assertThat(attachment.content()).isNotEmpty();
        verify(deliveryService, times(1)).sendOrQueue(RENDERED);
    }

    @Test
    void shouldResolveDeskNumberWithoutFloor_whenApprovingDeskRequest() {
        // Arrange: aprobacion de un puesto nº 12 (los puestos no tienen planta)
        Employee employee = EmployeeTestFactory.active(EMP_ID, "emp", "emp@aleatica.com", null, Role.EMPLOYEE);
        given(employeeRepository.findById(EMP_ID)).willReturn(Optional.of(employee));
        given(deskRepository.findById(DESK_RESOURCE_ID)).willReturn(Optional.of(
                Desk.create(12, DeskCategory.STANDARD, BigDecimal.ZERO, BigDecimal.ZERO)));
        given(renderer.renderRequestApproved(any(), any(), any(), any())).willReturn(RENDERED);

        // Act
        dispatcher().requestApproved(approvedDesk());

        // Assert
        ResolvedResource resolved = captureResolved();
        assertThat(resolved.type()).isEqualTo(ResourceType.DESK);
        assertThat(resolved.number()).isEqualTo(12);
        assertThat(resolved.floor()).isNull();
    }

    @Test
    void shouldDegradeWithoutResolvedResource_whenResourceNotFound() {
        // Arrange: la plaza referida ya no existe (borrado/carrera)
        Employee employee = EmployeeTestFactory.active(EMP_ID, "emp", "emp@aleatica.com", null, Role.EMPLOYEE);
        given(employeeRepository.findById(EMP_ID)).willReturn(Optional.of(employee));
        given(parkingSpaceRepository.findById(PARKING_RESOURCE_ID)).willReturn(Optional.empty());
        given(renderer.renderRequestApproved(any(), any(), isNull(), any())).willReturn(RENDERED);

        // Act
        dispatcher().requestApproved(approvedParking());

        // Assert: no lanza excepcion, sigue enviando con recurso resuelto null
        verify(renderer).renderRequestApproved(eq(employee), any(), isNull(), any());
        verify(deliveryService, times(1)).sendOrQueue(RENDERED);
    }

    @Test
    void shouldQueueEmailToAffectedEmployee_whenAssignmentRevoked() {
        // Arrange
        Employee employee = EmployeeTestFactory.active(EMP_ID, "emp", "emp@aleatica.com", null, Role.EMPLOYEE);
        given(employeeRepository.findById(EMP_ID)).willReturn(Optional.of(employee));
        given(renderer.renderAssignmentRevoked(any())).willReturn(RENDERED);

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

    private ResolvedResource captureResolved() {
        ArgumentCaptor<ResolvedResource> captor = ArgumentCaptor.forClass(ResolvedResource.class);
        verify(renderer).renderRequestApproved(any(), any(), captor.capture(), any());
        return captor.getValue();
    }

    private EmailAttachment captureAttachment() {
        ArgumentCaptor<EmailAttachment> captor = ArgumentCaptor.forClass(EmailAttachment.class);
        verify(renderer).renderRequestApproved(any(), any(), any(), captor.capture());
        return captor.getValue();
    }

    private RequestResponse createdRequest() {
        return new RequestResponse(REQUEST_ID, EMP_ID, REQUESTED_DATE, RequestStatus.PENDING,
                null, null, null, null, null, null, NOW, ResourceType.PARKING);
    }

    private RequestResponse approvedParking() {
        return new RequestResponse(REQUEST_ID, EMP_ID, REQUESTED_DATE, RequestStatus.APPROVED,
                PARKING_RESOURCE_ID, "nota", null, null, 1L, NOW, NOW, ResourceType.PARKING);
    }

    private RequestResponse approvedDesk() {
        return new RequestResponse(REQUEST_ID, EMP_ID, REQUESTED_DATE, RequestStatus.APPROVED,
                DESK_RESOURCE_ID, "nota", null, null, 1L, NOW, NOW, ResourceType.DESK);
    }
}
