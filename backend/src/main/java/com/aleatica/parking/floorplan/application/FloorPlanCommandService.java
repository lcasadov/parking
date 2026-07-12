package com.aleatica.parking.floorplan.application;

import com.aleatica.parking.auth.domain.ClockPort;
import com.aleatica.parking.availability.application.AvailabilityService;
import com.aleatica.parking.desk.Desk;
import com.aleatica.parking.desk.DeskRepository;
import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.floorplan.FloorPlanDeskState;
import com.aleatica.parking.floorplan.dto.DeskRequestResponse;
import com.aleatica.parking.notification.event.RequestCreatedEvent;
import com.aleatica.parking.request.infrastructure.RequestEntity;
import com.aleatica.parking.request.infrastructure.RequestJpaRepository;
import com.aleatica.parking.request.domain.RequestStatus;
import com.aleatica.parking.request.application.DuplicatePendingRequestException;
import com.aleatica.parking.request.application.OutsideRequestWindowException;
import com.aleatica.parking.request.application.SpaceUnavailableException;
import com.aleatica.parking.request.dto.RequestResponse;
import com.aleatica.parking.request.infrastructure.RequestMapper;
import com.aleatica.parking.resource.ResourceType;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de escritura del plano de puestos (capability {@code floor-plan}): solicitar
 * un puesto pinchandolo en el plano y reposicionar el marcador de un puesto (editor de
 * arrastre del admin).
 *
 * <p>Arquitectura hexagonal: la logica no depende de la web y nunca devuelve entidades JPA
 * (OWASP API3). La solicitud desde el plano reutiliza el ciclo de {@code requests}
 * (misma ventana hoy..+14, misma unicidad {@code PENDING} por empleado/tipo/fecha, misma
 * regla de disponibilidad consolidada de {@code AvailabilityService}, issue #43), pero fija
 * el puesto pinchado desde la creacion ({@code resource_id = deskId}) para que el puesto
 * quede ocupado: asi un segundo empleado que pinche el mismo puesto libre recibe 409 y el
 * plano lo pinta como {@code REQUESTED}. La unicidad puesto/fecha entre pendientes la
 * garantiza el indice unico filtrado {@code UX_requests_desk_date_pending} frente a
 * concurrencia; la comprobacion previa es la primera capa (design §Decisions: unicidad en
 * dos capas). Editar posiciones es exclusivo del {@code ADMIN} (RBAC en el controlador).</p>
 */
@Service
public class FloorPlanCommandService {

    /** Ventana de solicitud: hoy..hoy+14 dias naturales (extremos inclusive), como en requests. */
    private static final int WINDOW_DAYS = 14;
    private static final String MSG_ACTOR_NOT_FOUND = "Usuario de sesion no encontrado: ";
    private static final String MSG_DESK_NOT_FOUND = "Puesto no encontrado: ";
    private static final String MSG_OUTSIDE_WINDOW =
            "La fecha solicitada debe estar entre hoy y hoy+14 dias";
    private static final String MSG_ALREADY_PENDING =
            "Ya existe una solicitud pendiente para esa fecha";
    private static final String MSG_DESK_UNAVAILABLE =
            "El puesto no esta disponible para la fecha solicitada";

    private final DeskRepository deskRepository;
    private final RequestJpaRepository requestRepository;
    private final EmployeeRepository employeeRepository;
    private final AvailabilityService availabilityService;
    private final ApplicationEventPublisher eventPublisher;
    private final ClockPort clock;

    /**
     * @param deskRepository      repositorio de puestos (existencia/estado y coordenadas)
     * @param requestRepository   repositorio de solicitudes (alta puesto-especifica, unicidad)
     * @param employeeRepository  repositorio de empleados (resolucion de la sesion)
     * @param availabilityService servicio de disponibilidad consolidado (regla unica, issue #43)
     * @param eventPublisher      publicador de eventos de notificacion (solicitud creada)
     * @param clock               reloj inyectable (ventana hoy..+14d y marcas de tiempo)
     */
    public FloorPlanCommandService(
            DeskRepository deskRepository,
            RequestJpaRepository requestRepository,
            EmployeeRepository employeeRepository,
            AvailabilityService availabilityService,
            ApplicationEventPublisher eventPublisher,
            ClockPort clock) {
        this.deskRepository = deskRepository;
        this.requestRepository = requestRepository;
        this.employeeRepository = employeeRepository;
        this.availabilityService = availabilityService;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    /**
     * Solicita un puesto pinchado en el plano para una fecha, creando una solicitud
     * {@code PENDING} puesto-especifica solo si el puesto esta libre esa fecha.
     *
     * @param requesterLogin login del empleado solicitante (principal de la sesion)
     * @param deskId         identificador del puesto pinchado
     * @param date           fecha solicitada (debe estar en la ventana hoy..hoy+14)
     * @return el identificador de la solicitud creada y el nuevo estado del puesto
     * @throws OutsideRequestWindowException    si la fecha esta fuera de la ventana
     * @throws EntityNotFoundException          si el puesto no existe (o el login de sesion)
     * @throws DuplicatePendingRequestException si el empleado ya tiene un puesto pendiente esa fecha
     * @throws SpaceUnavailableException        si el puesto no esta libre para la fecha
     */
    @Transactional
    public DeskRequestResponse requestDesk(String requesterLogin, Long deskId, LocalDate date) {
        Instant now = clock.now();
        requireWithinWindow(date, now);
        Long employeeId = resolveEmployeeId(requesterLogin);
        Desk desk = deskRepository.findById(deskId)
                .orElseThrow(() -> new EntityNotFoundException(MSG_DESK_NOT_FOUND + deskId));
        requireDeskRequestable(desk, date);
        if (requestRepository.existsByEmployeeIdAndResourceTypeAndRequestedDateAndStatus(
                employeeId, ResourceType.DESK, date, RequestStatus.PENDING)) {
            throw new DuplicatePendingRequestException(MSG_ALREADY_PENDING);
        }
        RequestEntity saved = requestRepository.saveAndFlush(
                RequestEntity.createForResource(employeeId, ResourceType.DESK, desk.getId(), date, now));
        eventPublisher.publishEvent(
                new RequestCreatedEvent(RequestResponse.from(RequestMapper.toDomain(saved))));
        return new DeskRequestResponse(saved.getId(), desk.getId(), FloorPlanDeskState.MINE);
    }

    /**
     * Persiste las coordenadas relativas de un puesto (editor de arrastre del admin). El rango
     * 0-100 lo valida el DTO de entrada (400); aqui solo se comprueba la existencia del puesto.
     *
     * @param deskId identificador del puesto a reposicionar
     * @param coordX coordenada X (porcentaje 0-100, ya validada)
     * @param coordY coordenada Y (porcentaje 0-100, ya validada)
     * @throws EntityNotFoundException si el puesto no existe
     */
    @Transactional
    public void updatePosition(Long deskId, BigDecimal coordX, BigDecimal coordY) {
        Desk desk = deskRepository.findById(deskId)
                .orElseThrow(() -> new EntityNotFoundException(MSG_DESK_NOT_FOUND + deskId));
        desk.setCoordX(coordX);
        desk.setCoordY(coordY);
        deskRepository.save(desk);
    }

    private void requireDeskRequestable(Desk desk, LocalDate date) {
        boolean unavailable = !desk.isActive()
                || availabilityService.isSpaceTakenForDate(desk.getId(), ResourceType.DESK, date)
                || requestRepository.existsByResourceIdAndResourceTypeAndRequestedDateAndStatus(
                        desk.getId(), ResourceType.DESK, date, RequestStatus.PENDING);
        if (unavailable) {
            throw new SpaceUnavailableException(MSG_DESK_UNAVAILABLE);
        }
    }

    private void requireWithinWindow(LocalDate date, Instant now) {
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);
        LocalDate maxDate = today.plusDays(WINDOW_DAYS);
        if (date.isBefore(today) || date.isAfter(maxDate)) {
            throw new OutsideRequestWindowException(MSG_OUTSIDE_WINDOW);
        }
    }

    private Long resolveEmployeeId(String login) {
        return employeeRepository.findByLogin(login)
                .map(Employee::getId)
                .orElseThrow(() -> new EntityNotFoundException(MSG_ACTOR_NOT_FOUND + login));
    }
}
