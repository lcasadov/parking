package com.aleatica.parking.notification.application;

import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.employee.Role;
import com.aleatica.parking.notification.NotificationEventType;
import com.aleatica.parking.request.dto.RequestResponse;
import org.springframework.stereotype.Service;

/**
 * Traduce cada evento de dominio de notificacion en una o varias {@link NotificationCommand}
 * y las entrega a {@link NotificationDeliveryService}, que las renderiza y envia (o encola
 * para reintento). No renderiza ni resuelve el recurso: eso lo hace {@link NotificationRenderer}
 * de forma compartida entre el envio inmediato y el reintento (evita duplicar el renderizado y
 * el riesgo de divergencia).
 *
 * <p>Resolucion del conjunto de destinatarios (design §Decisions): "nueva solicitud" -> una
 * orden por cada {@code Employee} con {@code role = ADMIN} y {@code active = true} en el
 * momento del envio (excluye admins inactivos; si no hay ninguno, no se emite ninguna orden);
 * el resto de eventos -> el empleado afectado. Cada orden lleva solo el id del destinatario;
 * su email/nombre se resuelven al renderizar.</p>
 */
@Service
public class NotificationDispatcher {

    private final EmployeeRepository employeeRepository;
    private final NotificationDeliveryService deliveryService;

    /**
     * @param employeeRepository repositorio de empleados (resolucion del conjunto de admins)
     * @param deliveryService    servicio de entrega/encolado resiliente
     */
    public NotificationDispatcher(
            EmployeeRepository employeeRepository,
            NotificationDeliveryService deliveryService) {
        this.employeeRepository = employeeRepository;
        this.deliveryService = deliveryService;
    }

    /**
     * Notifica la creacion de una solicitud a todos los administradores activos. Si no hay
     * administradores activos no se emite ninguna orden (el flujo no falla).
     *
     * @param request solicitud creada
     */
    public void requestCreated(RequestResponse request) {
        for (Employee admin : employeeRepository.findByRoleAndActiveTrue(Role.ADMIN)) {
            deliveryService.dispatch(
                    new NotificationCommand(NotificationEventType.REQUEST_CREATED, admin.getId(), request));
        }
    }

    /**
     * Notifica la aprobacion de una solicitud a su empleado solicitante (con tono formal, el
     * numero real del recurso, la nota del admin y el plano adjunto, resueltos al renderizar).
     * Cubre por igual la aprobacion manual y la auto-aprobacion (ambas emiten
     * {@code RequestApprovedEvent}).
     *
     * @param request solicitud aprobada
     */
    public void requestApproved(RequestResponse request) {
        deliveryService.dispatch(
                new NotificationCommand(NotificationEventType.REQUEST_APPROVED, request.employeeId(), request));
    }

    /**
     * Notifica el rechazo de una solicitud a su empleado solicitante (con el motivo).
     *
     * @param request solicitud rechazada
     */
    public void requestRejected(RequestResponse request) {
        deliveryService.dispatch(
                new NotificationCommand(NotificationEventType.REQUEST_REJECTED, request.employeeId(), request));
    }

    /**
     * Notifica al empleado afectado la revocacion de su asignacion fija.
     *
     * @param employeeId empleado afectado
     */
    public void assignmentRevoked(Long employeeId) {
        deliveryService.dispatch(
                new NotificationCommand(NotificationEventType.ASSIGNMENT_REVOKED, employeeId, null));
    }
}
