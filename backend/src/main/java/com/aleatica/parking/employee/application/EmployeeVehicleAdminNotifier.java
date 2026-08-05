package com.aleatica.parking.employee.application;

import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.employee.Role;
import com.aleatica.parking.notification.application.EmailMessage;
import com.aleatica.parking.notification.application.EmailSenderPort;
import com.aleatica.parking.push.PushSubscription;
import com.aleatica.parking.push.PushSubscriptionRepository;
import com.aleatica.parking.push.WebPushSender;
import com.aleatica.parking.systemsettings.application.SystemSettingsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Avisos por email + push del ciclo de vida de los vehículos de empleado (change
 * {@code employee-vehicle-self-service}): a los administradores cuando un empleado envía/modifica o
 * pide borrar un vehículo, y al empleado cuando el administrador lo pone en trámite, lo aprueba o lo
 * rechaza.
 *
 * <p>Notificador <strong>dedicado y best-effort</strong>: reutiliza los puertos de bajo nivel de
 * email ({@link EmailSenderPort}) y push ({@link WebPushSender} + suscripciones), con las mismas
 * reglas de gateado que el resto de avisos (interruptor global + preferencia del empleado + canal
 * habilitado). No pasa por el outbox de reintento de email (endurecimiento posterior). Un fallo de
 * un canal o de un destinatario nunca rompe la operación sobre el vehículo.</p>
 */
@Service
public class EmployeeVehicleAdminNotifier {

    private static final Logger log = LoggerFactory.getLogger(EmployeeVehicleAdminNotifier.class);

    private static final String REVIEW_URL = "/admin/vehicles";
    private static final String EMPLOYEE_URL = "/employee/my-vehicles";

    private final EmployeeRepository employeeRepository;
    private final SystemSettingsService systemSettingsService;
    private final EmailSenderPort emailSenderPort;
    private final PushSubscriptionRepository subscriptionRepository;
    private final WebPushSender webPushSender;
    private final ObjectMapper objectMapper;

    public EmployeeVehicleAdminNotifier(
            EmployeeRepository employeeRepository,
            SystemSettingsService systemSettingsService,
            EmailSenderPort emailSenderPort,
            PushSubscriptionRepository subscriptionRepository,
            WebPushSender webPushSender,
            ObjectMapper objectMapper) {
        this.employeeRepository = employeeRepository;
        this.systemSettingsService = systemSettingsService;
        this.emailSenderPort = emailSenderPort;
        this.subscriptionRepository = subscriptionRepository;
        this.webPushSender = webPushSender;
        this.objectMapper = objectMapper;
    }

    // --- Avisos a los administradores (fan-out a cada admin activo) ---

    /** Un empleado ha dado de alta o modificado un vehículo: queda pendiente de validación. */
    public void vehicleSubmitted(String employeeName, String licensePlate) {
        notifyAdmins(new Notice(
                "Un empleado ha registrado un vehículo pendiente de validación",
                "Vehículo pendiente de validación",
                employeeName + " ha añadido o modificado el vehículo " + licensePlate
                        + ", pendiente de validación.",
                REVIEW_URL));
    }

    /** Un empleado ha pedido borrar un vehículo que estaba en trámite o aprobado. */
    public void deletionRequested(String employeeName, String licensePlate) {
        notifyAdmins(new Notice(
                "Un empleado ha solicitado borrar un vehículo",
                "Solicitud de borrado de vehículo",
                employeeName + " ha solicitado borrar el vehículo " + licensePlate
                        + " (pendiente de borrado).",
                REVIEW_URL));
    }

    // --- Avisos al empleado (resultado de la revisión del administrador) ---

    /** El administrador ha puesto el vehículo del empleado "en trámite". */
    public void vehicleInProgress(Long employeeId, String licensePlate) {
        notifyEmployee(employeeId, new Notice(
                "Tu vehículo está en trámite",
                "Vehículo en trámite",
                "Tu vehículo " + licensePlate + " está en trámite de validación.",
                EMPLOYEE_URL));
    }

    /** El administrador ha aprobado el vehículo del empleado. */
    public void vehicleApproved(Long employeeId, String licensePlate) {
        notifyEmployee(employeeId, new Notice(
                "Tu vehículo ha sido aprobado",
                "Vehículo aprobado",
                "Tu vehículo " + licensePlate + " ha sido aprobado.",
                EMPLOYEE_URL));
    }

    /** El administrador ha rechazado el vehículo del empleado, con motivo. */
    public void vehicleRejected(Long employeeId, String licensePlate, String reason) {
        notifyEmployee(employeeId, new Notice(
                "Tu vehículo ha sido rechazado",
                "Vehículo rechazado",
                "Tu vehículo " + licensePlate + " ha sido rechazado. Motivo: " + reason,
                EMPLOYEE_URL));
    }

    // --- Entrega ---

    private void notifyAdmins(Notice notice) {
        for (Employee admin : employeeRepository.findByRoleAndActiveTrue(Role.ADMIN)) {
            deliver(admin, notice);
        }
    }

    private void notifyEmployee(Long employeeId, Notice notice) {
        employeeRepository.findById(employeeId).ifPresent(employee -> deliver(employee, notice));
    }

    private void deliver(Employee recipient, Notice notice) {
        sendEmail(recipient, notice);
        sendPush(recipient, notice);
    }

    private void sendEmail(Employee recipient, Notice notice) {
        if (!systemSettingsService.emailNotificationsEnabled()
                || !recipient.isEmailNotificationsEnabled()
                || recipient.getEmail() == null) {
            return;
        }
        try {
            emailSenderPort.send(new EmailMessage(recipient.getEmail(), notice.subject(), htmlBody(notice)));
        } catch (RuntimeException ex) {
            log.warn("[vehicle-notify] Fallo al enviar email a {}", recipient.getId(), ex);
        }
    }

    private void sendPush(Employee recipient, Notice notice) {
        if (!webPushSender.isEnabled()
                || !systemSettingsService.pushNotificationsEnabled()
                || !recipient.isPushNotificationsEnabled()) {
            return;
        }
        List<PushSubscription> subscriptions = subscriptionRepository.findByEmployeeId(recipient.getId());
        if (subscriptions.isEmpty()) {
            return;
        }
        String payload = pushPayload(notice);
        for (PushSubscription subscription : subscriptions) {
            sendPushTo(subscription, payload, recipient.getId());
        }
    }

    private void sendPushTo(PushSubscription subscription, String payload, Long recipientId) {
        try {
            WebPushSender.Outcome outcome = webPushSender.send(
                    subscription.getEndpoint(), subscription.getP256dh(), subscription.getAuth(), payload);
            if (outcome == WebPushSender.Outcome.EXPIRED) {
                subscriptionRepository.deleteByEndpoint(subscription.getEndpoint());
            }
        } catch (RuntimeException ex) {
            log.warn("[vehicle-notify] Fallo al enviar push a {}", recipientId, ex);
        }
    }

    private static String htmlBody(Notice notice) {
        return "<p>Hola,</p><p>" + notice.body() + "</p>";
    }

    private String pushPayload(Notice notice) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("title", notice.pushTitle());
        payload.put("body", notice.body());
        payload.put("url", notice.url());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return "{\"title\":\"" + notice.pushTitle() + "\",\"body\":\"\",\"url\":\"" + notice.url() + "\"}";
        }
    }

    /** Contenido de un aviso (asunto de email, título de push, cuerpo común y deep-link). */
    private record Notice(String subject, String pushTitle, String body, String url) {
    }
}
