package com.aleatica.parking.availability.application;

import com.aleatica.parking.auth.domain.ClockPort;
import com.aleatica.parking.availability.CalendarCellState;
import com.aleatica.parking.availability.MyWeekDayState;
import com.aleatica.parking.availability.dto.AdminWeeklyCalendarResponse;
import com.aleatica.parking.availability.dto.AvailabilityItemResponse;
import com.aleatica.parking.availability.dto.AvailabilityResponse;
import com.aleatica.parking.availability.dto.CalendarCellResponse;
import com.aleatica.parking.availability.dto.CalendarRowResponse;
import com.aleatica.parking.availability.dto.MyWeekDayResponse;
import com.aleatica.parking.availability.dto.MyWeekResponse;
import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.fixedassignment.FixedAssignment;
import com.aleatica.parking.fixedassignment.FixedAssignmentRepository;
import com.aleatica.parking.parkingspace.ParkingSpace;
import com.aleatica.parking.parkingspace.ParkingSpaceRepository;
import com.aleatica.parking.release.Release;
import com.aleatica.parking.release.ReleaseRepository;
import com.aleatica.parking.request.Request;
import com.aleatica.parking.request.RequestRepository;
import com.aleatica.parking.request.RequestStatus;
import com.aleatica.parking.visitor.VisitorReservation;
import com.aleatica.parking.visitor.VisitorReservationRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de dominio de disponibilidad y calendario (consulta-only): consolida en un
 * unico sitio el calculo de disponibilidad de un recurso para una fecha, reutilizado por
 * la disponibilidad puntual, el calendario semanal admin y "Mi Semana".
 *
 * <p><strong>Definicion consolidada de disponibilidad.</strong> Una plaza esta disponible
 * para una fecha F si y solo si: esta activa; y no tiene una {@code FixedAssignment} activa
 * para {@code dayOfWeek(F)} (o, teniendola, existe un {@code Release} para esa plaza y F); y
 * no existe una {@code Request} {@code APPROVED} para esa plaza y F; y (solo plazas) no
 * existe una {@code VisitorReservation} para esa plaza y F. Es exactamente la misma regla,
 * con el mismo mapeo de dia de la semana ({@code getDayOfWeek().getValue()}, 1=Lunes..7=Domingo),
 * que aplican en linea {@code RequestService#approve} y {@code VisitorReservationService#create};
 * este servicio la centraliza para que no diverjan.</p>
 *
 * <p><strong>Sin N+1.</strong> Todas las vistas cargan por rango: una consulta por entidad
 * (plazas activas, asignaciones fijas de esas plazas, liberaciones, solicitudes aprobadas y
 * reservas del intervalo) y ensamblan en memoria; nunca una consulta por celda ni por dia.</p>
 *
 * <p><strong>Privacidad.</strong> "Mi Semana" se filtra a los recursos propios del solicitante
 * y no serializa ningun campo de identidad (ni propia ni ajena); el calendario completo con
 * titulares es exclusivo del {@code ADMIN} (autorizacion en el controlador).</p>
 */
@Service
public class AvailabilityService {

    private static final int WEEK_DAYS = 7;
    private static final String MSG_ACTOR_NOT_FOUND = "Usuario de sesion no encontrado: ";

    private final ParkingSpaceRepository parkingSpaceRepository;
    private final FixedAssignmentRepository fixedAssignmentRepository;
    private final ReleaseRepository releaseRepository;
    private final RequestRepository requestRepository;
    private final VisitorReservationRepository visitorReservationRepository;
    private final EmployeeRepository employeeRepository;
    private final ClockPort clock;

    /**
     * @param parkingSpaceRepository       repositorio de plazas (recursos activos)
     * @param fixedAssignmentRepository    repositorio de asignaciones fijas (por dia de la semana)
     * @param releaseRepository            repositorio de liberaciones (libera el recurso una fecha)
     * @param requestRepository            repositorio de solicitudes (APPROVED ocupa el recurso)
     * @param visitorReservationRepository repositorio de reservas de visitante (ocupa la plaza)
     * @param employeeRepository           repositorio de empleados (titulares, resolucion de sesion)
     * @param clock                        reloj inyectable (semana actual en "Mi Semana")
     */
    public AvailabilityService(
            ParkingSpaceRepository parkingSpaceRepository,
            FixedAssignmentRepository fixedAssignmentRepository,
            ReleaseRepository releaseRepository,
            RequestRepository requestRepository,
            VisitorReservationRepository visitorReservationRepository,
            EmployeeRepository employeeRepository,
            ClockPort clock) {
        this.parkingSpaceRepository = parkingSpaceRepository;
        this.fixedAssignmentRepository = fixedAssignmentRepository;
        this.releaseRepository = releaseRepository;
        this.requestRepository = requestRepository;
        this.visitorReservationRepository = visitorReservationRepository;
        this.employeeRepository = employeeRepository;
        this.clock = clock;
    }

    /**
     * Clave (plaza, fecha) para los indices en memoria del ensamblado.
     *
     * @param spaceId identificador de la plaza
     * @param date    fecha
     */
    private record SpaceDate(Long spaceId, LocalDate date) {
    }

    /**
     * Clave (plaza, dia de la semana) para indexar las asignaciones fijas.
     *
     * @param spaceId identificador de la plaza
     * @param dow     dia de la semana (1=Lunes..7=Domingo)
     */
    private record SpaceDow(Long spaceId, int dow) {
    }

    // -------------------------------------------------------------------------
    // Disponibilidad puntual
    // -------------------------------------------------------------------------

    /**
     * Devuelve los recursos (plazas) disponibles para una fecha aplicando las cuatro
     * condiciones consolidadas de disponibilidad.
     *
     * @param date fecha a consultar
     * @return la disponibilidad de la fecha (lista de plazas disponibles, posiblemente vacia)
     */
    @Transactional(readOnly = true)
    public AvailabilityResponse availabilityForDate(LocalDate date) {
        List<ParkingSpace> spaces = parkingSpaceRepository.findByActiveTrueOrderByIdAsc();
        List<Long> spaceIds = spaces.stream().map(ParkingSpace::getId).toList();
        int dow = date.getDayOfWeek().getValue();

        Set<Long> fixedAssigned = activeFixedSpaceIdsForDay(spaceIds, dow);
        Set<Long> released = spaceIds(releaseRepository.findByReleaseDateBetween(date, date),
                Release::getParkingSpaceId);
        Set<Long> approved = spaceIds(
                requestRepository.findByStatusAndRequestedDateBetween(RequestStatus.APPROVED, date, date),
                Request::getParkingSpaceId);
        Set<Long> reserved = spaceIds(visitorReservationRepository.findByReservationDateBetween(date, date),
                VisitorReservation::getParkingSpaceId);

        List<AvailabilityItemResponse> items = spaces.stream()
                .filter(space -> isAvailable(space.getId(), fixedAssigned, released, approved, reserved))
                .map(space -> new AvailabilityItemResponse(space.getId(), space.getLabel()))
                .toList();
        return new AvailabilityResponse(date, items);
    }

    private boolean isAvailable(
            Long spaceId, Set<Long> fixedAssigned, Set<Long> released, Set<Long> approved,
            Set<Long> reserved) {
        boolean fixedTaken = fixedAssigned.contains(spaceId) && !released.contains(spaceId);
        return !fixedTaken && !approved.contains(spaceId) && !reserved.contains(spaceId);
    }

    // -------------------------------------------------------------------------
    // Calendario semanal admin
    // -------------------------------------------------------------------------

    /**
     * Construye el calendario semanal completo de todas las plazas activas para el
     * {@code ADMIN}, con el estado y el titular de cada celda (plaza, dia).
     *
     * @param weekStartInput lunes de la semana solicitada (se normaliza al lunes de esa semana)
     * @return el calendario semanal admin
     */
    @Transactional(readOnly = true)
    public AdminWeeklyCalendarResponse adminCalendar(LocalDate weekStartInput) {
        LocalDate weekStart = mondayOf(weekStartInput);
        LocalDate weekEnd = weekStart.plusDays(WEEK_DAYS - 1L);
        List<LocalDate> days = weekDays(weekStart);

        List<ParkingSpace> spaces = parkingSpaceRepository.findByActiveTrueOrderByIdAsc();
        List<Long> spaceIds = spaces.stream().map(ParkingSpace::getId).toList();

        List<FixedAssignment> fixed = activeFixed(spaceIds);
        Map<SpaceDow, FixedAssignment> fixedBySpaceDow = fixed.stream()
                .collect(Collectors.toMap(
                        fa -> new SpaceDow(fa.getParkingSpaceId(), fa.getDayOfWeek()),
                        Function.identity(), (a, b) -> a));
        Set<SpaceDate> releasedKeys = releaseRepository.findByReleaseDateBetween(weekStart, weekEnd).stream()
                .map(r -> new SpaceDate(r.getParkingSpaceId(), r.getReleaseDate()))
                .collect(Collectors.toSet());
        Map<SpaceDate, Request> approvedBySpaceDate = requestRepository
                .findByStatusAndRequestedDateBetween(RequestStatus.APPROVED, weekStart, weekEnd).stream()
                .collect(Collectors.toMap(
                        r -> new SpaceDate(r.getParkingSpaceId(), r.getRequestedDate()),
                        Function.identity(), (a, b) -> a));
        Map<Long, String> names = employeeNames(fixed, approvedBySpaceDate.values());

        List<CalendarRowResponse> rows = spaces.stream()
                .map(space -> row(space, days, fixedBySpaceDow, releasedKeys, approvedBySpaceDate, names))
                .toList();
        return new AdminWeeklyCalendarResponse(weekStart, days, rows);
    }

    private CalendarRowResponse row(
            ParkingSpace space, List<LocalDate> days, Map<SpaceDow, FixedAssignment> fixedBySpaceDow,
            Set<SpaceDate> releasedKeys, Map<SpaceDate, Request> approvedBySpaceDate,
            Map<Long, String> names) {
        List<CalendarCellResponse> cells = days.stream()
                .map(day -> cell(space.getId(), day, fixedBySpaceDow, releasedKeys, approvedBySpaceDate, names))
                .toList();
        return new CalendarRowResponse(space.getId(), space.getLabel(), cells);
    }

    private CalendarCellResponse cell(
            Long spaceId, LocalDate day, Map<SpaceDow, FixedAssignment> fixedBySpaceDow,
            Set<SpaceDate> releasedKeys, Map<SpaceDate, Request> approvedBySpaceDate,
            Map<Long, String> names) {
        SpaceDate spaceDate = new SpaceDate(spaceId, day);
        Request approved = approvedBySpaceDate.get(spaceDate);
        if (approved != null) {
            return new CalendarCellResponse(day, CalendarCellState.REQUEST_APPROVED,
                    approved.getEmployeeId(), names.get(approved.getEmployeeId()), approved.getId());
        }
        FixedAssignment assignment = fixedBySpaceDow.get(new SpaceDow(spaceId, day.getDayOfWeek().getValue()));
        if (assignment != null) {
            CalendarCellState state = releasedKeys.contains(spaceDate)
                    ? CalendarCellState.RELEASED : CalendarCellState.ASSIGNED;
            return new CalendarCellResponse(day, state, assignment.getEmployeeId(),
                    names.get(assignment.getEmployeeId()), null);
        }
        return new CalendarCellResponse(day, CalendarCellState.FREE, null, null, null);
    }

    // -------------------------------------------------------------------------
    // Mi Semana
    // -------------------------------------------------------------------------

    /**
     * Construye la vista personal de la semana del solicitante: el estado diario referido
     * unicamente a sus recursos propios, sin exponer identidad de terceros.
     *
     * @param requesterLogin login del solicitante (principal de la sesion)
     * @param weekStartInput lunes de la semana; {@code null} para la semana actual (via reloj)
     * @return la vista "Mi Semana" del solicitante
     * @throws EntityNotFoundException si el login de sesion no corresponde a ningun empleado
     */
    @Transactional(readOnly = true)
    public MyWeekResponse myWeek(String requesterLogin, LocalDate weekStartInput) {
        Long employeeId = resolveEmployeeId(requesterLogin);
        LocalDate weekStart = weekStartInput != null ? mondayOf(weekStartInput) : mondayOf(today());
        LocalDate weekEnd = weekStart.plusDays(WEEK_DAYS - 1L);
        List<LocalDate> days = weekDays(weekStart);

        List<FixedAssignment> myFixed =
                fixedAssignmentRepository.findByEmployeeIdAndActiveTrueOrderByDayOfWeekAsc(employeeId);
        List<Request> myRequests =
                requestRepository.findByEmployeeIdAndRequestedDateBetween(employeeId, weekStart, weekEnd);
        List<Release> myReleases =
                releaseRepository.findByEmployeeIdAndReleaseDateBetween(employeeId, weekStart, weekEnd);

        Map<Integer, FixedAssignment> fixedByDow = myFixed.stream()
                .collect(Collectors.toMap(FixedAssignment::getDayOfWeek, Function.identity(), (a, b) -> a));
        Map<LocalDate, Request> approvedByDate = requestsByDate(myRequests, RequestStatus.APPROVED);
        Map<LocalDate, Request> pendingByDate = requestsByDate(myRequests, RequestStatus.PENDING);
        Set<SpaceDate> releasedKeys = myReleases.stream()
                .map(r -> new SpaceDate(r.getParkingSpaceId(), r.getReleaseDate()))
                .collect(Collectors.toSet());
        Map<Long, String> labels = spaceLabels(fixedByDow, approvedByDate);

        List<MyWeekDayResponse> dayViews = days.stream()
                .map(day -> myWeekDay(day, fixedByDow, approvedByDate, pendingByDate, releasedKeys, labels))
                .toList();
        return new MyWeekResponse(weekStart, dayViews);
    }

    private MyWeekDayResponse myWeekDay(
            LocalDate day, Map<Integer, FixedAssignment> fixedByDow, Map<LocalDate, Request> approvedByDate,
            Map<LocalDate, Request> pendingByDate, Set<SpaceDate> releasedKeys, Map<Long, String> labels) {
        Request approved = approvedByDate.get(day);
        if (approved != null) {
            return new MyWeekDayResponse(day, MyWeekDayState.ASSIGNED,
                    labels.get(approved.getParkingSpaceId()), RequestStatus.APPROVED);
        }
        FixedAssignment assignment = fixedByDow.get(day.getDayOfWeek().getValue());
        if (assignment != null) {
            boolean released = releasedKeys.contains(new SpaceDate(assignment.getParkingSpaceId(), day));
            MyWeekDayState state = released ? MyWeekDayState.RELEASED : MyWeekDayState.ASSIGNED;
            return new MyWeekDayResponse(day, state, labels.get(assignment.getParkingSpaceId()), null);
        }
        if (pendingByDate.containsKey(day)) {
            return new MyWeekDayResponse(day, MyWeekDayState.REQUEST_PENDING, null, RequestStatus.PENDING);
        }
        return new MyWeekDayResponse(day, MyWeekDayState.FREE, null, null);
    }

    // -------------------------------------------------------------------------
    // Helpers de carga y utilidades
    // -------------------------------------------------------------------------

    private Set<Long> activeFixedSpaceIdsForDay(List<Long> spaceIds, int dow) {
        return activeFixed(spaceIds).stream()
                .filter(fa -> fa.getDayOfWeek() == dow)
                .map(FixedAssignment::getParkingSpaceId)
                .collect(Collectors.toSet());
    }

    private List<FixedAssignment> activeFixed(List<Long> spaceIds) {
        if (spaceIds.isEmpty()) {
            return List.of();
        }
        return fixedAssignmentRepository.findByParkingSpaceIdInAndActiveTrue(spaceIds);
    }

    private Map<Long, String> employeeNames(
            List<FixedAssignment> fixed, Collection<Request> approved) {
        Set<Long> ids = fixed.stream().map(FixedAssignment::getEmployeeId)
                .collect(Collectors.toCollection(HashSet::new));
        approved.forEach(r -> ids.add(r.getEmployeeId()));
        if (ids.isEmpty()) {
            return Map.of();
        }
        return employeeRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Employee::getId, AvailabilityService::fullName));
    }

    private Map<Long, String> spaceLabels(
            Map<Integer, FixedAssignment> fixedByDow, Map<LocalDate, Request> approvedByDate) {
        Set<Long> ids = fixedByDow.values().stream().map(FixedAssignment::getParkingSpaceId)
                .collect(Collectors.toCollection(HashSet::new));
        approvedByDate.values().forEach(r -> ids.add(r.getParkingSpaceId()));
        if (ids.isEmpty()) {
            return Map.of();
        }
        return parkingSpaceRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(ParkingSpace::getId, ParkingSpace::getLabel));
    }

    private static Map<LocalDate, Request> requestsByDate(List<Request> requests, RequestStatus status) {
        return requests.stream()
                .filter(r -> r.getStatus() == status)
                .collect(Collectors.toMap(Request::getRequestedDate, Function.identity(), (a, b) -> a));
    }

    private static <T> Set<Long> spaceIds(List<T> rows, Function<T, Long> extractor) {
        return rows.stream().map(extractor).collect(Collectors.toSet());
    }

    private static String fullName(Employee employee) {
        return employee.getFirstName() + " " + employee.getLastName();
    }

    private static LocalDate mondayOf(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private static List<LocalDate> weekDays(LocalDate weekStart) {
        return IntStream.range(0, WEEK_DAYS)
                .mapToObj(offset -> weekStart.plusDays(offset))
                .toList();
    }

    private LocalDate today() {
        return LocalDate.ofInstant(clock.now(), ZoneOffset.UTC);
    }

    private Long resolveEmployeeId(String login) {
        return employeeRepository.findByLogin(login)
                .map(Employee::getId)
                .orElseThrow(() -> new EntityNotFoundException(MSG_ACTOR_NOT_FOUND + login));
    }
}
