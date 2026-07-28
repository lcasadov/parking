package com.aleatica.parking.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.aleatica.parking.desk.Desk;
import com.aleatica.parking.desk.DeskCategory;
import com.aleatica.parking.desk.DeskRepository;
import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.employee.Role;
import com.aleatica.parking.notification.NotificationEventType;
import com.aleatica.parking.parkingspace.ParkingSpace;
import com.aleatica.parking.parkingspace.ParkingSpaceRepository;
import com.aleatica.parking.request.domain.RequestStatus;
import com.aleatica.parking.request.dto.RequestResponse;
import com.aleatica.parking.resource.ResourceType;
import com.aleatica.parking.support.EmployeeTestFactory;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests unitarios de {@link NotificationRenderer}: resolucion del destinatario y del NUMERO
 * real del recurso aprobado, carga del plano adjunto, enrutado por tipo de evento y
 * degradacion (recurso o destinatario no encontrados). Con repositorios y el renderizador de
 * plantillas mockeados. Verifica las reglas de la spec: aprobacion -> empleado solicitante con
 * numero+planta+plano (misma via para aprobacion manual y auto-aprobacion); recurso no
 * encontrado degrada sin lanzar excepcion; destinatario inexistente -> sin mensaje.
 */
@ExtendWith(MockitoExtension.class)
class NotificationRendererTest {

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
    private ParkingSpaceRepository parkingSpaceRepository;

    @Mock
    private DeskRepository deskRepository;

    private NotificationRenderer notificationRenderer() {
        return new NotificationRenderer(
                employeeRepository, renderer, parkingSpaceRepository, deskRepository);
    }

    private Employee employee() {
        Employee employee = EmployeeTestFactory.active(EMP_ID, "emp", "emp@aleatica.com", null, Role.EMPLOYEE);
        given(employeeRepository.findById(EMP_ID)).willReturn(Optional.of(employee));
        return employee;
    }

    @Test
    void shouldRenderRequestCreated_whenCommandIsRequestCreated() {
        // Arrange
        Employee admin = employee();
        RequestResponse request = createdRequest();
        given(renderer.renderRequestCreated(admin, request)).willReturn(RENDERED);

        // Act
        Optional<EmailMessage> message = notificationRenderer()
                .render(new NotificationCommand(NotificationEventType.REQUEST_CREATED, EMP_ID, request));

        // Assert
        assertThat(message).contains(RENDERED);
        verify(renderer).renderRequestCreated(admin, request);
    }

    @Test
    void shouldResolveParkingNumberAndFloorWithPlan_whenApprovingParkingRequest() {
        // Arrange: aprobacion de una plaza nº 3005
        employee();
        given(parkingSpaceRepository.findById(PARKING_RESOURCE_ID))
                .willReturn(Optional.of(ParkingSpace.create(3005)));
        given(renderer.renderRequestApproved(any(), any(), any(), any())).willReturn(RENDERED);

        // Act
        notificationRenderer().render(approvedParkingCommand());

        // Assert: el renderer recibe numero 3005, planta 3 y el plano adjunto
        ResolvedResource resolved = captureResolved();
        assertThat(resolved.type()).isEqualTo(ResourceType.PARKING);
        assertThat(resolved.number()).isEqualTo(3005);
        assertThat(resolved.floor()).isEqualTo(3);

        EmailAttachment attachment = captureAttachment();
        assertThat(attachment).isNotNull();
        assertThat(attachment.filename()).isEqualTo("floor-plan.png");
        assertThat(attachment.contentType()).isEqualTo("image/png");
        assertThat(attachment.content()).isNotEmpty();
    }

    @Test
    void shouldResolveDeskNumberWithoutFloor_whenApprovingDeskRequest() {
        // Arrange: aprobacion de un puesto nº 12 (los puestos no tienen planta)
        employee();
        given(deskRepository.findById(DESK_RESOURCE_ID)).willReturn(Optional.of(
                Desk.create(12, DeskCategory.STANDARD, BigDecimal.ZERO, BigDecimal.ZERO)));
        given(renderer.renderRequestApproved(any(), any(), any(), any())).willReturn(RENDERED);

        // Act
        notificationRenderer().render(
                new NotificationCommand(NotificationEventType.REQUEST_APPROVED, EMP_ID, approvedDesk()));

        // Assert
        ResolvedResource resolved = captureResolved();
        assertThat(resolved.type()).isEqualTo(ResourceType.DESK);
        assertThat(resolved.number()).isEqualTo(12);
        assertThat(resolved.floor()).isNull();
    }

    @Test
    void shouldDegradeWithoutResolvedResource_whenResourceNotFound() {
        // Arrange: la plaza referida ya no existe (borrado/carrera)
        Employee employee = employee();
        given(parkingSpaceRepository.findById(PARKING_RESOURCE_ID)).willReturn(Optional.empty());
        given(renderer.renderRequestApproved(any(), any(), isNull(), any())).willReturn(RENDERED);

        // Act
        Optional<EmailMessage> message = notificationRenderer().render(approvedParkingCommand());

        // Assert: no lanza excepcion, sigue renderizando con recurso resuelto null
        assertThat(message).contains(RENDERED);
        verify(renderer).renderRequestApproved(eq(employee), any(), isNull(), any());
    }

    @Test
    void shouldResolveRequesterAndReleasedResource_whenCommandIsRequestCancelled() {
        // Arrange: el destinatario es el admin, pero el cuerpo nombra al solicitante (employeeId)
        Employee admin = EmployeeTestFactory.active(1L, "admin", "admin@aleatica.com", null, Role.ADMIN);
        Employee requester = EmployeeTestFactory.active(EMP_ID, "emp", "emp@aleatica.com", null, Role.EMPLOYEE);
        given(employeeRepository.findById(1L)).willReturn(Optional.of(admin));
        given(employeeRepository.findById(EMP_ID)).willReturn(Optional.of(requester));
        given(parkingSpaceRepository.findById(PARKING_RESOURCE_ID))
                .willReturn(Optional.of(ParkingSpace.create(3005)));
        given(renderer.renderRequestCancelled(any(), any(), any(), any())).willReturn(RENDERED);

        // Act: la orden va dirigida al admin (destinatario = 1L), la solicitud es del empleado 15
        Optional<EmailMessage> message = notificationRenderer().render(
                new NotificationCommand(NotificationEventType.REQUEST_CANCELLED, 1L, approvedParking()));

        // Assert: se pasa el admin como destinatario, el solicitante como nombre y el recurso liberado
        assertThat(message).contains(RENDERED);
        ArgumentCaptor<ResolvedResource> resolvedCaptor = ArgumentCaptor.forClass(ResolvedResource.class);
        verify(renderer).renderRequestCancelled(eq(admin), any(), eq(requester), resolvedCaptor.capture());
        ResolvedResource resolved = resolvedCaptor.getValue();
        assertThat(resolved.type()).isEqualTo(ResourceType.PARKING);
        assertThat(resolved.number()).isEqualTo(3005);
        assertThat(resolved.floor()).isEqualTo(3);
    }

    @Test
    void shouldDegradeWithNullRequester_whenRequesterNotFoundOnCancelled() {
        // Arrange: el solicitante ya no existe (borrado); no debe romper el envio
        Employee admin = EmployeeTestFactory.active(1L, "admin", "admin@aleatica.com", null, Role.ADMIN);
        given(employeeRepository.findById(1L)).willReturn(Optional.of(admin));
        given(employeeRepository.findById(EMP_ID)).willReturn(Optional.empty());
        given(parkingSpaceRepository.findById(PARKING_RESOURCE_ID))
                .willReturn(Optional.of(ParkingSpace.create(3005)));
        given(renderer.renderRequestCancelled(any(), any(), isNull(), any())).willReturn(RENDERED);

        // Act
        Optional<EmailMessage> message = notificationRenderer().render(
                new NotificationCommand(NotificationEventType.REQUEST_CANCELLED, 1L, approvedParking()));

        // Assert: renderiza con solicitante null (degradacion), sin lanzar excepcion
        assertThat(message).contains(RENDERED);
        verify(renderer).renderRequestCancelled(eq(admin), any(), isNull(), any());
    }

    @Test
    void shouldResolveResourceForAdminAssignedRecipient_whenCommandIsRequestAdminAssigned() {
        // Arrange: el destinatario es el empleado destino de la asignacion puntual del admin
        employee();
        given(parkingSpaceRepository.findById(PARKING_RESOURCE_ID))
                .willReturn(Optional.of(ParkingSpace.create(3005)));
        given(renderer.renderRequestAdminAssigned(any(), any(), any())).willReturn(RENDERED);

        // Act
        Optional<EmailMessage> message = notificationRenderer().render(
                new NotificationCommand(NotificationEventType.REQUEST_ADMIN_ASSIGNED, EMP_ID, approvedParking()));

        // Assert: el renderer recibe el recurso resuelto (numero 3005, planta 3)
        assertThat(message).contains(RENDERED);
        ArgumentCaptor<ResolvedResource> captor = ArgumentCaptor.forClass(ResolvedResource.class);
        verify(renderer).renderRequestAdminAssigned(any(), any(), captor.capture());
        ResolvedResource resolved = captor.getValue();
        assertThat(resolved.type()).isEqualTo(ResourceType.PARKING);
        assertThat(resolved.number()).isEqualTo(3005);
        assertThat(resolved.floor()).isEqualTo(3);
    }

    @Test
    void shouldDegradeWithoutResolvedResource_whenAdminAssignedResourceNotFound() {
        // Arrange: el recurso asignado ya no existe (borrado/carrera)
        employee();
        given(parkingSpaceRepository.findById(PARKING_RESOURCE_ID)).willReturn(Optional.empty());
        given(renderer.renderRequestAdminAssigned(any(), any(), isNull())).willReturn(RENDERED);

        // Act
        Optional<EmailMessage> message = notificationRenderer().render(
                new NotificationCommand(NotificationEventType.REQUEST_ADMIN_ASSIGNED, EMP_ID, approvedParking()));

        // Assert: no lanza excepcion, renderiza con recurso resuelto null
        assertThat(message).contains(RENDERED);
        verify(renderer).renderRequestAdminAssigned(any(), any(), isNull());
    }

    @Test
    void shouldRenderAssignmentRevoked_whenCommandIsAssignmentRevoked() {
        // Arrange
        Employee employee = employee();
        given(renderer.renderAssignmentRevoked(employee)).willReturn(RENDERED);

        // Act
        Optional<EmailMessage> message = notificationRenderer().render(
                new NotificationCommand(NotificationEventType.ASSIGNMENT_REVOKED, EMP_ID, null));

        // Assert
        assertThat(message).contains(RENDERED);
        verify(renderer).renderAssignmentRevoked(employee);
    }

    @Test
    void shouldReturnEmpty_whenRecipientEmployeeNotFound() {
        // Arrange: el destinatario ya no existe
        given(employeeRepository.findById(EMP_ID)).willReturn(Optional.empty());

        // Act
        Optional<EmailMessage> message = notificationRenderer().render(
                new NotificationCommand(NotificationEventType.ASSIGNMENT_REVOKED, EMP_ID, null));

        // Assert: sin destinatario no hay mensaje (el llamador no envia ni encola)
        assertThat(message).isEmpty();
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

    private NotificationCommand approvedParkingCommand() {
        return new NotificationCommand(NotificationEventType.REQUEST_APPROVED, EMP_ID, approvedParking());
    }

    private RequestResponse createdRequest() {
        return new RequestResponse(REQUEST_ID, EMP_ID, REQUESTED_DATE, RequestStatus.PENDING,
                null, null, null, null, null, null, NOW, ResourceType.PARKING, null, null);
    }

    private RequestResponse approvedParking() {
        return new RequestResponse(REQUEST_ID, EMP_ID, REQUESTED_DATE, RequestStatus.APPROVED,
                PARKING_RESOURCE_ID, "nota", null, null, 1L, NOW, NOW, ResourceType.PARKING, null, null);
    }

    private RequestResponse approvedDesk() {
        return new RequestResponse(REQUEST_ID, EMP_ID, REQUESTED_DATE, RequestStatus.APPROVED,
                DESK_RESOURCE_ID, "nota", null, null, 1L, NOW, NOW, ResourceType.DESK, null, null);
    }
}
