package com.aleatica.parking.notification.application;

import com.aleatica.parking.desk.DeskRepository;
import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.employee.Role;
import com.aleatica.parking.parkingspace.ParkingSpaceRepository;
import com.aleatica.parking.request.dto.RequestResponse;
import com.aleatica.parking.resource.ResourceType;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
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
    private final NotificationDeliveryService deliveryService;
    private final ParkingSpaceRepository parkingSpaceRepository;
    private final DeskRepository deskRepository;

    /**
     * @param employeeRepository     repositorio de empleados (resolucion de destinatarios)
     * @param renderer               renderizador de plantillas Thymeleaf
     * @param deliveryService        servicio de entrega/encolado resiliente
     * @param parkingSpaceRepository repositorio de plazas (numero/planta de la plaza aprobada)
     * @param deskRepository         repositorio de puestos (numero del puesto aprobado)
     */
    public NotificationDispatcher(
            EmployeeRepository employeeRepository,
            EmailContentRenderer renderer,
            NotificationDeliveryService deliveryService,
            ParkingSpaceRepository parkingSpaceRepository,
            DeskRepository deskRepository) {
        this.employeeRepository = employeeRepository;
        this.renderer = renderer;
        this.deliveryService = deliveryService;
        this.parkingSpaceRepository = parkingSpaceRepository;
        this.deskRepository = deskRepository;
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
     * Notifica la aprobacion de una solicitud a su empleado solicitante con tono formal,
     * el NUMERO real del recurso asignado (plaza + planta o puesto), la nota del admin si
     * existe y el plano de la planta adjunto. Cubre por igual la aprobacion manual del admin
     * y la auto-aprobacion automatica (ambas emiten {@code RequestApprovedEvent}).
     *
     * <p>Degrada con elegancia: si el recurso no se resuelve (borrado/carrera) o el plano no
     * puede cargarse, el correo se envia sin ese dato/adjunto, sin lanzar excepcion que rompa
     * la notificacion ni revertir la aprobacion ya confirmada.</p>
     *
     * @param request solicitud aprobada
     */
    public void requestApproved(RequestResponse request) {
        findRecipient(request.employeeId()).ifPresent(emp -> {
            ResolvedResource resolved = resolveResource(request);
            EmailAttachment floorPlan = loadFloorPlan().orElse(null);
            deliveryService.sendOrQueue(
                    renderer.renderRequestApproved(emp, request, resolved, floorPlan));
        });
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
