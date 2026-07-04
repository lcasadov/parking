package com.aleatica.parking.notification.application;

import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.employee.Role;
import com.aleatica.parking.request.dto.RequestResponse;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Resuelve destinatarios y contenido de cada evento de notificacion y delega el envio
 * (o encolado) en {@link NotificationDeliveryService}.
 *
 * <p>Resolucion de destinatarios (design §Decisions): "nueva solicitud" -> todos los
 * {@code Employee} con {@code role = ADMIN} y {@code active = true} en el momento del
 * envio (excluye admins inactivos; si no hay ninguno, no se encola nada); el resto de
 * eventos -> el empleado afectado. Nunca expone entidades JPA a la capa web; aqui las usa
 * solo para leer email/nombre del destinatario.</p>
 */
@Service
public class NotificationDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationDispatcher.class);

    private static final String MSG_RECIPIENT_NOT_FOUND =
            "Destinatario de notificacion no encontrado: employeeId={}";

    private final EmployeeRepository employeeRepository;
    private final EmailContentRenderer renderer;
    private final NotificationDeliveryService deliveryService;

    /**
     * @param employeeRepository repositorio de empleados (resolucion de destinatarios)
     * @param renderer           renderizador de plantillas Thymeleaf
     * @param deliveryService    servicio de entrega/encolado resiliente
     */
    public NotificationDispatcher(
            EmployeeRepository employeeRepository,
            EmailContentRenderer renderer,
            NotificationDeliveryService deliveryService) {
        this.employeeRepository = employeeRepository;
        this.renderer = renderer;
        this.deliveryService = deliveryService;
    }

    /**
     * Notifica la creacion de una solicitud a todos los administradores activos. Si no hay
     * administradores activos no se envia ni encola nada (el flujo no falla).
     *
     * @param request solicitud creada
     */
    public void requestCreated(RequestResponse request) {
        List<Employee> admins = employeeRepository.findByRoleAndActiveTrue(Role.ADMIN);
        for (Employee admin : admins) {
            deliveryService.sendOrQueue(renderer.renderRequestCreated(admin, request));
        }
    }

    /**
     * Notifica la aprobacion de una solicitud a su empleado solicitante (con la nota).
     *
     * @param request solicitud aprobada
     */
    public void requestApproved(RequestResponse request) {
        findRecipient(request.employeeId())
                .ifPresent(emp -> deliveryService.sendOrQueue(renderer.renderRequestApproved(emp, request)));
    }

    /**
     * Notifica el rechazo de una solicitud a su empleado solicitante (con el motivo).
     *
     * @param request solicitud rechazada
     */
    public void requestRejected(RequestResponse request) {
        findRecipient(request.employeeId())
                .ifPresent(emp -> deliveryService.sendOrQueue(renderer.renderRequestRejected(emp, request)));
    }

    /**
     * Notifica al empleado afectado la revocacion de su asignacion fija.
     *
     * @param employeeId empleado afectado
     */
    public void assignmentRevoked(Long employeeId) {
        findRecipient(employeeId)
                .ifPresent(emp -> deliveryService.sendOrQueue(renderer.renderAssignmentRevoked(emp)));
    }

    private Optional<Employee> findRecipient(Long employeeId) {
        Optional<Employee> employee = employeeRepository.findById(employeeId);
        if (employee.isEmpty()) {
            LOG.warn(MSG_RECIPIENT_NOT_FOUND, employeeId);
        }
        return employee;
    }
}
