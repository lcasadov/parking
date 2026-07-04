package com.aleatica.parking.visitor.application;

import com.aleatica.parking.auth.domain.ClockPort;
import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.employee.dto.PageResponse;
import com.aleatica.parking.fixedassignment.FixedAssignmentRepository;
import com.aleatica.parking.parkingspace.ParkingSpace;
import com.aleatica.parking.parkingspace.ParkingSpaceRepository;
import com.aleatica.parking.release.ReleaseRepository;
import com.aleatica.parking.request.RequestRepository;
import com.aleatica.parking.request.RequestStatus;
import com.aleatica.parking.visitor.VisitorRepository;
import com.aleatica.parking.visitor.VisitorReservation;
import com.aleatica.parking.visitor.VisitorReservationRepository;
import com.aleatica.parking.visitor.dto.VisitorReservationCreateRequest;
import com.aleatica.parking.visitor.dto.VisitorReservationResponse;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de reservas de visitante: creacion con control de disponibilidad
 * transaccional, listado paginado con filtros y anulacion de reservas futuras. Reservado
 * al {@code ADMIN}.
 *
 * <p>Arquitectura hexagonal: la logica de negocio no depende de la web y nunca devuelve
 * entidades JPA (convierte a DTO antes de salir, OWASP API3). La disponibilidad de la
 * plaza para la fecha se comprueba dentro de la transaccion; la unicidad plaza+fecha la
 * garantiza el indice unico de la BD ({@code UX_visitor_reservations_space_date}), de modo
 * que dos reservas concurrentes sobre la misma plaza/fecha derivan en un unico 201 y un
 * 409. La anulacion es un borrado fisico de la fila futura ({@code ClockPort}). La
 * auditoria se dispara {@code AFTER_COMMIT} via eventos.</p>
 *
 * <p><strong>Disponibilidad (consolidara availability-calendar B7):</strong> una plaza
 * esta NO disponible para (plaza, fecha) si (a) esta inactiva, (b) tiene una asignacion
 * fija activa para el dia de la semana de la fecha y esa fecha no esta liberada, (c) tiene
 * una solicitud APPROVED para esa fecha, o (d) tiene otra reserva de visitante para esa
 * fecha. La misma logica que aplica {@code RequestService#approve}, aqui ampliada con el
 * estado de la plaza, las liberaciones y las reservas de visitante.</p>
 */
@Service
public class VisitorReservationService {

    private static final String MSG_ACTOR_NOT_FOUND = "Usuario de sesion no encontrado: ";
    private static final String MSG_VISITOR_NOT_FOUND = "Visitante no encontrado: ";
    private static final String MSG_SPACE_NOT_FOUND = "Plaza no encontrada: ";
    private static final String MSG_RESERVATION_NOT_FOUND = "Reserva de visitante no encontrada: ";
    private static final String MSG_SPACE_UNAVAILABLE =
            "La plaza no esta disponible para la fecha de la reserva";
    private static final String MSG_PAST_CANCELLATION =
            "Solo se pueden anular reservas de visitante con fecha futura";

    private final VisitorReservationRepository reservationRepository;
    private final VisitorRepository visitorRepository;
    private final ParkingSpaceRepository parkingSpaceRepository;
    private final FixedAssignmentRepository fixedAssignmentRepository;
    private final RequestRepository requestRepository;
    private final ReleaseRepository releaseRepository;
    private final EmployeeRepository employeeRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ClockPort clock;

    /**
     * @param reservationRepository     repositorio de reservas de visitante
     * @param visitorRepository         repositorio de fichas de visitante (integridad)
     * @param parkingSpaceRepository    repositorio de plazas (estado + integridad)
     * @param fixedAssignmentRepository repositorio de asignaciones fijas (disponibilidad)
     * @param requestRepository         repositorio de solicitudes (disponibilidad APPROVED)
     * @param releaseRepository         repositorio de liberaciones (disponibilidad liberada)
     * @param employeeRepository        repositorio de empleados (resolucion del ADMIN)
     * @param eventPublisher            publicador de eventos de auditoria
     * @param clock                     reloj inyectable para la ventana de anulacion
     */
    public VisitorReservationService(
            VisitorReservationRepository reservationRepository,
            VisitorRepository visitorRepository,
            ParkingSpaceRepository parkingSpaceRepository,
            FixedAssignmentRepository fixedAssignmentRepository,
            RequestRepository requestRepository,
            ReleaseRepository releaseRepository,
            EmployeeRepository employeeRepository,
            ApplicationEventPublisher eventPublisher,
            ClockPort clock) {
        this.reservationRepository = reservationRepository;
        this.visitorRepository = visitorRepository;
        this.parkingSpaceRepository = parkingSpaceRepository;
        this.fixedAssignmentRepository = fixedAssignmentRepository;
        this.requestRepository = requestRepository;
        this.releaseRepository = releaseRepository;
        this.employeeRepository = employeeRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    /**
     * Crea una reserva de plaza para un visitante en una fecha, atribuida al {@code ADMIN}
     * de la sesion, tras comprobar transaccionalmente que la plaza esta disponible ese dia.
     *
     * @param adminLogin login del {@code ADMIN} creador (principal de la sesion)
     * @param request    visitante, plaza, fecha y anotaciones
     * @return la reserva creada (DTO)
     * @throws EntityNotFoundException                    si el visitante o la plaza no existen
     * @throws SpaceNotAvailableForReservationException   si la plaza no esta disponible esa fecha
     */
    @Transactional
    public VisitorReservationResponse create(String adminLogin, VisitorReservationCreateRequest request) {
        Long adminId = resolveEmployeeId(adminLogin);
        requireVisitorExists(request.visitorId());
        ParkingSpace space = loadSpace(request.parkingSpaceId());
        requireSpaceAvailable(space, request.reservationDate());
        VisitorReservation saved = reservationRepository.saveAndFlush(VisitorReservation.create(
                request.visitorId(), request.parkingSpaceId(), request.reservationDate(),
                request.notes(), adminId, clock.now()));
        VisitorReservationResponse response = VisitorReservationResponse.from(saved);
        eventPublisher.publishEvent(
                new VisitorReservationAuditEvent(VisitorReservationAuditEvent.Kind.CREATED, response));
        return response;
    }

    /**
     * Lista de forma paginada las reservas de visitante con filtros opcionales por fecha y
     * plaza.
     *
     * @param date           filtro por fecha; {@code null} para no filtrar
     * @param parkingSpaceId filtro por plaza; {@code null} para no filtrar
     * @param pageable       pagina y tamano solicitados
     * @return pagina de reservas (DTO)
     */
    @Transactional(readOnly = true)
    public PageResponse<VisitorReservationResponse> list(
            LocalDate date, Long parkingSpaceId, Pageable pageable) {
        return PageResponse.from(
                reservationRepository.search(date, parkingSpaceId, pageable),
                VisitorReservationResponse::from);
    }

    /**
     * Anula (borrado fisico) una reserva de visitante cuya fecha sea futura, liberando la
     * plaza ese dia.
     *
     * @param id identificador de la reserva
     * @throws EntityNotFoundException                        si la reserva no existe
     * @throws PastVisitorReservationCancellationException    si la reserva es de fecha pasada
     */
    @Transactional
    public void cancel(Long id) {
        VisitorReservation reservation = loadReservation(id);
        if (reservation.getReservationDate().isBefore(today())) {
            throw new PastVisitorReservationCancellationException(MSG_PAST_CANCELLATION);
        }
        VisitorReservationResponse snapshot = VisitorReservationResponse.from(reservation);
        reservationRepository.delete(reservation);
        eventPublisher.publishEvent(
                new VisitorReservationAuditEvent(VisitorReservationAuditEvent.Kind.CANCELLED, snapshot));
    }

    private void requireSpaceAvailable(ParkingSpace space, LocalDate date) {
        if (!space.isActive() || isSpaceTaken(space.getId(), date)) {
            throw new SpaceNotAvailableForReservationException(MSG_SPACE_UNAVAILABLE);
        }
    }

    private boolean isSpaceTaken(Long spaceId, LocalDate date) {
        return fixedAssignmentTaken(spaceId, date)
                || approvedRequestTaken(spaceId, date)
                || visitorReservationTaken(spaceId, date);
    }

    private boolean fixedAssignmentTaken(Long spaceId, LocalDate date) {
        int dayOfWeek = date.getDayOfWeek().getValue();
        boolean assigned =
                fixedAssignmentRepository.existsByParkingSpaceIdAndDayOfWeekAndActiveTrue(spaceId, dayOfWeek);
        return assigned && !releaseRepository.existsByParkingSpaceIdAndReleaseDate(spaceId, date);
    }

    private boolean approvedRequestTaken(Long spaceId, LocalDate date) {
        return requestRepository.existsByParkingSpaceIdAndRequestedDateAndStatus(
                spaceId, date, RequestStatus.APPROVED);
    }

    private boolean visitorReservationTaken(Long spaceId, LocalDate date) {
        return reservationRepository.existsByParkingSpaceIdAndReservationDate(spaceId, date);
    }

    private void requireVisitorExists(Long visitorId) {
        if (!visitorRepository.existsById(visitorId)) {
            throw new EntityNotFoundException(MSG_VISITOR_NOT_FOUND + visitorId);
        }
    }

    private ParkingSpace loadSpace(Long spaceId) {
        return parkingSpaceRepository.findById(spaceId)
                .orElseThrow(() -> new EntityNotFoundException(MSG_SPACE_NOT_FOUND + spaceId));
    }

    private VisitorReservation loadReservation(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(MSG_RESERVATION_NOT_FOUND + id));
    }

    private Long resolveEmployeeId(String login) {
        return employeeRepository.findByLogin(login)
                .map(Employee::getId)
                .orElseThrow(() -> new EntityNotFoundException(MSG_ACTOR_NOT_FOUND + login));
    }

    private LocalDate today() {
        return LocalDate.ofInstant(clock.now(), ZoneOffset.UTC);
    }
}
