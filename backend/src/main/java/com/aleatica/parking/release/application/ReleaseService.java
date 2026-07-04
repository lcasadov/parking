package com.aleatica.parking.release.application;

import com.aleatica.parking.auth.domain.ClockPort;
import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.employee.dto.PageResponse;
import com.aleatica.parking.fixedassignment.FixedAssignment;
import com.aleatica.parking.fixedassignment.FixedAssignmentRepository;
import com.aleatica.parking.release.Release;
import com.aleatica.parking.release.ReleaseRepository;
import com.aleatica.parking.resource.ResourceResolvers;
import com.aleatica.parking.resource.ResourceType;
import com.aleatica.parking.release.dto.AdministrativeReleaseRequest;
import com.aleatica.parking.release.dto.ReleaseCreateRequest;
import com.aleatica.parking.release.dto.ReleaseResponse;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de liberaciones de recurso: creacion voluntaria (por el titular) y
 * administrativa (por el {@code ADMIN}), listado propio paginado y cancelacion de una
 * liberacion futura propia.
 *
 * <p>Arquitectura hexagonal: la logica de negocio no depende de la web y nunca devuelve
 * entidades JPA (convierte a DTO antes de salir, OWASP API3). La ventana temporal
 * ({@code releaseDate >= hoy}) se evalua con {@code ClockPort}. La unicidad recurso+fecha
 * la garantiza el indice unico de la BD; el servicio hace una comprobacion previa
 * (mensaje claro) pero deja que la violacion de indice se traduzca a 409 bajo
 * concurrencia (design §Decisions). La verificacion de pertenencia (BOLA) vive aqui, no
 * solo en el RBAC del controlador. La auditoria se dispara {@code AFTER_COMMIT} via
 * eventos ({@link ReleaseAuditEvent}). La cancelacion es un borrado fisico de la fila
 * futura (design §Decisions: {@code releases} es historico purgable).</p>
 */
@Service
public class ReleaseService {

    private static final String MSG_ACTOR_NOT_FOUND = "Usuario de sesion no encontrado: ";
    private static final String MSG_EMPLOYEE_NOT_FOUND = "Empleado no encontrado: ";
    private static final String MSG_SPACE_NOT_FOUND = "Recurso no encontrado: ";
    private static final String MSG_RELEASE_NOT_FOUND = "Liberacion no encontrada: ";
    private static final String MSG_DATE_IN_PAST =
            "La fecha de liberacion no puede ser anterior a hoy";
    private static final String MSG_NO_FIXED_ASSIGNMENT =
            "No hay una asignacion fija activa del recurso para ese dia de la semana";
    private static final String MSG_AMBIGUOUS_ASSIGNMENT =
            "El empleado tiene varias asignaciones fijas ese dia; indique el recurso a liberar";
    private static final String MSG_ALREADY_RELEASED =
            "El recurso ya esta liberado para esa fecha";
    private static final String MSG_NOT_OWNER =
            "No puede operar sobre la liberacion de otro empleado";
    private static final String MSG_PAST_CANCELLATION =
            "No se pueden anular liberaciones de fechas pasadas";

    private final ReleaseRepository releaseRepository;
    private final EmployeeRepository employeeRepository;
    private final ResourceResolvers resourceResolvers;
    private final FixedAssignmentRepository fixedAssignmentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ClockPort clock;

    /**
     * @param releaseRepository         repositorio de liberaciones
     * @param employeeRepository        repositorio de empleados (titular/ejecutor)
     * @param resourceResolvers         resolutor polimorfico de recursos (integridad de plaza
     *                                  o puesto segun {@code resourceType})
     * @param fixedAssignmentRepository repositorio de asignaciones fijas (resolucion de recurso)
     * @param eventPublisher            publicador de eventos de auditoria
     * @param clock                     reloj inyectable para la ventana y las marcas de tiempo
     */
    public ReleaseService(
            ReleaseRepository releaseRepository,
            EmployeeRepository employeeRepository,
            ResourceResolvers resourceResolvers,
            FixedAssignmentRepository fixedAssignmentRepository,
            ApplicationEventPublisher eventPublisher,
            ClockPort clock) {
        this.releaseRepository = releaseRepository;
        this.employeeRepository = employeeRepository;
        this.resourceResolvers = resourceResolvers;
        this.fixedAssignmentRepository = fixedAssignmentRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    /**
     * Crea una liberacion voluntaria del recurso fijo del empleado de la sesion para una
     * fecha presente o futura. Si {@code parkingSpaceId} se omite, resuelve la plaza fija
     * del empleado para el dia de la semana de {@code releaseDate}.
     *
     * @param requesterLogin login del empleado titular (principal de la sesion)
     * @param request        fecha a liberar y, opcionalmente, el recurso
     * @return la liberacion creada (DTO)
     * @throws ReleaseDateInPastException       si la fecha es anterior a hoy
     * @throws NoFixedAssignmentException       si no hay asignacion fija activa ese dia
     * @throws ResourceAlreadyReleasedException si el recurso ya esta liberado esa fecha
     */
    @Transactional
    public ReleaseResponse createRelease(String requesterLogin, ReleaseCreateRequest request) {
        Long employeeId = resolveEmployeeId(requesterLogin);
        LocalDate releaseDate = request.releaseDate();
        ResourceType resourceType = request.resourceTypeOrDefault();
        requireNotPast(releaseDate);
        Long spaceId = resolveVoluntarySpace(
                employeeId, resourceType, releaseDate, request.parkingSpaceId());
        requireResourceNotReleased(spaceId, resourceType, releaseDate);
        Release saved = releaseRepository.saveAndFlush(
                Release.voluntary(spaceId, resourceType, employeeId, releaseDate, clock.now()));
        ReleaseResponse response = ReleaseResponse.from(saved);
        publish(ReleaseAuditEvent.Kind.VOLUNTARY_RELEASED, response);
        return response;
    }

    /**
     * Crea una liberacion administrativa del recurso fijo de un empleado, ejecutada por
     * un {@code ADMIN}, para una fecha presente o futura y con motivo obligatorio.
     *
     * @param adminLogin login del administrador que ejecuta (principal de la sesion)
     * @param request    empleado, recurso, fecha y motivo de la liberacion
     * @return la liberacion creada (DTO)
     * @throws EntityNotFoundException          si el empleado o el recurso no existen
     * @throws ReleaseDateInPastException       si la fecha es anterior a hoy
     * @throws NoFixedAssignmentException       si no hay asignacion fija activa del recurso ese dia
     * @throws ResourceAlreadyReleasedException si el recurso ya esta liberado esa fecha
     */
    @Transactional
    public ReleaseResponse createAdministrativeRelease(
            String adminLogin, AdministrativeReleaseRequest request) {
        Long adminId = resolveEmployeeId(adminLogin);
        LocalDate releaseDate = request.releaseDate();
        ResourceType resourceType = request.resourceTypeOrDefault();
        requireNotPast(releaseDate);
        Long employeeId = request.employeeId();
        Long spaceId = request.parkingSpaceId();
        requireEmployeeExists(employeeId);
        requireResourceExists(spaceId, resourceType);
        requireActiveAssignment(employeeId, spaceId, resourceType, releaseDate);
        requireResourceNotReleased(spaceId, resourceType, releaseDate);
        Release saved = releaseRepository.saveAndFlush(
                Release.administrative(spaceId, resourceType, employeeId, releaseDate,
                        request.reason(), adminId, clock.now()));
        ReleaseResponse response = ReleaseResponse.from(saved);
        publish(ReleaseAuditEvent.Kind.ADMINISTRATIVE_RELEASED, response);
        return response;
    }

    /**
     * Lista de forma paginada las liberaciones del empleado de la sesion (verificacion de
     * pertenencia implicita: solo las propias).
     *
     * @param requesterLogin login del empleado (principal de la sesion)
     * @param pageable       pagina y tamano solicitados
     * @return pagina de liberaciones propias (DTO)
     */
    @Transactional(readOnly = true)
    public PageResponse<ReleaseResponse> listMyReleases(String requesterLogin, Pageable pageable) {
        Long employeeId = resolveEmployeeId(requesterLogin);
        return PageResponse.from(
                releaseRepository.findByEmployeeId(employeeId, pageable), ReleaseResponse::from);
    }

    /**
     * Cancela (borrado fisico) una liberacion futura propia del empleado de la sesion.
     *
     * @param id             identificador de la liberacion
     * @param requesterLogin login del empleado (principal de la sesion)
     * @throws EntityNotFoundException          si la liberacion no existe
     * @throws AccessDeniedException            si el solicitante no es el propietario (BOLA)
     * @throws PastReleaseCancellationException si la liberacion es de una fecha pasada
     */
    @Transactional
    public void cancelRelease(Long id, String requesterLogin) {
        Release release = loadRelease(id);
        Long employeeId = resolveEmployeeId(requesterLogin);
        if (!release.getEmployeeId().equals(employeeId)) {
            throw new AccessDeniedException(MSG_NOT_OWNER);
        }
        if (release.getReleaseDate().isBefore(today())) {
            throw new PastReleaseCancellationException(MSG_PAST_CANCELLATION);
        }
        ReleaseResponse snapshot = ReleaseResponse.from(release);
        releaseRepository.delete(release);
        publish(ReleaseAuditEvent.Kind.CANCELLED, snapshot);
    }

    private Long resolveVoluntarySpace(
            Long employeeId, ResourceType resourceType, LocalDate releaseDate, Long requestedSpaceId) {
        int dayOfWeek = releaseDate.getDayOfWeek().getValue();
        List<FixedAssignment> assignments =
                fixedAssignmentRepository.findByEmployeeIdAndResourceTypeAndDayOfWeekAndActiveTrue(
                        employeeId, resourceType, dayOfWeek);
        if (requestedSpaceId != null) {
            boolean owns = assignments.stream()
                    .anyMatch(assignment -> assignment.getResourceId().equals(requestedSpaceId));
            if (!owns) {
                throw new NoFixedAssignmentException(MSG_NO_FIXED_ASSIGNMENT);
            }
            return requestedSpaceId;
        }
        if (assignments.isEmpty()) {
            throw new NoFixedAssignmentException(MSG_NO_FIXED_ASSIGNMENT);
        }
        if (assignments.size() > 1) {
            throw new NoFixedAssignmentException(MSG_AMBIGUOUS_ASSIGNMENT);
        }
        return assignments.get(0).getResourceId();
    }

    private void requireActiveAssignment(
            Long employeeId, Long spaceId, ResourceType resourceType, LocalDate releaseDate) {
        int dayOfWeek = releaseDate.getDayOfWeek().getValue();
        boolean present = fixedAssignmentRepository
                .findByEmployeeIdAndResourceTypeAndDayOfWeekAndActiveTrue(
                        employeeId, resourceType, dayOfWeek).stream()
                .anyMatch(assignment -> assignment.getResourceId().equals(spaceId));
        if (!present) {
            throw new NoFixedAssignmentException(MSG_NO_FIXED_ASSIGNMENT);
        }
    }

    private void requireNotPast(LocalDate releaseDate) {
        if (releaseDate.isBefore(today())) {
            throw new ReleaseDateInPastException(MSG_DATE_IN_PAST);
        }
    }

    private void requireResourceNotReleased(
            Long spaceId, ResourceType resourceType, LocalDate releaseDate) {
        if (releaseRepository.existsByResourceIdAndResourceTypeAndReleaseDate(
                spaceId, resourceType, releaseDate)) {
            throw new ResourceAlreadyReleasedException(MSG_ALREADY_RELEASED);
        }
    }

    private void requireEmployeeExists(Long employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new EntityNotFoundException(MSG_EMPLOYEE_NOT_FOUND + employeeId);
        }
    }

    private void requireResourceExists(Long spaceId, ResourceType resourceType) {
        if (!resourceResolvers.exists(spaceId, resourceType)) {
            throw new EntityNotFoundException(MSG_SPACE_NOT_FOUND + spaceId);
        }
    }

    private Release loadRelease(Long id) {
        return releaseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(MSG_RELEASE_NOT_FOUND + id));
    }

    private Long resolveEmployeeId(String login) {
        return employeeRepository.findByLogin(login)
                .map(Employee::getId)
                .orElseThrow(() -> new EntityNotFoundException(MSG_ACTOR_NOT_FOUND + login));
    }

    private LocalDate today() {
        return LocalDate.ofInstant(clock.now(), ZoneOffset.UTC);
    }

    private void publish(ReleaseAuditEvent.Kind kind, ReleaseResponse response) {
        eventPublisher.publishEvent(new ReleaseAuditEvent(kind, response));
    }
}
