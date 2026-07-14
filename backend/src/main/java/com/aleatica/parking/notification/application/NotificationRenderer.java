package com.aleatica.parking.notification.application;

import com.aleatica.parking.desk.DeskRepository;
import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.parkingspace.ParkingSpaceRepository;
import com.aleatica.parking.request.dto.RequestResponse;
import com.aleatica.parking.resource.ResourceType;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Resuelve el destinatario y el recurso de una {@link NotificationCommand} y renderiza el
 * {@link EmailMessage} con la plantilla vigente, delegando en {@link EmailContentRenderer}.
 *
 * <p>Es el punto de renderizado <strong>compartido</strong> por el flujo inmediato
 * ({@link NotificationDispatcher} -> {@code NotificationDeliveryService.dispatch}) y por el
 * reintento ({@code NotificationDeliveryService.retryPending}): al re-renderizar en cada
 * intento, un correo reenviado usa siempre la plantilla actual y nunca contenido obsoleto.
 * Al no duplicar el renderizado entre ambos caminos, se elimina el riesgo de divergencia que
 * causaba el bug del outbox.</p>
 *
 * <p>Degrada con elegancia: si el destinatario ya no existe devuelve {@link Optional#empty()}
 * (el flujo inmediato no envia; el reintento marca la fila {@code FAILED}); si el recurso o el
 * plano no se resuelven, el correo se renderiza sin ese dato/adjunto. Nunca expone entidades
 * JPA a la capa web; las usa solo para leer email/nombre y numero del recurso.</p>
 */
@Component
public class NotificationRenderer {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationRenderer.class);

    private static final String MSG_RECIPIENT_NOT_FOUND =
            "Destinatario de notificacion no encontrado: employeeId={}";
    private static final String MSG_RESOURCE_NOT_RESOLVED =
            "No se pudo resolver el recurso del correo de aprobacion: resourceId={}, type={}";
    private static final String MSG_FLOOR_PLAN_FAILED =
            "No se pudo cargar el plano adjunto del correo de aprobacion; se envia sin adjunto";

    /** Recurso del classpath con el plano de la planta que se adjunta al correo de aprobacion. */
    private static final String FLOOR_PLAN_RESOURCE = "templates/email/floor-plan.png";
    private static final String FLOOR_PLAN_FILENAME = "floor-plan.png";
    private static final String FLOOR_PLAN_CONTENT_TYPE = "image/png";

    private final EmployeeRepository employeeRepository;
    private final EmailContentRenderer renderer;
    private final ParkingSpaceRepository parkingSpaceRepository;
    private final DeskRepository deskRepository;

    /**
     * @param employeeRepository     repositorio de empleados (resolucion del destinatario)
     * @param renderer               renderizador de plantillas Thymeleaf
     * @param parkingSpaceRepository repositorio de plazas (numero/planta de la plaza aprobada)
     * @param deskRepository         repositorio de puestos (numero del puesto aprobado)
     */
    public NotificationRenderer(
            EmployeeRepository employeeRepository,
            EmailContentRenderer renderer,
            ParkingSpaceRepository parkingSpaceRepository,
            DeskRepository deskRepository) {
        this.employeeRepository = employeeRepository;
        this.renderer = renderer;
        this.parkingSpaceRepository = parkingSpaceRepository;
        this.deskRepository = deskRepository;
    }

    /**
     * Renderiza el correo de la orden dada resolviendo su destinatario actual y, para las
     * aprobaciones, el numero del recurso y el plano adjunto.
     *
     * @param command orden de notificacion a renderizar
     * @return el mensaje renderizado, o {@link Optional#empty()} si el destinatario no existe
     */
    public Optional<EmailMessage> render(NotificationCommand command) {
        return findRecipient(command.recipientEmployeeId())
                .map(employee -> renderFor(command, employee));
    }

    private EmailMessage renderFor(NotificationCommand command, Employee employee) {
        RequestResponse request = command.request();
        return switch (command.eventType()) {
            case REQUEST_CREATED -> renderer.renderRequestCreated(employee, request);
            case REQUEST_APPROVED -> renderApproved(employee, request);
            case REQUEST_REJECTED -> renderer.renderRequestRejected(employee, request);
            case ASSIGNMENT_REVOKED -> renderer.renderAssignmentRevoked(employee);
        };
    }

    private EmailMessage renderApproved(Employee employee, RequestResponse request) {
        ResolvedResource resolved = resolveResource(request);
        EmailAttachment floorPlan = loadFloorPlan().orElse(null);
        return renderer.renderRequestApproved(employee, request, resolved, floorPlan);
    }

    /**
     * Resuelve el numero (y planta cuando aplique) del recurso asignado a partir de su
     * {@code resourceId} ({@code request.parkingSpaceId()}) y su {@code resourceType}. Devuelve
     * {@code null} si no hay recurso asignado o no se encuentra (manejo seguro de {@code Optional},
     * S3655/S2259), degradando el correo sin romper la notificacion.
     */
    private ResolvedResource resolveResource(RequestResponse request) {
        Long resourceId = request.parkingSpaceId();
        ResourceType type = request.resourceType();
        if (resourceId == null || type == null) {
            LOG.warn(MSG_RESOURCE_NOT_RESOLVED, resourceId, type);
            return null;
        }
        ResolvedResource resolved = switch (type) {
            case PARKING -> parkingSpaceRepository.findById(resourceId)
                    .map(space -> new ResolvedResource(type, space.getNumber(), space.floor()))
                    .orElse(null);
            case DESK -> deskRepository.findById(resourceId)
                    .map(desk -> new ResolvedResource(type, desk.getNumber(), null))
                    .orElse(null);
        };
        if (resolved == null) {
            LOG.warn(MSG_RESOURCE_NOT_RESOLVED, resourceId, type);
        }
        return resolved;
    }

    /**
     * Carga el plano de la planta del classpath a un {@link EmailAttachment} en memoria
     * (try-with-resources sobre el {@code InputStream}, S2095). Devuelve {@link Optional#empty()}
     * si el recurso no existe o falla la lectura, para enviar el correo sin adjunto.
     */
    private Optional<EmailAttachment> loadFloorPlan() {
        ClassPathResource resource = new ClassPathResource(FLOOR_PLAN_RESOURCE);
        try (InputStream in = resource.getInputStream()) {
            byte[] content = in.readAllBytes();
            return Optional.of(
                    new EmailAttachment(FLOOR_PLAN_FILENAME, FLOOR_PLAN_CONTENT_TYPE, content));
        } catch (IOException ex) {
            LOG.warn(MSG_FLOOR_PLAN_FAILED, ex);
            return Optional.empty();
        }
    }

    private Optional<Employee> findRecipient(Long employeeId) {
        Optional<Employee> employee = employeeRepository.findById(employeeId);
        if (employee.isEmpty()) {
            LOG.warn(MSG_RECIPIENT_NOT_FOUND, employeeId);
        }
        return employee;
    }
}
