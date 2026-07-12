package com.aleatica.parking.notification.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.Role;
import com.aleatica.parking.request.domain.RequestStatus;
import com.aleatica.parking.request.dto.RequestResponse;
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

    @Test
    void shouldCarryApprovalNoteInBody_whenRenderingRequestApproved() {
        // Arrange
        Employee employee = EmployeeTestFactory.active(EMP_ID, "emp", EMP_EMAIL, null, Role.EMPLOYEE);
        String approvalNote = "Plaza asignada junto al ascensor";
        RequestResponse request = approvedRequest(approvalNote);

        // Act
        EmailMessage message = renderer.renderRequestApproved(employee, request);

        // Assert: el cuerpo incluye la nota del administrador (spec §aprobacion)
        assertThat(message.to()).isEqualTo(EMP_EMAIL);
        assertThat(message.htmlBody()).contains(approvalNote);
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
                null, null, null, null, null, null, NOW, com.aleatica.parking.resource.ResourceType.PARKING);
    }

    private RequestResponse approvedRequest(String approvalNote) {
        return new RequestResponse(REQUEST_ID, EMP_ID, REQUESTED_DATE, RequestStatus.APPROVED,
                SPACE_ID, approvalNote, null, null, 1L, NOW, NOW, com.aleatica.parking.resource.ResourceType.PARKING);
    }

    private RequestResponse rejectedRequest(String reason) {
        return new RequestResponse(REQUEST_ID, EMP_ID, REQUESTED_DATE, RequestStatus.REJECTED,
                null, null, com.aleatica.parking.request.domain.RejectionReasonCode.NO_AVAILABILITY,
                reason, 1L, NOW, NOW, com.aleatica.parking.resource.ResourceType.PARKING);
    }
}
