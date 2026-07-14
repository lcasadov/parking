package com.aleatica.parking.notification.application;

import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.request.domain.Request;
import com.aleatica.parking.request.dto.RequestResponse;
import com.aleatica.parking.resource.ResourceType;
import java.util.List;
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
    private static final String TEMPLATE_REQUEST_CANCELLED = "email/request-cancelled";
    private static final String TEMPLATE_ASSIGNMENT_REVOKED = "email/assignment-revoked";
    private static final String TEMPLATE_PASSWORD_RESET = "email/password-reset";

    /**
     * Plantillas de asunto parametrizadas por la palabra del recurso ({@code %s} = "plaza"/"puesto",
     * derivada de {@code resourceType}), para que el asunto nombre correctamente el recurso tanto en
     * PARKING como en DESK. Los asuntos que NO dependen del recurso (reset de contrasena, revocacion
     * de asignacion) permanecen fijos.
     */
    private static final String SUBJECT_REQUEST_CREATED_FMT = "Nueva solicitud de %s pendiente";
    private static final String SUBJECT_REQUEST_APPROVED_FMT = "Tu solicitud de %s ha sido aprobada";
    private static final String SUBJECT_REQUEST_REJECTED_FMT = "Tu solicitud de %s ha sido rechazada";
    private static final String SUBJECT_REQUEST_CANCELLED_FMT =
            "Un empleado ha cancelado una solicitud de %s aprobada (recurso liberado)";
    private static final String SUBJECT_ASSIGNMENT_REVOKED = "Tu asignacion fija ha sido revocada";
    private static final String SUBJECT_PASSWORD_RESET = "Tu contrasena temporal de parking";

    /** Palabra humana del recurso segun su tipo, para componer los asuntos (evita literales sueltos). */
    private static final String RESOURCE_WORD_PARKING = "plaza";
    private static final String RESOURCE_WORD_DESK = "puesto";

    private static final String VAR_RECIPIENT_NAME = "recipientName";
    private static final String VAR_RECIPIENT_FULL_NAME = "recipientFullName";
    private static final String VAR_REQUESTER_FULL_NAME = "requesterFullName";
    private static final String VAR_REQUEST_ID = "requestId";
    private static final String VAR_REQUESTED_DATE = "requestedDate";
    private static final String VAR_APPROVAL_NOTE = "approvalNote";
    private static final String VAR_REJECTION_REASON = "rejectionReason";
    private static final String VAR_TEMPORARY_PASSWORD = "temporaryPassword";
    private static final String VAR_RESOURCE_TYPE = "resourceType";
    private static final String VAR_RESOURCE_NUMBER = "resourceNumber";
    private static final String VAR_FLOOR = "floor";
    private static final String VAR_FLOOR_PLAN_ATTACHED = "floorPlanAttached";

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
        return render(admin, subjectFor(SUBJECT_REQUEST_CREATED_FMT, request), TEMPLATE_REQUEST_CREATED, ctx);
    }

    /**
     * Renderiza el email formal de "solicitud aprobada" dirigido al empleado solicitante:
     * saludo con nombre y apellidos, comunicacion de APROBADA, el NUMERO real del recurso
     * asignado (plaza + planta o puesto), la nota del administrador si existe, y el plano de
     * la planta como adjunto. No muestra el {@code requestId} como referencia del recurso.
     *
     * <p>Degrada con elegancia: si {@code resolved} es {@code null} (recurso no localizado) se
     * omite el bloque del numero; si {@code floorPlan} es {@code null} se envia sin adjunto.</p>
     *
     * @param employee  empleado solicitante destinatario
     * @param request   solicitud aprobada (con {@code approvalNote})
     * @param resolved  recurso resuelto a su numero/planta; {@code null} si no se localizo
     * @param floorPlan adjunto del plano de la planta; {@code null} si no se pudo cargar
     * @return el mensaje renderizado (con el adjunto si {@code floorPlan} no es {@code null})
     */
    public EmailMessage renderRequestApproved(
            Employee employee, RequestResponse request,
            ResolvedResource resolved, EmailAttachment floorPlan) {
        Context ctx = baseContext(employee);
        ctx.setVariable(VAR_RECIPIENT_FULL_NAME, fullName(employee));
        ctx.setVariable(VAR_APPROVAL_NOTE, displayableApprovalNote(request.approvalNote()));
        ctx.setVariable(VAR_FLOOR_PLAN_ATTACHED, floorPlan != null);
        if (resolved != null) {
            ctx.setVariable(VAR_RESOURCE_TYPE, resolved.type().name());
            ctx.setVariable(VAR_RESOURCE_NUMBER, resolved.number());
            ctx.setVariable(VAR_FLOOR, resolved.floor());
        }
        EmailMessage message = render(
                employee, subjectFor(SUBJECT_REQUEST_APPROVED_FMT, request), TEMPLATE_REQUEST_APPROVED, ctx);
        return floorPlan == null ? message : message.withAttachments(List.of(floorPlan));
    }

    /**
     * Trata la nota de auto-aprobacion ({@link Request#AUTO_APPROVAL_NOTE}) como ausente para que
     * la plantilla no muestre la linea "Nota del administrador" en las auto-aprobaciones; las notas
     * reales escritas por un admin se conservan. Se compara contra la constante de dominio (no un
     * literal, S1192) con {@code equals} null-safe (S4973).
     *
     * @param approvalNote nota de aprobacion del DTO; puede ser {@code null}
     * @return {@code null} si la nota es la de auto-aprobacion; la nota original en otro caso
     */
    private static String displayableApprovalNote(String approvalNote) {
        return Request.AUTO_APPROVAL_NOTE.equals(approvalNote) ? null : approvalNote;
    }

    /**
     * Renderiza el aviso de "solicitud aprobada cancelada" dirigido a un administrador: el empleado
     * SOLICITANTE ha cancelado su solicitud aprobada y el recurso reservado ha quedado liberado.
     * El cuerpo nombra al solicitante (no al admin destinatario) e identifica el recurso liberado
     * por su NUMERO real (plaza + planta o puesto), resuelto igual que en el correo de aprobacion.
     *
     * <p>Degrada con elegancia: si {@code requester} es {@code null} (solicitante ya borrado) se
     * omite su nombre; si {@code resolved} es {@code null} (recurso no localizado) se omite el
     * bloque del numero.</p>
     *
     * @param admin     administrador destinatario del aviso
     * @param request   solicitud cancelada (estado previo {@code APPROVED})
     * @param requester empleado que solicito y cancelo; {@code null} si ya no existe
     * @param resolved  recurso liberado resuelto a su numero/planta; {@code null} si no se localizo
     * @return el mensaje renderizado
     */
    public EmailMessage renderRequestCancelled(
            Employee admin, RequestResponse request, Employee requester, ResolvedResource resolved) {
        Context ctx = baseContext(admin);
        ctx.setVariable(VAR_REQUESTER_FULL_NAME, requester == null ? null : fullName(requester));
        ctx.setVariable(VAR_REQUESTED_DATE, request.requestedDate());
        if (resolved != null) {
            ctx.setVariable(VAR_RESOURCE_TYPE, resolved.type().name());
            ctx.setVariable(VAR_RESOURCE_NUMBER, resolved.number());
            ctx.setVariable(VAR_FLOOR, resolved.floor());
        }
        return render(admin, subjectFor(SUBJECT_REQUEST_CANCELLED_FMT, request), TEMPLATE_REQUEST_CANCELLED, ctx);
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
        return render(
                employee, subjectFor(SUBJECT_REQUEST_REJECTED_FMT, request), TEMPLATE_REQUEST_REJECTED, ctx);
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

    private static String fullName(Employee employee) {
        return employee.getFirstName() + " " + employee.getLastName();
    }

    /**
     * Compone un asunto que nombra el recurso a partir de una plantilla ({@code %s}) y el tipo de
     * recurso de la solicitud, para que el correo diga "plaza" o "puesto" segun corresponda.
     *
     * @param subjectFormat plantilla del asunto con un unico {@code %s}
     * @param request       solicitud cuyo {@code resourceType} determina la palabra del recurso
     * @return el asunto ya compuesto con "plaza"/"puesto"
     */
    private static String subjectFor(String subjectFormat, RequestResponse request) {
        return String.format(subjectFormat, resourceWord(request.resourceType()));
    }

    /**
     * Traduce el tipo de recurso a su palabra humana para el asunto: {@code DESK} &rarr; "puesto",
     * cualquier otro (incluido {@code PARKING} o {@code null}) &rarr; "plaza" (valor por defecto
     * seguro, S2259).
     *
     * @param resourceType tipo del recurso; puede ser {@code null}
     * @return "puesto" para {@code DESK}, "plaza" en caso contrario
     */
    private static String resourceWord(ResourceType resourceType) {
        return resourceType == ResourceType.DESK ? RESOURCE_WORD_DESK : RESOURCE_WORD_PARKING;
    }

    private EmailMessage render(Employee recipient, String subject, String template, Context ctx) {
        String body = templateEngine.process(template, ctx);
        return new EmailMessage(recipient.getEmail(), subject, body);
    }
}
