package com.aleatica.parking.notification.application;

import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.employee.Role;
import com.aleatica.parking.notification.NotificationEventType;
import com.aleatica.parking.push.PushDeliveryService;
import com.aleatica.parking.request.dto.RequestResponse;
import com.aleatica.parking.systemsettings.application.SystemSettingsService;
import org.springframework.stereotype.Service;

/**
 * Traduce cada evento de dominio de notificacion en una o varias {@link NotificationCommand}
 * y las entrega por los canales habilitados. Resolucion de destinatarios en un unico punto
 * (aqui); entrega en dos canales independientes: email ({@link NotificationDeliveryService},
 * con reintento via outbox) y push ({@link PushDeliveryService}, best-effort).
 *
 * <p>Regla de entrega efectiva por canal (change {@code push-notifications}): email si
 * {@code global.email AND empleado.email}; push si {@code global.push AND empleado.push AND
 * tiene suscripcion} (esto ultimo lo comprueba el propio {@link PushDeliveryService}).</p>
 *
 * <p>Destinatarios (design): "nueva solicitud"/"cancelada"/"lista de espera" -> cada
 * {@code Employee} con {@code role = ADMIN} y {@code active = true}; "cancelacion admin de una
 * aprobada" -> el empleado afectado (design D12); el resto -> el empleado de la solicitud.</p>
 */
@Service
public class NotificationDispatcher {

    private final EmployeeRepository employeeRepository;
    private final NotificationDeliveryService deliveryService;
    private final PushDeliveryService pushDeliveryService;
    private final SystemSettingsService systemSettingsService;

    public NotificationDispatcher(
            EmployeeRepository employeeRepository,
            NotificationDeliveryService deliveryService,
            PushDeliveryService pushDeliveryService,
            SystemSettingsService systemSettingsService) {
        this.employeeRepository = employeeRepository;
        this.deliveryService = deliveryService;
        this.pushDeliveryService = pushDeliveryService;
        this.systemSettingsService = systemSettingsService;
    }

    /** Nueva solicitud pendiente -> todos los administradores activos. */
    public void requestCreated(RequestResponse request) {
        deliverToActiveAdmins(NotificationEventType.REQUEST_CREATED, request);
    }

    /** Cancelacion (recurso liberado) -> todos los administradores activos. */
    public void requestCancelled(RequestResponse request) {
        deliverToActiveAdmins(NotificationEventType.REQUEST_CANCELLED, request);
    }

    /** Aprobacion/auto-aprobacion -> empleado solicitante. */
    public void requestApproved(RequestResponse request) {
        deliver(new NotificationCommand(
                NotificationEventType.REQUEST_APPROVED, request.employeeId(), request));
    }

    /** Rechazo -> empleado solicitante. */
    public void requestRejected(RequestResponse request) {
        deliver(new NotificationCommand(
                NotificationEventType.REQUEST_REJECTED, request.employeeId(), request));
    }

    /** Revocacion de asignacion fija -> empleado afectado. */
    public void assignmentRevoked(Long employeeId) {
        deliver(new NotificationCommand(NotificationEventType.ASSIGNMENT_REVOKED, employeeId, null));
    }

    /** Asignacion puntual del admin -> empleado destino (plantilla propia). */
    public void requestAdminAssigned(RequestResponse request) {
        deliver(new NotificationCommand(
                NotificationEventType.REQUEST_ADMIN_ASSIGNED, request.employeeId(), request));
    }

    /**
     * Cancelacion administrativa de una reserva {@code APPROVED} -> el <strong>empleado
     * afectado</strong> (change {@code push-notifications}, design D12). El aviso de liberacion
     * del recurso a los admins lo cubre {@link #requestCancelled(RequestResponse)}.
     *
     * @param request reserva cancelada por el admin (estado previo {@code APPROVED})
     */
    public void requestAdminCancelled(RequestResponse request) {
        deliver(new NotificationCommand(
                NotificationEventType.REQUEST_ADMIN_CANCELLED, request.employeeId(), request));
    }

    /** Hueco de lista de espera disponible -> todos los administradores activos. */
    public void waitlistAvailable(RequestResponse topWaitlistedRequest) {
        deliverToActiveAdmins(NotificationEventType.WAITLIST_AVAILABLE, topWaitlistedRequest);
    }

    /** Emite una orden por cada administrador activo (fan-out); si no hay, no emite ninguna. */
    private void deliverToActiveAdmins(NotificationEventType eventType, RequestResponse request) {
        for (Employee admin : employeeRepository.findByRoleAndActiveTrue(Role.ADMIN)) {
            deliver(new NotificationCommand(eventType, admin.getId(), request));
        }
    }

    /** Entrega una orden por los canales habilitados: email (gateado) + push (auto-gateado). */
    private void deliver(NotificationCommand command) {
        if (emailEnabledFor(command.recipientEmployeeId())) {
            deliveryService.dispatch(command);
        }
        pushDeliveryService.dispatch(command);
    }

    /** Email habilitado para el destinatario = interruptor global AND preferencia del empleado. */
    private boolean emailEnabledFor(Long employeeId) {
        if (!systemSettingsService.emailNotificationsEnabled()) {
            return false;
        }
        return employeeRepository.findById(employeeId)
                .map(Employee::isEmailNotificationsEnabled)
                .orElse(false);
    }
}
