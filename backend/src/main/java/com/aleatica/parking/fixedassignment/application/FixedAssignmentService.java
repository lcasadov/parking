package com.aleatica.parking.fixedassignment.application;

import com.aleatica.parking.auth.domain.ClockPort;
import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.employee.dto.PageResponse;
import com.aleatica.parking.fixedassignment.FixedAssignment;
import com.aleatica.parking.fixedassignment.FixedAssignmentRepository;
import com.aleatica.parking.fixedassignment.dto.FixedAssignmentPutRequest;
import com.aleatica.parking.fixedassignment.dto.FixedAssignmentResponse;
import com.aleatica.parking.notification.event.FixedAssignmentRevokedEvent;
import com.aleatica.parking.parkingspace.ParkingSpaceRepository;
import com.aleatica.parking.resource.ResourceType;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de asignaciones fijas (establecer, revocar, consultar y listar),
 * reservados en su mayoria al rol {@code ADMIN}.
 *
 * <p>Arquitectura hexagonal: la logica de negocio no depende de la web y nunca
 * devuelve entidades JPA (convierte a DTO antes de salir, OWASP API3). La unicidad
 * plaza/dia y empleado/dia (entre filas activas) la garantizan los indices unicos
 * filtrados de la BD; el servicio no hace <em>check-then-insert</em> en memoria, sino
 * que deja que la violacion de indice se traduzca a 409 (design §Decisions). La
 * verificacion de pertenencia (BOLA) vive aqui, no solo en el RBAC del controlador.</p>
 */
@Service
public class FixedAssignmentService {

    private static final int DAY_MIN = 1;
    private static final int DAY_MAX = 7;

    private static final String MSG_INVALID_DAY =
            "Los dias de la semana deben estar entre 1 (Lunes) y 7 (Domingo)";
    private static final String MSG_FORBIDDEN_OTHER =
            "No puede consultar las asignaciones fijas de otro empleado";
    private static final String MSG_EMPLOYEE_NOT_FOUND = "Empleado no encontrado: ";
    private static final String MSG_SPACE_NOT_FOUND = "Plaza no encontrada: ";
    private static final String MSG_ACTOR_NOT_FOUND = "Usuario de sesion no encontrado: ";
    private static final String MSG_NO_ACTIVE =
            "El empleado no tiene ninguna asignacion fija activa que revocar: ";

    private final FixedAssignmentRepository fixedAssignmentRepository;
    private final EmployeeRepository employeeRepository;
    private final ParkingSpaceRepository parkingSpaceRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ClockPort clock;

    /**
     * @param fixedAssignmentRepository repositorio de asignaciones fijas
     * @param employeeRepository        repositorio de empleados (titular/actor)
     * @param parkingSpaceRepository    repositorio de plazas (integridad referencial)
     * @param eventPublisher            publicador de eventos de notificacion (revocacion)
     * @param clock                     reloj inyectable para {@code created_at}/{@code revoked_at}
     */
    public FixedAssignmentService(
            FixedAssignmentRepository fixedAssignmentRepository,
            EmployeeRepository employeeRepository,
            ParkingSpaceRepository parkingSpaceRepository,
            ApplicationEventPublisher eventPublisher,
            ClockPort clock) {
        this.fixedAssignmentRepository = fixedAssignmentRepository;
        this.employeeRepository = employeeRepository;
        this.parkingSpaceRepository = parkingSpaceRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    /**
     * Lista de forma paginada todas las asignaciones fijas activas (solo {@code ADMIN}).
     *
     * @param pageable pagina y orden solicitados
     * @return pagina de asignaciones activas (DTO) con sus metadatos
     */
    @Transactional(readOnly = true)
    public PageResponse<FixedAssignmentResponse> list(Pageable pageable) {
        return PageResponse.from(
                fixedAssignmentRepository.findByActiveTrue(pageable), FixedAssignmentResponse::from);
    }

    /**
     * Devuelve las asignaciones fijas activas de un empleado aplicando la verificacion
     * de pertenencia (BOLA): un {@code EMPLOYEE} solo puede consultar las propias.
     *
     * @param employeeId     empleado consultado
     * @param requesterLogin login del solicitante (principal de la sesion)
     * @param isAdmin        si el solicitante tiene rol {@code ADMIN} (sin restriccion)
     * @return lista de asignaciones activas del empleado (posiblemente vacia)
     * @throws AccessDeniedException si un {@code EMPLOYEE} consulta las de otro empleado
     */
    @Transactional(readOnly = true)
    public List<FixedAssignmentResponse> getEmployeeAssignments(
            Long employeeId, String requesterLogin, boolean isAdmin) {
        if (!isAdmin && !resolveEmployeeId(requesterLogin).equals(employeeId)) {
            throw new AccessDeniedException(MSG_FORBIDDEN_OTHER);
        }
        return activeAssignments(employeeId);
    }

    /**
     * Establece, para un empleado y una plaza, el conjunto de dias de la semana
     * asignados: crea los dias nuevos y revoca logicamente los retirados respecto al
     * estado previo (idempotente cuando el conjunto no cambia).
     *
     * <p>La unicidad plaza/dia y empleado/dia entre filas activas la impone la BD; un
     * conflicto (misma plaza ya asignada ese dia, o el empleado ya con un recurso ese
     * dia en otra plaza) provoca una violacion de indice traducida a 409.</p>
     *
     * @param employeeId  empleado titular
     * @param request     plaza y dias de la semana solicitados
     * @param actorLogin  login del {@code ADMIN} que opera (para {@code created_by})
     * @return las asignaciones activas resultantes del empleado
     * @throws InvalidDayOfWeekException si algun dia esta fuera del rango 1-7
     * @throws EntityNotFoundException   si el empleado o la plaza no existen
     */
    @Transactional
    public List<FixedAssignmentResponse> setAssignments(
            Long employeeId, FixedAssignmentPutRequest request, String actorLogin) {
        List<Integer> days = validateDays(request.daysOfWeek());
        requireEmployeeExists(employeeId);
        requireSpaceExists(request.parkingSpaceId());
        Long actorId = resolveEmployeeId(actorLogin);
        applyDaySet(employeeId, request.parkingSpaceId(), days, actorId, clock.now());
        return activeAssignments(employeeId);
    }

    /**
     * Revoca logicamente todas las asignaciones fijas activas de un empleado
     * ({@code active=false} + {@code revoked_at}/{@code revoked_by_id}), sin borrar la
     * fila ni afectar a dias pasados ni a solicitudes ya aprobadas.
     *
     * @param employeeId empleado cuyas asignaciones se revocan
     * @param actorLogin login del {@code ADMIN} que revoca (para {@code revoked_by})
     * @throws EntityNotFoundException si el empleado no tiene asignacion activa alguna
     */
    @Transactional
    public void revoke(Long employeeId, String actorLogin) {
        List<FixedAssignment> active =
                fixedAssignmentRepository.findByEmployeeIdAndActiveTrueOrderByDayOfWeekAsc(employeeId);
        if (active.isEmpty()) {
            throw new EntityNotFoundException(MSG_NO_ACTIVE + employeeId);
        }
        Long actorId = resolveEmployeeId(actorLogin);
        Instant now = clock.now();
        active.forEach(assignment -> assignment.revoke(actorId, now));
        fixedAssignmentRepository.saveAll(active);
        // Notifica AFTER_COMMIT al empleado afectado (capability notifications): un unico
        // email por revocacion, no uno por dia/fila. Un fallo del envio no revierte la
        // revocacion (el listener se engancha tras el commit).
        eventPublisher.publishEvent(new FixedAssignmentRevokedEvent(employeeId));
    }

    private void applyDaySet(
            Long employeeId, Long spaceId, List<Integer> targetDays, Long actorId, Instant now) {
        List<FixedAssignment> current =
                fixedAssignmentRepository.findByEmployeeIdAndResourceIdAndResourceTypeAndActiveTrue(
                        employeeId, spaceId, ResourceType.PARKING);
        Set<Integer> currentDays = new HashSet<>();
        for (FixedAssignment assignment : current) {
            currentDays.add(assignment.getDayOfWeek());
            if (!targetDays.contains(assignment.getDayOfWeek())) {
                assignment.revoke(actorId, now);
            }
        }
        fixedAssignmentRepository.saveAll(current);
        // Aplica las revocaciones antes de insertar para no chocar con el indice
        // filtrado empleado/dia al reasignar un dia dentro de la misma plaza.
        fixedAssignmentRepository.flush();

        List<FixedAssignment> toCreate = new ArrayList<>();
        for (Integer day : targetDays) {
            if (!currentDays.contains(day)) {
                toCreate.add(FixedAssignment.create(spaceId, employeeId, day, actorId, now));
            }
        }
        fixedAssignmentRepository.saveAll(toCreate);
        // Fuerza la violacion del indice unico filtrado (si la hay) dentro de la
        // transaccion, para que se traduzca a 409 y haga rollback atomico.
        fixedAssignmentRepository.flush();
    }

    private List<Integer> validateDays(List<Integer> days) {
        for (Integer day : days) {
            if (day == null || day < DAY_MIN || day > DAY_MAX) {
                throw new InvalidDayOfWeekException(MSG_INVALID_DAY);
            }
        }
        return days;
    }

    private List<FixedAssignmentResponse> activeAssignments(Long employeeId) {
        return fixedAssignmentRepository
                .findByEmployeeIdAndActiveTrueOrderByDayOfWeekAsc(employeeId).stream()
                .map(FixedAssignmentResponse::from)
                .toList();
    }

    private Long resolveEmployeeId(String login) {
        return employeeRepository.findByLogin(login)
                .map(Employee::getId)
                .orElseThrow(() -> new EntityNotFoundException(MSG_ACTOR_NOT_FOUND + login));
    }

    private void requireEmployeeExists(Long employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new EntityNotFoundException(MSG_EMPLOYEE_NOT_FOUND + employeeId);
        }
    }

    private void requireSpaceExists(Long parkingSpaceId) {
        if (!parkingSpaceRepository.existsById(parkingSpaceId)) {
            throw new EntityNotFoundException(MSG_SPACE_NOT_FOUND + parkingSpaceId);
        }
    }
}
