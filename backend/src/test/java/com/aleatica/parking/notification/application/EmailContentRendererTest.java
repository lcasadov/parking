package com.aleatica.parking.notification.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.Role;
import com.aleatica.parking.request.domain.RequestStatus;
import com.aleatica.parking.request.dto.RequestResponse;
import com.aleatica.parking.resource.ResourceType;
import com.aleatica.parking.support.EmployeeTestFactory;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.dialect.SpringStandardDialect;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Tests unitarios de {@link EmailContentRenderer}: renderiza las plantillas Thymeleaf
 * reales (desde el classpath) y verifica que el contenido variable de cada evento
 * (nota de aprobacion, motivo de rechazo) viaja en el cuerpo del email. No usa Spring.
 */
class EmailContentRendererTest {

    private static final Long EMP_ID = 15L;
    private static final Long REQUEST_ID = 42L;
    private static final Long SPACE_ID = 8L;
    private static final LocalDate REQUESTED_DATE = LocalDate.of(2026, 7, 10);
    private static final Instant NOW = Instant.parse("2026-07-04T10:00:00Z");
    private static final String EMP_EMAIL = "empleado@aleatica.com";
    private static final String ADMIN_EMAIL = "admin@aleatica.com";

    private final EmailContentRenderer renderer = new EmailContentRenderer(templateEngine());

    private static TemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        TemplateEngine engine = new TemplateEngine();
        // Dialecto Spring (SpEL) para reproducir el motor autoconfigurado en produccion
        // (Spring Boot usa SpringTemplateEngine); evita la dependencia OGNL del dialecto estandar.
        engine.setDialect(new SpringStandardDialect());
        engine.setTemplateResolver(resolver);
        return engine;
    }

    @Test
    void shouldAddressCreatedEmailToAdmin_whenRenderingRequestCreated() {
        // Arrange
        Employee admin = EmployeeTestFactory.active(1L, "admin", ADMIN_EMAIL, null, Role.ADMIN);
        RequestResponse request = pendingRequest();

        // Act
        EmailMessage message = renderer.renderRequestCreated(admin, request);

        // Assert
        assertThat(message.to()).isEqualTo(ADMIN_EMAIL);
        assertThat(message.htmlBody()).contains(String.valueOf(REQUEST_ID));
    }

    private static final EmailAttachment FLOOR_PLAN =
            new EmailAttachment("floor-plan.png", "image/png", new byte[] {1, 2, 3});

    @Test
    void shouldRenderFormalApprovalWithParkingNumberFloorAndPlan_whenRenderingRequestApproved() {
        // Arrange
        Employee employee = EmployeeTestFactory.active(EMP_ID, "emp", EMP_EMAIL, null, Role.EMPLOYEE);
        String approvalNote = "Plaza asignada junto al ascensor";
        RequestResponse request = approvedRequest(approvalNote);
        ResolvedResource resolved = new ResolvedResource(ResourceType.PARKING, 3005, 3);

        // Act
        EmailMessage message = renderer.renderRequestApproved(employee, request, resolved, FLOOR_PLAN);

        // Assert: saludo formal con nombre+apellidos, aprobacion, numero+planta, nota y adjunto
        assertThat(message.to()).isEqualTo(EMP_EMAIL);
        assertThat(message.htmlBody())
                .contains("Estimado/a Sr./Sra. Test User")
                .contains("APROBADA")
                .contains("plaza n")
                .contains("3005")
                .contains("planta")
                .contains(approvalNote);
        // NO muestra el requestId como referencia del recurso
        assertThat(message.htmlBody()).doesNotContain(String.valueOf(REQUEST_ID));
        assertThat(message.attachments()).containsExactly(FLOOR_PLAN);
    }

    @Test
    void shouldRenderDeskNumberWithoutFloor_whenRenderingRequestApprovedForDesk() {
        // Arrange
        Employee employee = EmployeeTestFactory.active(EMP_ID, "emp", EMP_EMAIL, null, Role.EMPLOYEE);
        RequestResponse request = approvedRequest(null);
        ResolvedResource resolved = new ResolvedResource(ResourceType.DESK, 12, null);

        // Act
        EmailMessage message = renderer.renderRequestApproved(employee, request, resolved, FLOOR_PLAN);

        // Assert: puesto por numero, sin planta
        assertThat(message.htmlBody())
                .contains("puesto n")
                .contains("12")
                .doesNotContain("en la planta");
        assertThat(message.attachments()).containsExactly(FLOOR_PLAN);
    }

    @Test
    void shouldRenderWithoutAttachmentOrNumber_whenResourceNotResolved() {
        // Arrange: recurso no localizado (resolved null) y plano no cargado (floorPlan null)
        Employee employee = EmployeeTestFactory.active(EMP_ID, "emp", EMP_EMAIL, null, Role.EMPLOYEE);
        RequestResponse request = approvedRequest(null);

        // Act
        EmailMessage message = renderer.renderRequestApproved(employee, request, null, null);

        // Assert: se degrada sin numero ni adjunto, conservando la comunicacion de aprobacion
        assertThat(message.htmlBody()).contains("APROBADA");
        assertThat(message.attachments()).isEmpty();
    }

    @Test
    void shouldNameParkingInSubject_whenApprovingParkingRequest() {
        // Arrange: recurso PARKING -> el asunto debe decir "plaza"
        Employee employee = EmployeeTestFactory.active(EMP_ID, "emp", EMP_EMAIL, null, Role.EMPLOYEE);
        RequestResponse request = approvedRequest("Nota real del admin");
        ResolvedResource resolved = new ResolvedResource(ResourceType.PARKING, 3005, 3);

        // Act
        EmailMessage message = renderer.renderRequestApproved(employee, request, resolved, FLOOR_PLAN);

        // Assert
        assertThat(message.subject()).isEqualTo("Tu solicitud de plaza ha sido aprobada");
    }

    @Test
    void shouldNameDeskInSubject_whenApprovingDeskRequest() {
        // Arrange: recurso DESK -> el asunto debe decir "puesto" (no "plaza")
        Employee employee = EmployeeTestFactory.active(EMP_ID, "emp", EMP_EMAIL, null, Role.EMPLOYEE);
        RequestResponse request = approvedDeskRequest();
        ResolvedResource resolved = new ResolvedResource(ResourceType.DESK, 12, null);

        // Act
        EmailMessage message = renderer.renderRequestApproved(employee, request, resolved, FLOOR_PLAN);

        // Assert
        assertThat(message.subject()).isEqualTo("Tu solicitud de puesto ha sido aprobada");
    }

    @Test
    void shouldNameParkingInSubject_whenRejectingParkingRequest() {
        // Arrange: rechazo de un recurso PARKING
        Employee employee = EmployeeTestFactory.active(EMP_ID, "emp", EMP_EMAIL, null, Role.EMPLOYEE);
        RequestResponse request = rejectedRequest("No hay plazas");

        // Act
        EmailMessage message = renderer.renderRequestRejected(employee, request);

        // Assert
        assertThat(message.subject()).isEqualTo("Tu solicitud de plaza ha sido rechazada");
    }

    @Test
    void shouldNameDeskInSubject_whenRejectingDeskRequest() {
        // Arrange: rechazo de un recurso DESK -> el asunto debe decir "puesto"
        Employee employee = EmployeeTestFactory.active(EMP_ID, "emp", EMP_EMAIL, null, Role.EMPLOYEE);
        RequestResponse request = rejectedDeskRequest("No hay puestos");

        // Act
        EmailMessage message = renderer.renderRequestRejected(employee, request);

        // Assert
        assertThat(message.subject()).isEqualTo("Tu solicitud de puesto ha sido rechazada");
    }

    @Test
    void shouldNameDeskInSubject_whenCancellingDeskRequest() {
        // Arrange: aviso al admin de la cancelacion de una solicitud DESK aprobada
        Employee admin = EmployeeTestFactory.active(1L, "admin", ADMIN_EMAIL, null, Role.ADMIN);
        Employee requester = EmployeeTestFactory.active(EMP_ID, "emp", EMP_EMAIL, null, Role.EMPLOYEE);
        RequestResponse request = cancelledDeskRequest();
        ResolvedResource resolved = new ResolvedResource(ResourceType.DESK, 12, null);

        // Act
        EmailMessage message = renderer.renderRequestCancelled(admin, request, requester, resolved);

        // Assert
        assertThat(message.subject())
                .isEqualTo("Un empleado ha cancelado una solicitud de puesto aprobada (recurso liberado)");
    }

    @Test
    void shouldHideAdminNoteLine_whenApprovalNoteIsAutoApproval() {
        // Arrange: auto-aprobacion -> nota interna "auto", que NO debe mostrarse
        Employee employee = EmployeeTestFactory.active(EMP_ID, "emp", EMP_EMAIL, null, Role.EMPLOYEE);
        RequestResponse request = approvedRequest(
                com.aleatica.parking.request.domain.Request.AUTO_APPROVAL_NOTE);
        ResolvedResource resolved = new ResolvedResource(ResourceType.PARKING, 3005, 3);

        // Act
        EmailMessage message = renderer.renderRequestApproved(employee, request, resolved, FLOOR_PLAN);

        // Assert: se comunica la aprobacion y el numero, pero sin la linea de nota del administrador
        assertThat(message.htmlBody())
                .contains("APROBADA")
                .contains("3005")
                .doesNotContain("Nota del administrador");
    }

    @Test
    void shouldShowAdminNoteLine_whenApprovalNoteIsRealAdminNote() {
        // Arrange: aprobacion manual con nota real de admin -> debe mostrarse
        Employee employee = EmployeeTestFactory.active(EMP_ID, "emp", EMP_EMAIL, null, Role.EMPLOYEE);
        String realNote = "Plaza asignada junto al ascensor";
        RequestResponse request = approvedRequest(realNote);
        ResolvedResource resolved = new ResolvedResource(ResourceType.PARKING, 3005, 3);

        // Act
        EmailMessage message = renderer.renderRequestApproved(employee, request, resolved, FLOOR_PLAN);

        // Assert
        assertThat(message.htmlBody())
                .contains("Nota del administrador")
                .contains(realNote);
    }

    @Test
    void shouldNameRequesterAndReleasedParking_whenRenderingRequestCancelled() {
        // Arrange: aviso al admin de que el solicitante cancelo su solicitud aprobada de una plaza
        Employee admin = EmployeeTestFactory.active(1L, "admin", ADMIN_EMAIL, null, Role.ADMIN);
        Employee requester = EmployeeTestFactory.active(EMP_ID, "emp", EMP_EMAIL, null, Role.EMPLOYEE);
        RequestResponse request = cancelledRequest();
        ResolvedResource resolved = new ResolvedResource(ResourceType.PARKING, 3005, 3);

        // Act
        EmailMessage message = renderer.renderRequestCancelled(admin, request, requester, resolved);

        // Assert: dirigido al admin, nombra al solicitante y la plaza liberada con su numero y planta
        assertThat(message.to()).isEqualTo(ADMIN_EMAIL);
        assertThat(message.htmlBody())
                .contains("Test User")
                .contains("plaza n")
                .contains("3005")
                .contains("planta")
                .contains("liberado");
    }

    @Test
    void shouldReleaseDeskWithoutFloor_whenRenderingRequestCancelledForDesk() {
        // Arrange: recurso liberado de tipo puesto (sin planta)
        Employee admin = EmployeeTestFactory.active(1L, "admin", ADMIN_EMAIL, null, Role.ADMIN);
        Employee requester = EmployeeTestFactory.active(EMP_ID, "emp", EMP_EMAIL, null, Role.EMPLOYEE);
        RequestResponse request = cancelledRequest();
        ResolvedResource resolved = new ResolvedResource(ResourceType.DESK, 12, null);

        // Act
        EmailMessage message = renderer.renderRequestCancelled(admin, request, requester, resolved);

        // Assert
        assertThat(message.htmlBody())
                .contains("puesto n")
                .contains("12")
                .doesNotContain("en la planta");
    }

    @Test
    void shouldDegradeWithoutRequesterName_whenRequesterMissingOnCancelled() {
        // Arrange: solicitante ya borrado (requester null) -> el correo se envia sin su nombre
        Employee admin = EmployeeTestFactory.active(1L, "admin", ADMIN_EMAIL, null, Role.ADMIN);
        RequestResponse request = cancelledRequest();
        ResolvedResource resolved = new ResolvedResource(ResourceType.PARKING, 3005, 3);

        // Act
        EmailMessage message = renderer.renderRequestCancelled(admin, request, null, resolved);

        // Assert: se avisa de la liberacion sin romper, aunque sin nombre del solicitante
        assertThat(message.htmlBody())
                .contains("un empleado")
                .contains("3005")
                .contains("liberado");
    }

    @Test
    void shouldCarryRejectionReasonInBody_whenRenderingRequestRejected() {
        // Arrange
        Employee employee = EmployeeTestFactory.active(EMP_ID, "emp", EMP_EMAIL, null, Role.EMPLOYEE);
        String reason = "No quedan plazas disponibles esa fecha";
        RequestResponse request = rejectedRequest(reason);

        // Act
        EmailMessage message = renderer.renderRequestRejected(employee, request);

        // Assert
        assertThat(message.htmlBody()).contains(reason);
    }

    @Test
    void shouldAddressRevokedEmailToAffectedEmployee_whenRenderingAssignmentRevoked() {
        // Arrange
        Employee employee = EmployeeTestFactory.active(EMP_ID, "emp", EMP_EMAIL, null, Role.EMPLOYEE);

        // Act
        EmailMessage message = renderer.renderAssignmentRevoked(employee);

        // Assert
        assertThat(message.to()).isEqualTo(EMP_EMAIL);
        assertThat(message.htmlBody()).contains("revocada");
    }

    private RequestResponse pendingRequest() {
        return new RequestResponse(REQUEST_ID, EMP_ID, REQUESTED_DATE, RequestStatus.PENDING,
                null, null, null, null, null, null, NOW, com.aleatica.parking.resource.ResourceType.PARKING, null, null);
    }

    private RequestResponse approvedRequest(String approvalNote) {
        return new RequestResponse(REQUEST_ID, EMP_ID, REQUESTED_DATE, RequestStatus.APPROVED,
                SPACE_ID, approvalNote, null, null, 1L, NOW, NOW, com.aleatica.parking.resource.ResourceType.PARKING, null, null);
    }

    private RequestResponse cancelledRequest() {
        return new RequestResponse(REQUEST_ID, EMP_ID, REQUESTED_DATE, RequestStatus.CANCELLED,
                SPACE_ID, null, null, null, 1L, NOW, NOW,
                com.aleatica.parking.resource.ResourceType.PARKING, null, null);
    }

    private RequestResponse cancelledDeskRequest() {
        return new RequestResponse(REQUEST_ID, EMP_ID, REQUESTED_DATE, RequestStatus.CANCELLED,
                SPACE_ID, null, null, null, 1L, NOW, NOW, ResourceType.DESK, null, null);
    }

    private RequestResponse approvedDeskRequest() {
        return new RequestResponse(REQUEST_ID, EMP_ID, REQUESTED_DATE, RequestStatus.APPROVED,
                SPACE_ID, null, null, null, 1L, NOW, NOW, ResourceType.DESK, null, null);
    }

    private RequestResponse rejectedDeskRequest(String reason) {
        return new RequestResponse(REQUEST_ID, EMP_ID, REQUESTED_DATE, RequestStatus.REJECTED,
                null, null, com.aleatica.parking.request.domain.RejectionReasonCode.NO_AVAILABILITY,
                reason, 1L, NOW, NOW, ResourceType.DESK, null, null);
    }

    private RequestResponse rejectedRequest(String reason) {
        return new RequestResponse(REQUEST_ID, EMP_ID, REQUESTED_DATE, RequestStatus.REJECTED,
                null, null, com.aleatica.parking.request.domain.RejectionReasonCode.NO_AVAILABILITY,
                reason, 1L, NOW, NOW, com.aleatica.parking.resource.ResourceType.PARKING, null, null);
    }
}
