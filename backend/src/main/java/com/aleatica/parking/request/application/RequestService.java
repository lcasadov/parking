package com.aleatica.parking.request.application;

import com.aleatica.parking.audit.AuditEntry;
import com.aleatica.parking.audit.AuditRecorder;
import com.aleatica.parking.auth.domain.ClockPort;
import com.aleatica.parking.availability.application.AvailabilityService;
import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeCategory;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.employee.dto.PageResponse;
import com.aleatica.parking.notification.event.RequestApprovedEvent;
import com.aleatica.parking.notification.event.RequestCreatedEvent;
import com.aleatica.parking.notification.event.RequestRejectedEvent;
import com.aleatica.parking.parkingspace.ParkingSpace;
import com.aleatica.parking.request.domain.RejectionReasonCode;
import com.aleatica.parking.request.domain.Request;
import com.aleatica.parking.request.domain.RequestRepositoryPort;
import com.aleatica.parking.request.domain.RequestStatus;
import com.aleatica.parking.request.dto.RequestApproveRequest;
import com.aleatica.parking.request.dto.RequestCreateRequest;
import com.aleatica.parking.request.dto.RequestRejectRequest;
import com.aleatica.parking.request.dto.RequestResponse;
import com.aleatica.parking.resource.BookableResource;
import com.aleatica.parking.resource.ResourceResolvers;
import com.aleatica.parking.resource.ResourceType;
import com.aleatica.parking.systemsettings.application.SystemSettingsService;
import com.aleatica.parking.systemsettings.domain.ApprovalMode;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de solicitudes puntuales: creacion (con ventana y unicidad), listados
 * (propios y pendientes FIFO), cancelacion por el empleado y resolucion
 * (aprobar/rechazar) por el administrador.
 *
 * <p>Arquitectura hexagonal: la logica de negocio no depende de la web y nunca
 * devuelve entidades JPA (convierte a DTO antes de salir, OWASP API3). La unicidad
 * {@code PENDING} y la disponibilidad de plaza {@code APPROVED} las garantizan los
 * indices unicos filtrados de la BD; el servicio hace una comprobacion previa (mensaje
 * claro) pero deja que la violacion de indice se traduzca a 409 bajo concurrencia
 * (design §Decisions). La verificacion de pertenencia (BOLA) vive aqui, no solo en el
 * RBAC del controlador. Las notificaciones se disparan {@code AFTER_COMMIT} via eventos de
 * dominio ({@link RequestCreatedEvent}, {@link RequestApprovedEvent},
 * {@link RequestRejectedEvent}) consumidos por la capability {@code notifications}.</p>
 */
@Service
public class RequestService {

    /** Ventana de solicitud: hoy..hoy+14 dias naturales (extremos inclusive). */
    private static final int WINDOW_DAYS = 14;
    /** Longitud minima del texto libre cuando el motivo de rechazo es {@code OTHER}. */
    private static final int MIN_OTHER_REASON_LENGTH = 5;

    /**
     * Categorias de rango ALTO (hasta Director N2): prefieren las plantas MAS ALTAS en la
     * auto-asignacion (design §D3). El resto (Gerente, Mando intermedio, Empleado) prefiere las
     * mas bajas.
     */
    private static final Set<EmployeeCategory> HIGH_CATEGORIES = EnumSet.of(
            EmployeeCategory.CEO, EmployeeCategory.CONSEJO,
            EmployeeCategory.DIRECTOR_N1, EmployeeCategory.DIRECTOR_N2);

    private static final String MSG_ACTOR_NOT_FOUND = "Usuario de sesion no encontrado: ";
    private static final String MSG_REQUEST_NOT_FOUND = "Solicitud no encontrada: ";
    private static final String MSG_RESOURCE_NOT_FOUND = "Recurso no encontrado: ";
    private static final String MSG_OUTSIDE_WINDOW =
            "La fecha solicitada debe estar entre hoy y hoy+14 dias";
    private static final String MSG_ALREADY_PENDING =
            "Ya existe una solicitud pendiente para esa fecha";
    private static final String MSG_NOT_OWNER = "No puede operar sobre la solicitud de otro empleado";
    private static final String MSG_NOT_PENDING =
            "La solicitud no esta pendiente; no admite esta transicion";
    private static final String MSG_NOT_CANCELLABLE =
            "La solicitud no admite cancelacion; solo se cancela una PENDING o una APPROVED de fecha futura";
    private static final String MSG_NOT_REJECTABLE =
            "La solicitud no admite rechazo; solo se rechaza una solicitud PENDING o APPROVED";
    private static final String MSG_SPACE_UNAVAILABLE =
            "La plaza no esta disponible para la fecha solicitada";
    private static final String MSG_DESK_UNAVAILABLE =
            "El puesto no esta disponible para la fecha solicitada";
    private static final String MSG_NO_AVAILABILITY =
            "No hay ninguna plaza libre para la fecha solicitada";
    private static final String MSG_REASON_REQUIRED =
            "El motivo libre es obligatorio (>=5 caracteres) cuando el codigo es OTHER";

    /** Accion de auditoria: el empleado cancela una solicitud APPROVED y libera el recurso. */
    private static final String AUDIT_ACTION_RELEASE = "CANCEL_APPROVED_REQUEST";
    /** Tipo de entidad auditada en la liberacion por cancelacion. */
    private static final String AUDIT_ENTITY_REQUEST = "Request";

    private final RequestRepositoryPort requestRepository;
    private final EmployeeRepository employeeRepository;
    private final ResourceResolvers resourceResolvers;
    private final AvailabilityService availabilityService;
    private final SystemSettingsService systemSettingsService;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditRecorder auditRecorder;
    private final ClockPort clock;

    /**
     * @param requestRepository     puerto de persistencia de solicitudes (dominio)
     * @param employeeRepository    repositorio de empleados (solicitante/resolutor)
     * @param resourceResolvers     resolutor polimorfico de recursos (integridad al aprobar,
     *                              plaza o puesto segun el tipo de la solicitud)
     * @param availabilityService   servicio de disponibilidad consolidado (regla unica al aprobar
     *                              y auto-asignacion de plaza)
     * @param systemSettingsService ajuste global (modo de aprobacion MANUAL/AUTOMATIC)
     * @param eventPublisher        publicador de eventos de notificacion
     * @param auditRecorder         puerto de auditoria para trazar la liberacion por cancelacion
     *                              de una solicitud APPROVED (change {@code cancel-approved-request})
     * @param clock                 reloj inyectable para ventana y marcas de tiempo
     */
    public RequestService(
            RequestRepositoryPort requestRepository,
            EmployeeRepository employeeRepository,
            ResourceResolvers resourceResolvers,
            AvailabilityService availabilityService,
            SystemSettingsService systemSettingsService,
            ApplicationEventPublisher eventPublisher,
            AuditRecorder auditRecorder,
            ClockPort clock) {
        this.requestRepository = requestRepository;
        this.employeeRepository = employeeRepository;
        this.resourceResolvers = resourceResolvers;
        this.availabilityService = availabilityService;
        this.systemSettingsService = systemSettingsService;
        this.eventPublisher = eventPublisher;
        this.auditRecorder = auditRecorder;
        this.clock = clock;
    }

    /**
     * Crea una solicitud para el empleado de la sesion y una fecha dentro de la ventana
     * hoy..hoy+14, garantizando una unica solicitud {@code PENDING} por empleado/tipo/fecha.
     *
     * <p>El comportamiento se ramifica por el <strong>modo de aprobacion global</strong>
     * ({@link ApprovalMode}, change {@code request-auto-assignment}):</p>
     * <ul>
     *   <li>{@code MANUAL}: la solicitud nace {@code PENDING} sin recurso (comportamiento
     *       clasico) y la resuelve el ADMIN.</li>
     *   <li>{@code AUTOMATIC} + {@code PARKING}: se auto-asigna una plaza libre por
     *       categoria/planta y la solicitud nace {@code APPROVED} en la misma transaccion; si no
     *       hay ninguna plaza libre se responde 409 {@code NO_AVAILABILITY} sin crear la
     *       solicitud.</li>
     *   <li>{@code AUTOMATIC} + {@code DESK} con puesto elegido: la solicitud nace
     *       {@code APPROVED} con ese puesto (409 si no esta disponible).</li>
     * </ul>
     *
     * @param requesterLogin login del empleado solicitante (principal de la sesion)
     * @param request        fecha solicitada, tipo de recurso y (opcional) puesto elegido
     * @return la solicitud creada (DTO)
     * @throws OutsideRequestWindowException    si la fecha esta fuera de la ventana
     * @throws DuplicatePendingRequestException si ya hay una solicitud pendiente esa fecha
     * @throws NoAvailabilityException          si en modo automatico no hay ninguna plaza libre
     * @throws SpaceUnavailableException        si en modo automatico el puesto elegido no esta libre
     */
    @Transactional
    public RequestResponse create(String requesterLogin, RequestCreateRequest request) {
        Employee employee = loadEmployee(requesterLogin);
        Long employeeId = employee.getId();
        Instant now = clock.now();
        LocalDate requestedDate = request.requestedDate();
        ResourceType resourceType = request.resourceTypeOrDefault();
        requireWithinWindow(requestedDate, now);
        requireNoPendingDuplicate(employeeId, resourceType, requestedDate);
        if (systemSettingsService.approvalMode() == ApprovalMode.AUTOMATIC) {
            return createAutomatic(employee, request, resourceType, requestedDate, now);
        }
        return createManual(employeeId, resourceType, requestedDate, now);
    }

    private RequestResponse createManual(
            Long employeeId, ResourceType resourceType, LocalDate requestedDate, Instant now) {
        Request saved = requestRepository.saveAndFlush(
                Request.create(employeeId, resourceType, requestedDate, now));
        RequestResponse response = RequestResponse.from(saved);
        eventPublisher.publishEvent(new RequestCreatedEvent(response));
        return response;
    }

    private RequestResponse createAutomatic(
            Employee employee, RequestCreateRequest request, ResourceType resourceType,
            LocalDate requestedDate, Instant now) {
        // Para DESK sin puesto elegido no hay auto-asignacion (design non-goal): se cae al flujo
        // manual (nace PENDING) para no dejar la creacion sin recurso ni un estado incoherente.
        if (resourceType == ResourceType.DESK && request.resourceId() == null) {
            return createManual(employee.getId(), resourceType, requestedDate, now);
        }
        Long resourceId = resourceType == ResourceType.DESK
                ? chosenDesk(request.resourceId(), requestedDate)
                : autoAssignedSpaceId(employee.getCategory(), requestedDate);
        return autoApprove(employee.getId(), resourceType, resourceId, requestedDate, now);
    }

    private Long chosenDesk(Long deskId, LocalDate requestedDate) {
        requireResourceExists(deskId, ResourceType.DESK);
        if (availabilityService.isSpaceTakenForDate(deskId, ResourceType.DESK, requestedDate)) {
            throw new SpaceUnavailableException(MSG_DESK_UNAVAILABLE);
        }
        return deskId;
    }

    private Long autoAssignedSpaceId(EmployeeCategory category, LocalDate requestedDate) {
        return autoAssignParkingSpace(category, requestedDate)
                .map(ParkingSpace::getId)
                .orElseThrow(() -> new NoAvailabilityException(MSG_NO_AVAILABILITY));
    }

    private RequestResponse autoApprove(
            Long employeeId, ResourceType resourceType, Long resourceId,
            LocalDate requestedDate, Instant now) {
        Request request = Request.createForResource(employeeId, resourceType, resourceId, requestedDate, now);
        // El actor de una auto-aprobacion es el propio sistema: resolvedById nulo, nota "auto".
        request.approve(resourceId, null, Request.AUTO_APPROVAL_NOTE, now);
        Request saved = requestRepository.saveAndFlush(request);
        RequestResponse response = RequestResponse.from(saved);
        eventPublisher.publishEvent(new RequestApprovedEvent(response));
        return response;
    }

    /**
     * Auto-asigna una plaza LIBRE para la fecha segun la preferencia de planta de la categoria:
     * las categorias ALTAS (hasta Director N2) prefieren las plantas mas altas; el resto, las mas
     * bajas. Recorre el espacio completo de plantas (fallback total): si existe cualquier plaza
     * libre se asigna, la categoria solo fija el orden de preferencia. Dentro de una planta la
     * eleccion es determinista (menor {@code number}).
     *
     * @param category      categoria del empleado (determina el orden de preferencia de plantas)
     * @param requestedDate fecha para la que se asigna la plaza
     * @return la plaza elegida, o vacio si no hay ninguna plaza libre en ninguna planta
     */
    Optional<ParkingSpace> autoAssignParkingSpace(EmployeeCategory category, LocalDate requestedDate) {
        List<ParkingSpace> free = availabilityService.freeParkingSpacesForDate(requestedDate);
        if (free.isEmpty()) {
            return Optional.empty();
        }
        boolean high = isHighCategory(category);
        Comparator<ParkingSpace> byFloorThenNumber = Comparator
                .comparingInt((ParkingSpace space) -> floorPreferenceKey(space, high))
                .thenComparing(ParkingSpace::getNumber);
        return free.stream().min(byFloorThenNumber);
    }

    /**
     * Indica si la categoria pertenece al grupo de rango ALTO (CEO, Consejo, Director N1,
     * Director N2), que prefiere las plantas mas altas en la auto-asignacion (design §D3).
     *
     * @param category categoria del empleado
     * @return {@code true} si es una categoria alta
     */
    static boolean isHighCategory(EmployeeCategory category) {
        return HIGH_CATEGORIES.contains(category);
    }

    /**
     * Clave de orden de planta segun la preferencia: para las categorias altas se niega la planta
     * (mayor planta = clave menor = mas preferente); para el resto se usa la planta tal cual
     * (menor planta = clave menor = mas preferente).
     */
    private static int floorPreferenceKey(ParkingSpace space, boolean high) {
        int floor = space.floor() == null ? 0 : space.floor();
        return high ? -floor : floor;
    }

    /**
     * Lista de forma paginada las solicitudes del empleado de la sesion (verificacion
     * de pertenencia implicita: solo las propias), con filtro opcional por estado.
     *
     * @param requesterLogin login del empleado (principal de la sesion)
     * @param status         estado por el que filtrar; {@code null} para todos
     * @param pageable       pagina y tamano solicitados
     * @return pagina de solicitudes propias (DTO)
     */
    @Transactional(readOnly = true)
    public PageResponse<RequestResponse> listMine(
            String requesterLogin, RequestStatus status, Pageable pageable) {
        Long employeeId = resolveEmployeeId(requesterLogin);
        Page<Request> page = status == null
                ? requestRepository.findByEmployeeId(employeeId, pageable)
                : requestRepository.findByEmployeeIdAndStatus(employeeId, status, pageable);
        return enrichWithResourceNumber(page);
    }

    /**
     * Convierte la pagina de solicitudes a DTO resolviendo, para las {@code APPROVED} con recurso
     * asignado, el NUMERO humano del recurso (plaza/puesto) y su planta, de modo que el empleado
     * vea "Plaza 3005"/"Puesto 12" y nunca el {@code resource_id} interno de BD. La resolucion es
     * en lote por tipo (una consulta por {@code PARKING}/{@code DESK}, sin N+1); las solicitudes
     * {@code PENDING}/{@code REJECTED}/{@code CANCELLED} o cuyo recurso ya no exista degradan a
     * {@code resourceNumber = null} (el frontend muestra "&mdash;").
     */
    private PageResponse<RequestResponse> enrichWithResourceNumber(Page<Request> page) {
        List<Request> requests = page.getContent();
        Map<Long, BookableResource> parking = resolveApprovedResources(requests, ResourceType.PARKING);
        Map<Long, BookableResource> desks = resolveApprovedResources(requests, ResourceType.DESK);
        return PageResponse.from(page, request -> toResponse(request, parking, desks));
    }

    private Map<Long, BookableResource> resolveApprovedResources(
            List<Request> requests, ResourceType resourceType) {
        List<Long> ids = requests.stream()
                .filter(request -> request.getStatus() == RequestStatus.APPROVED)
                .filter(request -> request.getResourceType() == resourceType
                        && request.getResourceId() != null)
                .map(Request::getResourceId)
                .distinct()
                .toList();
        return resourceResolvers.resolveAll(ids, resourceType);
    }

    private static RequestResponse toResponse(
            Request request, Map<Long, BookableResource> parking, Map<Long, BookableResource> desks) {
        RequestResponse base = RequestResponse.from(request);
        if (request.getStatus() != RequestStatus.APPROVED
                || request.getResourceId() == null || request.getResourceType() == null) {
            return base;
        }
        Map<Long, BookableResource> source =
                request.getResourceType() == ResourceType.PARKING ? parking : desks;
        BookableResource resource = source.get(request.getResourceId());
        if (resource == null) {
            return base;
        }
        return base.withResource(resource.getNumber(), resource.getFloor());
    }

    /**
     * Lista de forma paginada las solicitudes pendientes en orden FIFO
     * ({@code created_at ASC}); reservado al {@code ADMIN}.
     *
     * @param pageable pagina y tamano solicitados
     * @return pagina de solicitudes pendientes en orden FIFO (DTO)
     */
    @Transactional(readOnly = true)
    public PageResponse<RequestResponse> listPending(Pageable pageable) {
        return PageResponse.from(
                requestRepository.findByStatusOrderByCreatedAtAsc(RequestStatus.PENDING, pageable),
                RequestResponse::from);
    }

    /**
     * Lista de forma paginada las solicitudes por estado para la bandeja del {@code ADMIN},
     * en orden de actividad reciente ({@code created_at DESC}); reservado al {@code ADMIN}.
     *
     * <p>Complementa a {@link #listPending(Pageable)} (que sirve la pestana FIFO de pendientes)
     * habilitando las pestanas "aprobadas", "rechazadas" y "todas" de la vista admin. Un
     * {@code status} nulo devuelve todas las solicitudes (pestana "todas"). Igual que el listado
     * propio, resuelve el numero humano del recurso para las {@code APPROVED} (plaza/puesto), de
     * modo que el administrador vea "Plaza 3005"/"Puesto 12" y no el {@code resource_id} interno.</p>
     *
     * @param status   estado por el que filtrar; {@code null} para todas
     * @param pageable pagina y tamano solicitados
     * @return pagina de solicitudes en ese estado (o todas), enriquecida con el numero de recurso
     */
    @Transactional(readOnly = true)
    public PageResponse<RequestResponse> listByStatus(RequestStatus status, Pageable pageable) {
        Page<Request> page = status == null
                ? requestRepository.findAllByOrderByCreatedAtDesc(pageable)
                : requestRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        return enrichWithResourceNumber(page);
    }

    /**
     * Devuelve el detalle de una solicitud por su id; reservado al {@code ADMIN}.
     *
     * @param id identificador de la solicitud
     * @return la solicitud (DTO)
     * @throws EntityNotFoundException si no existe
     */
    @Transactional(readOnly = true)
    public RequestResponse get(Long id) {
        return RequestResponse.from(loadRequest(id));
    }

    /**
     * Cancela la solicitud propia del empleado. Admite dos casos (change
     * {@code cancel-approved-request}): una solicitud {@code PENDING} para cualquier fecha
     * (comportamiento clasico) y una solicitud {@code APPROVED} cuya {@code requestedDate} es
     * futura (hoy inclusive), en cuyo caso la cancelacion <strong>libera el recurso</strong> (la
     * fila deja de contar como {@code APPROVED} y la plaza/puesto reaparece en disponibilidad) y
     * queda trazada en auditoria. No se puede cancelar una {@code APPROVED} de fecha pasada ni una
     * solicitud en estado terminal ({@code REJECTED}/{@code CANCELLED}), que responden 409. El
     * "hoy" se deriva del mismo reloj y zona ({@code ZoneOffset.UTC}) que la ventana de creacion.
     *
     * @param id             identificador de la solicitud
     * @param requesterLogin login del empleado (principal de la sesion)
     * @return la solicitud cancelada (DTO)
     * @throws EntityNotFoundException si no existe
     * @throws AccessDeniedException   si el solicitante no es el propietario (BOLA)
     * @throws RequestStateException   si la solicitud no admite cancelacion (terminal o
     *                                 {@code APPROVED} de fecha pasada)
     */
    @Transactional
    public RequestResponse cancel(Long id, String requesterLogin) {
        Request request = loadRequest(id);
        Long employeeId = resolveEmployeeId(requesterLogin);
        if (!request.getEmployeeId().equals(employeeId)) {
            throw new AccessDeniedException(MSG_NOT_OWNER);
        }
        LocalDate today = LocalDate.ofInstant(clock.now(), ZoneOffset.UTC);
        requireCancellable(request, today);
        boolean releasesResource = request.isApproved();
        request.cancel();
        RequestResponse response = RequestResponse.from(requestRepository.save(request));
        if (releasesResource) {
            recordRelease(employeeId, response);
        }
        return response;
    }

    /**
     * Registra en auditoria la cancelacion de una solicitud {@code APPROVED} como liberacion del
     * recurso por el empleado (change {@code cancel-approved-request} §D4). Reutiliza el puerto de
     * auditoria generico {@link AuditRecorder}, que persiste en su propia transaccion
     * ({@code REQUIRES_NEW}, best-effort): un fallo de registro no revierte la cancelacion.
     */
    private void recordRelease(Long employeeId, RequestResponse response) {
        auditRecorder.record(new AuditEntry(
                employeeId, AUDIT_ACTION_RELEASE, AUDIT_ENTITY_REQUEST, response.id(), null));
    }

    /**
     * Aprueba una solicitud {@code PENDING} asignando una plaza disponible para la
     * fecha, atribuyendo el resolutor y la nota opcional. La comprobacion de
     * disponibilidad y la asignacion ocurren en la misma transaccion; la colision de
     * concurrencia entre administradores la rechaza el indice unico filtrado (409).
     *
     * @param id         identificador de la solicitud
     * @param body       plaza a asignar y nota opcional
     * @param adminLogin login del administrador que resuelve
     * @return la solicitud aprobada (DTO)
     * @throws EntityNotFoundException   si la solicitud o la plaza no existen
     * @throws RequestStateException     si la solicitud no esta en {@code PENDING}
     * @throws SpaceUnavailableException si la plaza no esta disponible esa fecha
     */
    @Transactional
    public RequestResponse approve(Long id, RequestApproveRequest body, String adminLogin) {
        Request request = loadRequest(id);
        requirePending(request);
        Long parkingSpaceId = body.parkingSpaceId();
        ResourceType resourceType = request.getResourceType();
        requireResourceExists(parkingSpaceId, resourceType);
        requireResourceAvailable(parkingSpaceId, resourceType, request.getRequestedDate());
        Long resolverId = resolveEmployeeId(adminLogin);
        request.approve(parkingSpaceId, resolverId, body.approvalNote(), clock.now());
        Request saved = requestRepository.saveAndFlush(request);
        RequestResponse response = RequestResponse.from(saved);
        eventPublisher.publishEvent(new RequestApprovedEvent(response));
        return response;
    }

    /**
     * Rechaza una solicitud {@code PENDING} o {@code APPROVED} con un codigo del catalogo; exige
     * texto libre (&ge;5 caracteres) cuando el codigo es {@code OTHER}.
     *
     * <p>Rechazar una solicitud {@code APPROVED} (auto-aprobada o aprobada por el ADMIN)
     * <strong>libera el recurso</strong>: al pasar a {@code REJECTED} la fila deja de cumplir el
     * filtro {@code status = 'APPROVED'} del indice unico {@code UX_requests_space_date_approved},
     * por lo que la plaza/puesto reaparece automaticamente en disponibilidad (change
     * {@code request-auto-assignment}, §D5). Los estados terminales
     * {@code REJECTED}/{@code CANCELLED} no admiten rechazo.</p>
     *
     * @param id         identificador de la solicitud
     * @param body       codigo del catalogo y texto libre opcional
     * @param adminLogin login del administrador que resuelve
     * @return la solicitud rechazada (DTO)
     * @throws EntityNotFoundException          si la solicitud no existe
     * @throws RequestStateException            si la solicitud no esta en {@code PENDING} ni {@code APPROVED}
     * @throws RejectionReasonRequiredException si falta el texto libre siendo {@code OTHER}
     */
    @Transactional
    public RequestResponse reject(Long id, RequestRejectRequest body, String adminLogin) {
        Request request = loadRequest(id);
        requireRejectable(request);
        RejectionReasonCode reasonCode = body.reasonCode();
        String reason = body.rejectionReason();
        requireReasonForOther(reasonCode, reason);
        Long resolverId = resolveEmployeeId(adminLogin);
        request.reject(reasonCode, reason, resolverId, clock.now());
        RequestResponse response = RequestResponse.from(requestRepository.save(request));
        eventPublisher.publishEvent(new RequestRejectedEvent(response));
        return response;
    }

    private void requireWithinWindow(LocalDate requestedDate, Instant now) {
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);
        LocalDate maxDate = today.plusDays(WINDOW_DAYS);
        if (requestedDate.isBefore(today) || requestedDate.isAfter(maxDate)) {
            throw new OutsideRequestWindowException(MSG_OUTSIDE_WINDOW);
        }
    }

    private void requireResourceAvailable(
            Long resourceId, ResourceType resourceType, LocalDate requestedDate) {
        // Regla unica de disponibilidad (issue #43): delega en AvailabilityService para no
        // divergir de la disponibilidad consolidada (liberaciones + reservas de visitante para
        // plazas) y evitar la doble reserva. Para DESK el termino de reserva de visitante no
        // aplica (no hay reservas de visitante en puestos).
        if (availabilityService.isSpaceTakenForDate(resourceId, resourceType, requestedDate)) {
            throw new SpaceUnavailableException(MSG_SPACE_UNAVAILABLE);
        }
    }

    private void requireReasonForOther(RejectionReasonCode reasonCode, String reason) {
        if (reasonCode == RejectionReasonCode.OTHER
                && (reason == null || reason.trim().length() < MIN_OTHER_REASON_LENGTH)) {
            throw new RejectionReasonRequiredException(MSG_REASON_REQUIRED);
        }
    }

    private void requirePending(Request request) {
        if (!request.isPending()) {
            throw new RequestStateException(MSG_NOT_PENDING);
        }
    }

    private void requireCancellable(Request request, LocalDate today) {
        if (!request.canBeCancelledBy(today)) {
            throw new RequestStateException(MSG_NOT_CANCELLABLE);
        }
    }

    private void requireRejectable(Request request) {
        if (!request.canBeRejected()) {
            throw new RequestStateException(MSG_NOT_REJECTABLE);
        }
    }

    private void requireNoPendingDuplicate(
            Long employeeId, ResourceType resourceType, LocalDate requestedDate) {
        if (requestRepository.existsByEmployeeIdAndResourceTypeAndRequestedDateAndStatus(
                employeeId, resourceType, requestedDate, RequestStatus.PENDING)) {
            throw new DuplicatePendingRequestException(MSG_ALREADY_PENDING);
        }
    }

    private void requireResourceExists(Long resourceId, ResourceType resourceType) {
        if (!resourceResolvers.exists(resourceId, resourceType)) {
            throw new EntityNotFoundException(MSG_RESOURCE_NOT_FOUND + resourceId);
        }
    }

    private Request loadRequest(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(MSG_REQUEST_NOT_FOUND + id));
    }

    private Long resolveEmployeeId(String login) {
        return employeeRepository.findByLogin(login)
                .map(Employee::getId)
                .orElseThrow(() -> new EntityNotFoundException(MSG_ACTOR_NOT_FOUND + login));
    }

    private Employee loadEmployee(String login) {
        return employeeRepository.findByLogin(login)
                .orElseThrow(() -> new EntityNotFoundException(MSG_ACTOR_NOT_FOUND + login));
    }
}
