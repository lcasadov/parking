package com.aleatica.parking.notification.application;

import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.request.dto.RequestResponse;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Renderiza el contenido de cada email a partir de su plantilla Thymeleaf y el modelo del
 * evento, devolviendo un {@link EmailMessage} listo para enviar.
 *
 * <p>Separa contenido (plantillas {@code templates/email/*.html}) de logica y centraliza
 * los asuntos y nombres de plantilla como constantes (S1192). El cuerpo se renderiza en
 * el momento del evento; el {@link EmailMessage} resultante es autocontenido, de modo que
 * un reintento posterior no necesita volver a renderizar.</p>
 */
@Component
public class EmailContentRenderer {

    private static final Locale LOCALE_ES = Locale.forLanguageTag("es");

    private static final String TEMPLATE_REQUEST_CREATED = "email/request-created";
    private static final String TEMPLATE_REQUEST_APPROVED = "email/request-approved";
    private static final String TEMPLATE_REQUEST_REJECTED = "email/request-rejected";
    private static final String TEMPLATE_ASSIGNMENT_REVOKED = "email/assignment-revoked";
    private static final String TEMPLATE_PASSWORD_RESET = "email/password-reset";

    private static final String SUBJECT_REQUEST_CREATED = "Nueva solicitud de plaza pendiente";
    private static final String SUBJECT_REQUEST_APPROVED = "Tu solicitud de plaza ha sido aprobada";
    private static final String SUBJECT_REQUEST_REJECTED = "Tu solicitud de plaza ha sido rechazada";
    private static final String SUBJECT_ASSIGNMENT_REVOKED = "Tu asignacion fija ha sido revocada";
    private static final String SUBJECT_PASSWORD_RESET = "Tu contrasena temporal de parking";

    private static final String VAR_RECIPIENT_NAME = "recipientName";
    private static final String VAR_REQUEST_ID = "requestId";
    private static final String VAR_REQUESTED_DATE = "requestedDate";
    private static final String VAR_APPROVAL_NOTE = "approvalNote";
    private static final String VAR_REJECTION_REASON = "rejectionReason";
    private static final String VAR_TEMPORARY_PASSWORD = "temporaryPassword";

    private final ITemplateEngine templateEngine;

    /**
     * @param templateEngine motor Thymeleaf (autoconfigurado por Spring Boot)
     */
    public EmailContentRenderer(ITemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    /**
     * Renderiza el email de "nueva solicitud" dirigido a un administrador.
     *
     * @param admin   administrador destinatario
     * @param request solicitud recien creada
     * @return el mensaje renderizado
     */
    public EmailMessage renderRequestCreated(Employee admin, RequestResponse request) {
        Context ctx = baseContext(admin);
        ctx.setVariable(VAR_REQUEST_ID, request.id());
        ctx.setVariable(VAR_REQUESTED_DATE, request.requestedDate());
        return render(admin, SUBJECT_REQUEST_CREATED, TEMPLATE_REQUEST_CREATED, ctx);
    }

    /**
     * Renderiza el email de "solicitud aprobada" dirigido al empleado solicitante, con la
     * nota del administrador.
     *
     * @param employee empleado solicitante destinatario
     * @param request  solicitud aprobada (con {@code approvalNote})
     * @return el mensaje renderizado
     */
    public EmailMessage renderRequestApproved(Employee employee, RequestResponse request) {
        Context ctx = baseContext(employee);
        ctx.setVariable(VAR_REQUEST_ID, request.id());
        ctx.setVariable(VAR_REQUESTED_DATE, request.requestedDate());
        ctx.setVariable(VAR_APPROVAL_NOTE, request.approvalNote());
        return render(employee, SUBJECT_REQUEST_APPROVED, TEMPLATE_REQUEST_APPROVED, ctx);
    }

    /**
     * Renderiza el email de "solicitud rechazada" dirigido al empleado solicitante, con el
     * motivo del rechazo.
     *
     * @param employee empleado solicitante destinatario
     * @param request  solicitud rechazada (con {@code rejectionReason})
     * @return el mensaje renderizado
     */
    public EmailMessage renderRequestRejected(Employee employee, RequestResponse request) {
        Context ctx = baseContext(employee);
        ctx.setVariable(VAR_REQUEST_ID, request.id());
        ctx.setVariable(VAR_REQUESTED_DATE, request.requestedDate());
        ctx.setVariable(VAR_REJECTION_REASON, request.rejectionReason());
        return render(employee, SUBJECT_REQUEST_REJECTED, TEMPLATE_REQUEST_REJECTED, ctx);
    }

    /**
     * Renderiza el email de "asignacion fija revocada" dirigido al empleado afectado.
     *
     * @param employee empleado afectado destinatario
     * @return el mensaje renderizado
     */
    public EmailMessage renderAssignmentRevoked(Employee employee) {
        Context ctx = baseContext(employee);
        return render(employee, SUBJECT_ASSIGNMENT_REVOKED, TEMPLATE_ASSIGNMENT_REVOKED, ctx);
    }

    /**
     * Renderiza el email de "contrasena temporal" dirigido al empleado. 🔵 Solo Fase 2.
     *
     * @param employee          empleado destinatario
     * @param temporaryPassword contrasena temporal en claro
     * @return el mensaje renderizado
     */
    public EmailMessage renderPasswordReset(Employee employee, String temporaryPassword) {
        Context ctx = baseContext(employee);
        ctx.setVariable(VAR_TEMPORARY_PASSWORD, temporaryPassword);
        return render(employee, SUBJECT_PASSWORD_RESET, TEMPLATE_PASSWORD_RESET, ctx);
    }

    private Context baseContext(Employee recipient) {
        Context ctx = new Context(LOCALE_ES);
        ctx.setVariable(VAR_RECIPIENT_NAME, recipient.getFirstName());
        return ctx;
    }

    private EmailMessage render(Employee recipient, String subject, String template, Context ctx) {
        String body = templateEngine.process(template, ctx);
        return new EmailMessage(recipient.getEmail(), subject, body);
    }
}
