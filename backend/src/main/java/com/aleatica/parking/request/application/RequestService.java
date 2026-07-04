package com.aleatica.parking.request.application;

import com.aleatica.parking.auth.domain.ClockPort;
import com.aleatica.parking.availability.application.AvailabilityService;
import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.employee.dto.PageResponse;
import com.aleatica.parking.parkingspace.ParkingSpaceRepository;
import com.aleatica.parking.request.RejectionReasonCode;
import com.aleatica.parking.request.Request;
import com.aleatica.parking.request.RequestRepository;
import com.aleatica.parking.request.RequestStatus;
import com.aleatica.parking.request.dto.RequestApproveRequest;
import com.aleatica.parking.request.dto.RequestCreateRequest;
import com.aleatica.parking.request.dto.RequestRejectRequest;
import com.aleatica.parking.request.dto.RequestResponse;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.context.ApplicationEventPublisher;
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
 * RBAC del controlador. Las notificaciones se disparan {@code AFTER_COMMIT} via eventos
 * ({@link RequestNotificationEvent}).</p>
 */
@Service
public class RequestService {

    /** Ventana de solicitud: hoy..hoy+14 dias naturales (extremos inclusive). */
    private static final int WINDOW_DAYS = 14;
    /** Longitud minima del texto libre cuando el motivo de rechazo es {@code OTHER}. */
    private static final int MIN_OTHER_REASON_LENGTH = 5;

    private static final String MSG_ACTOR_NOT_FOUND = "Usuario de sesion no encontrado: ";
    private static final String MSG_REQUEST_NOT_FOUND = "Solicitud no encontrada: ";
    private static final String MSG_SPACE_NOT_FOUND = "Plaza no encontrada: ";
    private static final String MSG_OUTSIDE_WINDOW =
            "La fecha solicitada debe estar entre hoy y hoy+14 dias";
    private static final String MSG_ALREADY_PENDING =
            "Ya existe una solicitud pendiente para esa fecha";
    private static final String MSG_NOT_OWNER = "No puede operar sobre la solicitud de otro empleado";
    private static final String MSG_NOT_PENDING =
            "La solicitud no esta pendiente; no admite esta transicion";
    private static final String MSG_SPACE_UNAVAILABLE =
            "La plaza no esta disponible para la fecha solicitada";
    private static final String MSG_REASON_REQUIRED =
            "El motivo libre es obligatorio (>=5 caracteres) cuando el codigo es OTHER";

    private final RequestRepository requestRepository;
    private final EmployeeRepository employeeRepository;
    private final ParkingSpaceRepository parkingSpaceRepository;
    private final AvailabilityService availabilityService;
    private final ApplicationEventPublisher eventPublisher;
    private final ClockPort clock;

    /**
     * @param requestRepository      repositorio de solicitudes
     * @param employeeRepository     repositorio de empleados (solicitante/resolutor)
     * @param parkingSpaceRepository repositorio de plazas (integridad al aprobar)
     * @param availabilityService    servicio de disponibilidad consolidado (regla unica al aprobar)
     * @param eventPublisher         publicador de eventos de notificacion
     * @param clock                  reloj inyectable para ventana y marcas de tiempo
     */
    public RequestService(
            RequestRepository requestRepository,
            EmployeeRepository employeeRepository,
            ParkingSpaceRepository parkingSpaceRepository,
            AvailabilityService availabilityService,
            ApplicationEventPublisher eventPublisher,
            ClockPort clock) {
        this.requestRepository = requestRepository;
        this.employeeRepository = employeeRepository;
        this.parkingSpaceRepository = parkingSpaceRepository;
        this.availabilityService = availabilityService;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    /**
     * Crea una solicitud en estado {@code PENDING} (sin plaza) para el empleado de la
     * sesion y una fecha dentro de la ventana hoy..hoy+14, garantizando una unica
     * solicitud {@code PENDING} por empleado y fecha.
     *
     * @param requesterLogin login del empleado solicitante (principal de la sesion)
     * @param request        fecha solicitada
     * @return la solicitud creada (DTO)
     * @throws OutsideRequestWindowException    si la fecha esta fuera de la ventana
     * @throws DuplicatePendingRequestException si ya hay una solicitud pendiente esa fecha
     */
    @Transactional
    public RequestResponse create(String requesterLogin, RequestCreateRequest request) {
        Long employeeId = resolveEmployeeId(requesterLogin);
        Instant now = clock.now();
        LocalDate requestedDate = request.requestedDate();
        requireWithinWindow(requestedDate, now);
        if (requestRepository.existsByEmployeeIdAndRequestedDateAndStatus(
                employeeId, requestedDate, RequestStatus.PENDING)) {
            throw new DuplicatePendingRequestException(MSG_ALREADY_PENDING);
        }
        Request saved = requestRepository.saveAndFlush(
                Request.create(employeeId, requestedDate, now));
        RequestResponse response = RequestResponse.from(saved);
        publish(RequestNotificationEvent.Kind.CREATED, response);
        return response;
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
        var page = status == null
                ? requestRepository.findByEmployeeId(employeeId, pageable)
                : requestRepository.findByEmployeeIdAndStatus(employeeId, status, pageable);
        return PageResponse.from(page, RequestResponse::from);
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
     * Cancela la solicitud propia del empleado, solo mientras esta en {@code PENDING}.
     *
     * @param id             identificador de la solicitud
     * @param requesterLogin login del empleado (principal de la sesion)
     * @return la solicitud cancelada (DTO)
     * @throws EntityNotFoundException si no existe
     * @throws AccessDeniedException   si el solicitante no es el propietario (BOLA)
     * @throws RequestStateException   si la solicitud no esta en {@code PENDING}
     */
    @Transactional
    public RequestResponse cancel(Long id, String requesterLogin) {
        Request request = loadRequest(id);
        Long employeeId = resolveEmployeeId(requesterLogin);
        if (!request.getEmployeeId().equals(employeeId)) {
            throw new AccessDeniedException(MSG_NOT_OWNER);
        }
        requirePending(request);
        request.cancel();
        return RequestResponse.from(requestRepository.save(request));
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
        requireSpaceExists(parkingSpaceId);
        requireSpaceAvailable(parkingSpaceId, request.getRequestedDate());
        Long resolverId = resolveEmployeeId(adminLogin);
        request.approve(parkingSpaceId, resolverId, body.approvalNote(), clock.now());
        Request saved = requestRepository.saveAndFlush(request);
        RequestResponse response = RequestResponse.from(saved);
        publish(RequestNotificationEvent.Kind.APPROVED, response);
        return response;
    }

    /**
     * Rechaza una solicitud {@code PENDING} con un codigo del catalogo; exige texto
     * libre (&ge;5 caracteres) cuando el codigo es {@code OTHER}.
     *
     * @param id         identificador de la solicitud
     * @param body       codigo del catalogo y texto libre opcional
     * @param adminLogin login del administrador que resuelve
     * @return la solicitud rechazada (DTO)
     * @throws EntityNotFoundException          si la solicitud no existe
     * @throws RequestStateException            si la solicitud no esta en {@code PENDING}
     * @throws RejectionReasonRequiredException si falta el texto libre siendo {@code OTHER}
     */
    @Transactional
    public RequestResponse reject(Long id, RequestRejectRequest body, String adminLogin) {
        Request request = loadRequest(id);
        requirePending(request);
        RejectionReasonCode reasonCode = body.reasonCode();
        String reason = body.rejectionReason();
        requireReasonForOther(reasonCode, reason);
        Long resolverId = resolveEmployeeId(adminLogin);
        request.reject(reasonCode, reason, resolverId, clock.now());
        RequestResponse response = RequestResponse.from(requestRepository.save(request));
        publish(RequestNotificationEvent.Kind.REJECTED, response);
        return response;
    }

    private void requireWithinWindow(LocalDate requestedDate, Instant now) {
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);
        LocalDate maxDate = today.plusDays(WINDOW_DAYS);
        if (requestedDate.isBefore(today) || requestedDate.isAfter(maxDate)) {
            throw new OutsideRequestWindowException(MSG_OUTSIDE_WINDOW);
        }
    }

    private void requireSpaceAvailable(Long parkingSpaceId, LocalDate requestedDate) {
        // Regla unica de disponibilidad (issue #43): delega en AvailabilityService para no
        // divergir de la disponibilidad consolidada (liberaciones + reservas de visitante) y
        // evitar la doble reserva de una plaza ya ocupada por un visitante.
        if (availabilityService.isSpaceTakenForDate(parkingSpaceId, requestedDate)) {
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

    private void requireSpaceExists(Long parkingSpaceId) {
        if (!parkingSpaceRepository.existsById(parkingSpaceId)) {
            throw new EntityNotFoundException(MSG_SPACE_NOT_FOUND + parkingSpaceId);
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

    private void publish(RequestNotificationEvent.Kind kind, RequestResponse response) {
        eventPublisher.publishEvent(new RequestNotificationEvent(kind, response));
    }
}
