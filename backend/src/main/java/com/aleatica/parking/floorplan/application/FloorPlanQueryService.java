package com.aleatica.parking.floorplan.application;

import com.aleatica.parking.auth.domain.ClockPort;
import com.aleatica.parking.desk.Desk;
import com.aleatica.parking.desk.DeskRepository;
import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.fixedassignment.infrastructure.FixedAssignmentEntity;
import com.aleatica.parking.fixedassignment.infrastructure.FixedAssignmentJpaRepository;
import com.aleatica.parking.floorplan.FloorPlanDeskState;
import com.aleatica.parking.floorplan.dto.FloorPlanDeskResponse;
import com.aleatica.parking.floorplan.dto.FloorPlanResponse;
import com.aleatica.parking.release.infrastructure.ReleaseEntity;
import com.aleatica.parking.release.infrastructure.ReleaseJpaRepository;
import com.aleatica.parking.request.infrastructure.RequestEntity;
import com.aleatica.parking.request.infrastructure.RequestJpaRepository;
import com.aleatica.parking.request.domain.RequestStatus;
import com.aleatica.parking.request.application.OutsideRequestWindowException;
import com.aleatica.parking.resource.ResourceType;
import com.aleatica.parking.visitor.VisitorReservation;
import com.aleatica.parking.visitor.VisitorReservationRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso de lectura del plano de puestos: proyecta, para una fecha, la posicion y el
 * estado de cada puesto activo, coloreado segun su disponibilidad relativa al empleado que
 * consulta (capability {@code floor-plan}).
 *
 * <p>Arquitectura hexagonal: la logica no depende de la web y nunca devuelve entidades JPA
 * (convierte a DTO antes de salir, OWASP API3). <strong>Sin N+1</strong>: carga por rango en
 * una unica consulta por entidad (puestos activos, asignaciones fijas de esos puestos,
 * liberaciones, solicitudes aprobadas y pendientes de la fecha) y ensambla en memoria; nunca
 * una consulta por puesto. <strong>Privacidad</strong>: el estado {@link FloorPlanDeskState#MINE}
 * solo se atribuye a los recursos del propio solicitante; los de terceros se devuelven como
 * {@code ASSIGNED}/{@code REQUESTED} sin identidad (RGPD / minimizacion,
 * {@code docs/security-design.md}). La ventana hoy..hoy+14 la valida el reloj inyectable
 * ({@code ClockPort}), coherente con {@code requests}.</p>
 */
@Service
public class FloorPlanQueryService {

    private static final String MSG_ACTOR_NOT_FOUND = "Usuario de sesion no encontrado: ";
    private static final String MSG_OUTSIDE_WINDOW =
            "La fecha consultada no puede ser anterior a hoy";

    private final DeskRepository deskRepository;
    private final FixedAssignmentJpaRepository fixedAssignmentRepository;
    private final ReleaseJpaRepository releaseRepository;
    private final RequestJpaRepository requestRepository;
    private final EmployeeRepository employeeRepository;
    private final VisitorReservationRepository visitorReservationRepository;
    private final ClockPort clock;

    /**
     * @param deskRepository               repositorio de puestos (recursos DESK activos)
     * @param fixedAssignmentRepository    repositorio de asignaciones fijas (titular por dia)
     * @param releaseRepository            repositorio de liberaciones (libera el puesto una fecha)
     * @param requestRepository            repositorio de solicitudes (aprobadas/pendientes por fecha)
     * @param employeeRepository           repositorio de empleados (resolucion de la sesion)
     * @param visitorReservationRepository repositorio de reservas de visitante (ocupa el puesto)
     * @param clock                        reloj inyectable (ventana hoy..+14d)
     */
    public FloorPlanQueryService(
            DeskRepository deskRepository,
            FixedAssignmentJpaRepository fixedAssignmentRepository,
            ReleaseJpaRepository releaseRepository,
            RequestJpaRepository requestRepository,
            EmployeeRepository employeeRepository,
            VisitorReservationRepository visitorReservationRepository,
            ClockPort clock) {
        this.deskRepository = deskRepository;
        this.fixedAssignmentRepository = fixedAssignmentRepository;
        this.releaseRepository = releaseRepository;
        this.requestRepository = requestRepository;
        this.employeeRepository = employeeRepository;
        this.visitorReservationRepository = visitorReservationRepository;
        this.clock = clock;
    }

    /**
     * Devuelve el plano de puestos para una fecha: cada puesto activo con su posicion y su
     * estado relativo al solicitante.
     *
     * @param requesterLogin login del solicitante (principal de la sesion)
     * @param date           fecha a consultar (debe estar en la ventana hoy..hoy+14)
     * @return el plano de la fecha (lista de puestos con estado, posiblemente vacia)
     * @throws OutsideRequestWindowException si la fecha esta fuera de la ventana
     * @throws EntityNotFoundException       si el login de sesion no corresponde a un empleado
     */
    @Transactional(readOnly = true)
    public FloorPlanResponse floorPlanForDate(String requesterLogin, LocalDate date) {
        requireWithinWindow(date);
        Long requesterId = resolveEmployeeId(requesterLogin);
        int dow = date.getDayOfWeek().getValue();

        List<Desk> desks = deskRepository.findByActiveTrueOrderByNumberAsc();
        List<Long> deskIds = desks.stream().map(Desk::getId).toList();

        Map<Long, Long> approvedByDesk = holdersByResource(RequestStatus.APPROVED, date);
        Map<Long, Long> pendingByDesk = holdersByResource(RequestStatus.PENDING, date);
        Map<Long, Long> fixedByDesk = fixedHoldersForDay(deskIds, dow);
        Set<Long> releasedDesks = releaseRepository
                .findByResourceTypeAndReleaseDateBetween(ResourceType.DESK, date, date).stream()
                .map(ReleaseEntity::getResourceId)
                .collect(Collectors.toSet());
        // Puestos ocupados por una reserva de visitante esa fecha: aparecen ocupados
        // (tercero, sin identidad) en el plano, igual que una asignación de otro.
        Set<Long> visitorReservedDesks = visitorReservationRepository
                .findByResourceTypeAndReservationDateBetween(ResourceType.DESK, date, date).stream()
                .map(VisitorReservation::getResourceId)
                .collect(Collectors.toSet());

        List<FloorPlanDeskResponse> items = desks.stream()
                .map(desk -> toResponse(desk, deskState(
                        desk.getId(), requesterId, approvedByDesk, fixedByDesk, releasedDesks,
                        pendingByDesk, visitorReservedDesks)))
                .toList();
        return new FloorPlanResponse(date, items);
    }

    private FloorPlanDeskResponse toResponse(Desk desk, FloorPlanDeskState state) {
        return new FloorPlanDeskResponse(
                desk.getId(), desk.getNumber(), desk.getCategory(),
                desk.getCoordX(), desk.getCoordY(), state);
    }

    private FloorPlanDeskState deskState(
            Long deskId, Long requesterId, Map<Long, Long> approvedByDesk,
            Map<Long, Long> fixedByDesk, Set<Long> releasedDesks, Map<Long, Long> pendingByDesk,
            Set<Long> visitorReservedDesks) {
        Long approvedHolder = approvedByDesk.get(deskId);
        if (approvedHolder != null) {
            return mineOr(approvedHolder, requesterId, FloorPlanDeskState.ASSIGNED);
        }
        Long fixedHolder = fixedByDesk.get(deskId);
        if (fixedHolder != null) {
            if (releasedDesks.contains(deskId)) {
                return FloorPlanDeskState.RELEASED;
            }
            return mineOr(fixedHolder, requesterId, FloorPlanDeskState.ASSIGNED);
        }
        // Reserva de visitante: ocupa el puesto (tercero); nunca es MINE ni liberable aquí.
        if (visitorReservedDesks.contains(deskId)) {
            return FloorPlanDeskState.ASSIGNED;
        }
        Long pendingHolder = pendingByDesk.get(deskId);
        if (pendingHolder != null) {
            return mineOr(pendingHolder, requesterId, FloorPlanDeskState.REQUESTED);
        }
        return FloorPlanDeskState.FREE;
    }

    private static FloorPlanDeskState mineOr(
            Long holderId, Long requesterId, FloorPlanDeskState otherwise) {
        return holderId.equals(requesterId) ? FloorPlanDeskState.MINE : otherwise;
    }

    private Map<Long, Long> holdersByResource(RequestStatus status, LocalDate date) {
        return requestRepository
                .findByStatusAndResourceTypeAndRequestedDateBetween(status, ResourceType.DESK, date, date)
                .stream()
                .filter(request -> request.getResourceId() != null)
                .collect(Collectors.toMap(
                        RequestEntity::getResourceId, RequestEntity::getEmployeeId, (a, b) -> a));
    }

    private Map<Long, Long> fixedHoldersForDay(List<Long> deskIds, int dow) {
        if (deskIds.isEmpty()) {
            return Map.of();
        }
        return fixedAssignmentRepository
                .findByResourceIdInAndResourceTypeAndActiveTrue(deskIds, ResourceType.DESK).stream()
                .filter(fa -> fa.getDayOfWeek() == dow)
                .collect(Collectors.toMap(
                        FixedAssignmentEntity::getResourceId, FixedAssignmentEntity::getEmployeeId, (a, b) -> a));
    }

    private void requireWithinWindow(LocalDate date) {
        LocalDate today = LocalDate.ofInstant(clock.now(), ZoneOffset.UTC);
        if (date.isBefore(today)) {
            throw new OutsideRequestWindowException(MSG_OUTSIDE_WINDOW);
        }
    }

    private Long resolveEmployeeId(String login) {
        return employeeRepository.findByLogin(login)
                .map(Employee::getId)
                .orElseThrow(() -> new EntityNotFoundException(MSG_ACTOR_NOT_FOUND + login));
    }
}
