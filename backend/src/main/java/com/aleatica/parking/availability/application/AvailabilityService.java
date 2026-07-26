package com.aleatica.parking.availability.application;

import com.aleatica.parking.auth.domain.ClockPort;
import com.aleatica.parking.availability.CalendarCellState;
import com.aleatica.parking.availability.MyWeekDayState;
import com.aleatica.parking.availability.OccupancyOrigin;
import com.aleatica.parking.availability.dto.AdminWeeklyCalendarResponse;
import com.aleatica.parking.availability.dto.AvailabilityItemResponse;
import com.aleatica.parking.availability.dto.AvailabilityResponse;
import com.aleatica.parking.availability.dto.CalendarCellResponse;
import com.aleatica.parking.availability.dto.CalendarRowResponse;
import com.aleatica.parking.availability.dto.EmployeeWeekDayResponse;
import com.aleatica.parking.availability.dto.EmployeeWeekOccupancyResponse;
import com.aleatica.parking.availability.dto.MyWeekDayResponse;
import com.aleatica.parking.availability.dto.MyWeekResponse;
import com.aleatica.parking.availability.dto.OccupancyItemResponse;
import com.aleatica.parking.availability.dto.OccupancyResponse;
import com.aleatica.parking.desk.Desk;
import com.aleatica.parking.desk.DeskRepository;
import com.aleatica.parking.resource.BookableResource;
import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.fixedassignment.infrastructure.FixedAssignmentEntity;
import com.aleatica.parking.fixedassignment.infrastructure.FixedAssignmentJpaRepository;
import com.aleatica.parking.parkingspace.ParkingSpace;
import com.aleatica.parking.parkingspace.ParkingSpaceRepository;
import com.aleatica.parking.release.infrastructure.ReleaseEntity;
import com.aleatica.parking.release.infrastructure.ReleaseJpaRepository;
import com.aleatica.parking.request.infrastructure.RequestEntity;
import com.aleatica.parking.request.infrastructure.RequestJpaRepository;
import com.aleatica.parking.request.domain.RequestStatus;
import com.aleatica.parking.resource.ResourceType;
import com.aleatica.parking.visitor.Visitor;
import com.aleatica.parking.visitor.VisitorRepository;
import com.aleatica.parking.visitor.VisitorReservation;
import com.aleatica.parking.visitor.VisitorReservationRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
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
 * no existe una {@code RequestEntity} {@code APPROVED} para esa plaza y F; y (solo plazas) no
 * existe una {@code VisitorReservation} para esa plaza y F. Es exactamente la misma regla,
 * con el mismo mapeo de dia de la semana ({@code getDayOfWeek().getValue()}, 1=Lunes..7=Domingo),
 * que aplica {@code VisitorReservationService#create} y que reutiliza
 * {@code RequestService#approve} delegando en {@link #isSpaceTakenForDate(Long, java.time.LocalDate)};
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
    private static final String MSG_EMPLOYEE_NOT_FOUND = "Empleado no encontrado: ";

    private final ParkingSpaceRepository parkingSpaceRepository;
    private final DeskRepository deskRepository;
    private final FixedAssignmentJpaRepository fixedAssignmentRepository;
    private final ReleaseJpaRepository releaseRepository;
    private final RequestJpaRepository requestRepository;
    private final VisitorReservationRepository visitorReservationRepository;
    private final VisitorRepository visitorRepository;
    private final EmployeeRepository employeeRepository;
    private final ClockPort clock;

    /**
     * @param parkingSpaceRepository       repositorio de plazas (recursos activos PARKING)
     * @param deskRepository               repositorio de puestos (recursos activos DESK)
     * @param fixedAssignmentRepository    repositorio de asignaciones fijas (por dia de la semana)
     * @param releaseRepository            repositorio de liberaciones (libera el recurso una fecha)
     * @param requestRepository            repositorio de solicitudes (APPROVED ocupa el recurso)
     * @param visitorReservationRepository repositorio de reservas de visitante (ocupa la plaza)
     * @param employeeRepository           repositorio de empleados (titulares, resolucion de sesion)
     * @param clock                        reloj inyectable (semana actual en "Mi Semana")
     */
    public AvailabilityService(
            ParkingSpaceRepository parkingSpaceRepository,
            DeskRepository deskRepository,
            FixedAssignmentJpaRepository fixedAssignmentRepository,
            ReleaseJpaRepository releaseRepository,
            RequestJpaRepository requestRepository,
            VisitorReservationRepository visitorReservationRepository,
            VisitorRepository visitorRepository,
            EmployeeRepository employeeRepository,
            ClockPort clock) {
        this.parkingSpaceRepository = parkingSpaceRepository;
        this.deskRepository = deskRepository;
        this.fixedAssignmentRepository = fixedAssignmentRepository;
        this.releaseRepository = releaseRepository;
        this.requestRepository = requestRepository;
        this.visitorReservationRepository = visitorReservationRepository;
        this.visitorRepository = visitorRepository;
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

    /**
     * Recurso reservable minimo (id + etiqueta) para el calculo de disponibilidad, comun a
     * plazas ({@code PARKING}) y puestos ({@code DESK}).
     *
     * @param id    identificador del recurso dentro de su tabla
     * @param label etiqueta humana del recurso ({@code P-08} / {@code D-05})
     */
    private record ResourceRef(Long id, String label) {
    }

    /**
     * Clave (tipo de recurso, fecha) para indexar las solicitudes aprobadas del empleado en la
     * ocupacion semanal por empleado.
     *
     * @param type tipo de recurso ({@code PARKING}/{@code DESK})
     * @param date fecha
     */
    private record TypeDate(ResourceType type, LocalDate date) {
    }

    /**
     * Clave (tipo de recurso, dia de la semana) para indexar las asignaciones fijas del empleado
     * en la ocupacion semanal por empleado.
     *
     * @param type tipo de recurso ({@code PARKING}/{@code DESK})
     * @param dow  dia de la semana (1=Lunes..7=Domingo)
     */
    private record TypeDow(ResourceType type, int dow) {
    }

    /**
     * Clave (tipo de recurso, recurso, fecha) para indexar las liberaciones del empleado en la
     * ocupacion semanal por empleado.
     *
     * @param type       tipo de recurso ({@code PARKING}/{@code DESK})
     * @param resourceId identificador del recurso
     * @param date       fecha liberada
     */
    private record TypeResourceDate(ResourceType type, Long resourceId, LocalDate date) {
    }

    /**
     * Contexto ensamblado de la ocupacion semanal de un empleado: los indices en memoria cargados
     * por rango (una consulta por entidad) mas la identidad del empleado, para calcular las
     * reservas de cada dia sin N+1 ni pasar demasiados parametros sueltos (S107).
     *
     * @param employeeId       empleado consultado
     * @param employeeName     nombre completo del empleado consultado
     * @param fixedByTypeDow   asignaciones fijas activas del empleado por (tipo, dia de la semana)
     * @param approvedByTypeDate solicitudes aprobadas del empleado por (tipo, fecha)
     * @param releasedKeys     liberaciones del empleado por (tipo, recurso, fecha)
     * @param resourcesByType  recursos referenciados resueltos a su numero/planta, por tipo e id
     */
    private record WeekOccupancyContext(
            Long employeeId,
            String employeeName,
            Map<TypeDow, FixedAssignmentEntity> fixedByTypeDow,
            Map<TypeDate, RequestEntity> approvedByTypeDate,
            Set<TypeResourceDate> releasedKeys,
            Map<ResourceType, Map<Long, BookableResource>> resourcesByType) {
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
        return availabilityForDate(date, ResourceType.PARKING);
    }

    /**
     * Devuelve los recursos de un tipo ({@code PARKING} plazas / {@code DESK} puestos)
     * disponibles para una fecha aplicando las condiciones consolidadas de disponibilidad.
     *
     * <p>Todas las lecturas por rango se filtran por {@code resource_type}, de modo que un
     * puesto con el mismo {@code resource_id} que una plaza no interfiera en el calculo (los
     * identificadores no son unicos entre tablas de recurso). La reserva de visitante solo
     * aplica a {@code PARKING}: los puestos no tienen reservas de visitante (design §Decisions),
     * por lo que ese termino se omite para {@code DESK}.</p>
     *
     * @param date         fecha a consultar
     * @param resourceType tipo de recurso ({@code PARKING}/{@code DESK})
     * @return la disponibilidad de la fecha (lista de recursos disponibles, posiblemente vacia)
     */
    @Transactional(readOnly = true)
    public AvailabilityResponse availabilityForDate(LocalDate date, ResourceType resourceType) {
        List<ResourceRef> resources = activeResources(resourceType);
        List<Long> resourceIds = resources.stream().map(ResourceRef::id).toList();
        int dow = date.getDayOfWeek().getValue();

        Set<Long> fixedAssigned = activeFixedResourceIdsForDay(resourceIds, resourceType, dow);
        Set<Long> released = spaceIds(
                releaseRepository.findByResourceTypeAndReleaseDateBetween(resourceType, date, date),
                ReleaseEntity::getResourceId);
        Set<Long> approved = spaceIds(
                requestRepository.findByStatusAndResourceTypeAndRequestedDateBetween(
                        RequestStatus.APPROVED, resourceType, date, date),
                RequestEntity::getResourceId);
        // Las reservas de visitante ocupan el recurso (plaza O puesto) su fecha: se descuentan
        // en ambos tipos (data-model genérico, migración V25).
        Set<Long> reserved = spaceIds(
                visitorReservationRepository.findByResourceTypeAndReservationDateBetween(
                        resourceType, date, date),
                VisitorReservation::getResourceId);

        List<AvailabilityItemResponse> items = resources.stream()
                .filter(ref -> isAvailable(ref.id(), fixedAssigned, released, approved, reserved))
                .map(ref -> new AvailabilityItemResponse(ref.id(), ref.label()))
                .toList();
        return new AvailabilityResponse(date, items);
    }

    /**
     * Devuelve las plazas de parking LIBRES para una fecha (misma regla consolidada de
     * disponibilidad que {@link #availabilityForDate(LocalDate)}: activa, sin asignacion fija
     * vigente/liberada, sin solicitud {@code APPROVED}, sin reserva de visitante), como
     * entidades de dominio para que el consumidor pueda agrupar por planta
     * ({@code number / 1000}).
     *
     * <p>Uso interno servicio-a-servicio (auto-asignacion de plaza por categoria/planta, change
     * {@code request-auto-assignment}): NO se expone en la capa web, por lo que devolver la
     * entidad de plaza aqui no viola S4684 (la frontera web sigue trabajando con DTOs). Carga
     * por rango (una consulta por entidad) para evitar N+1. El orden dentro de la lista lo fija
     * el consumidor de forma determinista.</p>
     *
     * @param date fecha a consultar
     * @return las plazas activas disponibles para la fecha (posiblemente vacia)
     */
    @Transactional(readOnly = true)
    public List<ParkingSpace> freeParkingSpacesForDate(LocalDate date) {
        List<ParkingSpace> spaces = parkingSpaceRepository.findByActiveTrueOrderByIdAsc();
        List<Long> spaceIds = spaces.stream().map(ParkingSpace::getId).toList();
        int dow = date.getDayOfWeek().getValue();

        Set<Long> fixedAssigned = activeFixedResourceIdsForDay(spaceIds, ResourceType.PARKING, dow);
        Set<Long> released = spaceIds(
                releaseRepository.findByResourceTypeAndReleaseDateBetween(ResourceType.PARKING, date, date),
                ReleaseEntity::getResourceId);
        Set<Long> approved = spaceIds(
                requestRepository.findByStatusAndResourceTypeAndRequestedDateBetween(
                        RequestStatus.APPROVED, ResourceType.PARKING, date, date),
                RequestEntity::getResourceId);
        Set<Long> reserved = spaceIds(
                visitorReservationRepository.findByResourceTypeAndReservationDateBetween(
                        ResourceType.PARKING, date, date),
                VisitorReservation::getResourceId);

        return spaces.stream()
                .filter(space -> isAvailable(space.getId(), fixedAssigned, released, approved, reserved))
                .toList();
    }

    /**
     * Devuelve los puestos de oficina LIBRES para una fecha (misma regla consolidada de
     * disponibilidad que {@link #availabilityForDate(LocalDate, ResourceType)} con
     * {@code ResourceType.DESK}: activo, sin asignacion fija vigente/liberada, sin solicitud
     * {@code APPROVED}; los puestos no tienen reserva de visitante, design §Decisions), como
     * entidades de dominio para que el consumidor pueda filtrar por {@link com.aleatica.parking.desk.DeskCategory}.
     *
     * <p>Uso interno servicio-a-servicio (auto-asignacion de puesto por categoria, change
     * {@code desk-auto-assignment}): NO se expone en la capa web (S4684). Carga por rango (una
     * consulta por entidad) para evitar N+1, espejo de {@link #freeParkingSpacesForDate(LocalDate)}.
     * El orden dentro de la lista lo fija el consumidor de forma determinista.</p>
     *
     * @param date fecha a consultar
     * @return los puestos activos disponibles para la fecha (posiblemente vacia)
     */
    @Transactional(readOnly = true)
    public List<Desk> freeDesksForDate(LocalDate date) {
        List<Desk> desks = deskRepository.findByActiveTrueOrderByNumberAsc();
        List<Long> deskIds = desks.stream().map(Desk::getId).toList();
        int dow = date.getDayOfWeek().getValue();

        Set<Long> fixedAssigned = activeFixedResourceIdsForDay(deskIds, ResourceType.DESK, dow);
        Set<Long> released = spaceIds(
                releaseRepository.findByResourceTypeAndReleaseDateBetween(ResourceType.DESK, date, date),
                ReleaseEntity::getResourceId);
        Set<Long> approved = spaceIds(
                requestRepository.findByStatusAndResourceTypeAndRequestedDateBetween(
                        RequestStatus.APPROVED, ResourceType.DESK, date, date),
                RequestEntity::getResourceId);
        // Los puestos no tienen reserva de visitante (design §Decisions): conjunto vacio.
        Set<Long> reserved = Set.of();

        return desks.stream()
                .filter(desk -> isAvailable(desk.getId(), fixedAssigned, released, approved, reserved))
                .toList();
    }

    private List<ResourceRef> activeResources(ResourceType resourceType) {
        if (resourceType == ResourceType.DESK) {
            return deskRepository.findByActiveTrueOrderByNumberAsc().stream()
                    .map(desk -> new ResourceRef(desk.getId(), desk.getLabel()))
                    .toList();
        }
        return parkingSpaceRepository.findByActiveTrueOrderByIdAsc().stream()
                .map(space -> new ResourceRef(space.getId(), space.getLabel()))
                .toList();
    }

    private boolean isAvailable(
            Long spaceId, Set<Long> fixedAssigned, Set<Long> released, Set<Long> approved,
            Set<Long> reserved) {
        boolean fixedTaken = fixedAssigned.contains(spaceId) && !released.contains(spaceId);
        return !fixedTaken && !approved.contains(spaceId) && !reserved.contains(spaceId);
    }

    /**
     * Indica si la plaza esta ocupada para una fecha concreta segun la regla consolidada
     * de ocupacion (plaza, fecha): tiene una asignacion fija activa para el dia de la
     * semana de la fecha y esa fecha NO esta liberada, o tiene una solicitud {@code APPROVED}
     * para esa fecha, o tiene una reserva de visitante para esa fecha.
     *
     * <p>Es la unica definicion de ocupacion (plaza, fecha) del dominio: la reutiliza
     * {@code RequestService#approve} (via esta llamada) y la aplica identica
     * {@code VisitorReservationService#create}, evitando que la regla diverja entre modulos
     * (issue #43). El estado activo de la plaza es ortogonal y lo valida cada llamante por
     * separado. Comprueba por clave ({@code exists}), sin cargar por rango, porque opera
     * sobre una unica (plaza, fecha).</p>
     *
     * @param spaceId identificador de la plaza (se asume existente; el llamante lo verifica)
     * @param date    fecha a evaluar
     * @return {@code true} si la plaza esta ocupada esa fecha
     */
    @Transactional(readOnly = true)
    public boolean isSpaceTakenForDate(Long spaceId, LocalDate date) {
        return isSpaceTakenForDate(spaceId, ResourceType.PARKING, date);
    }

    /**
     * Indica si un recurso de un tipo concreto ({@code PARKING} plaza / {@code DESK} puesto)
     * esta ocupado para una fecha segun la regla consolidada de ocupacion.
     *
     * <p>Es la misma regla que {@link #isSpaceTakenForDate(Long, java.time.LocalDate)}
     * parametrizada por tipo: tiene asignacion fija activa ese dia y no liberada esa fecha, o
     * una solicitud {@code APPROVED} esa fecha, o (solo {@code PARKING}) una reserva de
     * visitante esa fecha. Para {@code DESK} el termino de reserva de visitante se omite (no
     * hay reservas de visitante en puestos, design §Decisions).</p>
     *
     * @param resourceId   identificador del recurso (se asume existente; el llamante lo verifica)
     * @param resourceType tipo del recurso ({@code PARKING}/{@code DESK})
     * @param date         fecha a evaluar
     * @return {@code true} si el recurso esta ocupado esa fecha
     */
    @Transactional(readOnly = true)
    public boolean isSpaceTakenForDate(Long spaceId, ResourceType resourceType, LocalDate date) {
        int dayOfWeek = date.getDayOfWeek().getValue();
        boolean fixedTaken = fixedAssignmentRepository
                .existsByResourceIdAndResourceTypeAndDayOfWeekAndActiveTrue(
                        spaceId, resourceType, dayOfWeek)
                && !releaseRepository.existsByResourceIdAndResourceTypeAndReleaseDate(
                        spaceId, resourceType, date);
        boolean approvedTaken = requestRepository
                .existsByResourceIdAndResourceTypeAndRequestedDateAndStatus(
                        spaceId, resourceType, date, RequestStatus.APPROVED);
        boolean reservedTaken = visitorReservationRepository
                .existsByResourceTypeAndResourceIdAndReservationDate(resourceType, spaceId, date);
        return fixedTaken || approvedTaken || reservedTaken;
    }

    // -------------------------------------------------------------------------
    // Calendario semanal admin
    // -------------------------------------------------------------------------

    /**
     * Construye el calendario semanal completo de todas las plazas activas ({@code PARKING})
     * para el {@code ADMIN}, con el estado y el titular de cada celda (plaza, dia). Atajo
     * retrocompatible de {@link #adminCalendar(LocalDate, ResourceType)} con {@code PARKING}
     * (design §D4: el parametro es nuevo, el comportamiento por defecto no cambia).
     *
     * @param weekStartInput lunes de la semana solicitada (se normaliza al lunes de esa semana)
     * @return el calendario semanal admin de plazas
     */
    @Transactional(readOnly = true)
    public AdminWeeklyCalendarResponse adminCalendar(LocalDate weekStartInput) {
        return adminCalendar(weekStartInput, ResourceType.PARKING);
    }

    /**
     * Construye el calendario semanal completo de todos los recursos activos de un tipo
     * ({@code PARKING} plazas / {@code DESK} puestos) para el {@code ADMIN}, con el estado y el
     * titular de cada celda (recurso, dia); change {@code restructure-admin-workflows}, design
     * §D4 (generalizacion PARKING/DESK por extension del contrato existente).
     *
     * @param weekStartInput lunes de la semana solicitada (se normaliza al lunes de esa semana)
     * @param resourceType   tipo de recurso a mostrar ({@code PARKING}/{@code DESK})
     * @return el calendario semanal admin del tipo de recurso solicitado
     */
    @Transactional(readOnly = true)
    public AdminWeeklyCalendarResponse adminCalendar(LocalDate weekStartInput, ResourceType resourceType) {
        LocalDate weekStart = mondayOf(weekStartInput);
        LocalDate weekEnd = weekStart.plusDays(WEEK_DAYS - 1L);
        List<LocalDate> days = weekDays(weekStart);

        List<BookableResource> resources = activeBookableResources(resourceType);
        List<Long> resourceIds = resources.stream().map(BookableResource::getResourceId).toList();

        List<FixedAssignmentEntity> fixed = activeFixed(resourceIds, resourceType);
        Map<SpaceDow, FixedAssignmentEntity> fixedBySpaceDow = fixed.stream()
                .collect(Collectors.toMap(
                        fa -> new SpaceDow(fa.getResourceId(), fa.getDayOfWeek()),
                        Function.identity(), (a, b) -> a));
        Set<SpaceDate> releasedKeys = releaseRepository
                .findByResourceTypeAndReleaseDateBetween(resourceType, weekStart, weekEnd).stream()
                .map(r -> new SpaceDate(r.getResourceId(), r.getReleaseDate()))
                .collect(Collectors.toSet());
        Map<SpaceDate, RequestEntity> approvedBySpaceDate = requestRepository
                .findByStatusAndResourceTypeAndRequestedDateBetween(
                        RequestStatus.APPROVED, resourceType, weekStart, weekEnd).stream()
                .collect(Collectors.toMap(
                        r -> new SpaceDate(r.getResourceId(), r.getRequestedDate()),
                        Function.identity(), (a, b) -> a));
        Map<Long, String> names = employeeNames(fixed, approvedBySpaceDate.values());

        // Reservas de visitante de la semana (por tipo): ocupan (recurso, fecha) con el
        // visitante como ocupante (estado VISITOR_RESERVATION), reflejadas en la rejilla.
        List<VisitorReservation> visitorWeek = visitorReservationRepository
                .findByResourceTypeAndReservationDateBetween(resourceType, weekStart, weekEnd);
        Map<SpaceDate, VisitorReservation> visitorBySpaceDate = visitorWeek.stream()
                .collect(Collectors.toMap(
                        r -> new SpaceDate(r.getResourceId(), r.getReservationDate()),
                        Function.identity(), (a, b) -> a));
        Map<Long, String> visitorNames = visitorNames(visitorWeek);

        List<CalendarRowResponse> rows = resources.stream()
                .map(resource -> row(resource, days, fixedBySpaceDow, releasedKeys, approvedBySpaceDate,
                        names, visitorBySpaceDate, visitorNames))
                .toList();
        return new AdminWeeklyCalendarResponse(weekStart, days, rows);
    }

    private CalendarRowResponse row(
            BookableResource resource, List<LocalDate> days, Map<SpaceDow, FixedAssignmentEntity> fixedBySpaceDow,
            Set<SpaceDate> releasedKeys, Map<SpaceDate, RequestEntity> approvedBySpaceDate,
            Map<Long, String> names, Map<SpaceDate, VisitorReservation> visitorBySpaceDate,
            Map<Long, String> visitorNames) {
        Long resourceId = resource.getResourceId();
        List<CalendarCellResponse> cells = days.stream()
                .map(day -> cell(resourceId, day, fixedBySpaceDow, releasedKeys, approvedBySpaceDate,
                        names, visitorBySpaceDate, visitorNames))
                .toList();
        return new CalendarRowResponse(resourceId, resource.getLabel(), cells);
    }

    private CalendarCellResponse cell(
            Long spaceId, LocalDate day, Map<SpaceDow, FixedAssignmentEntity> fixedBySpaceDow,
            Set<SpaceDate> releasedKeys, Map<SpaceDate, RequestEntity> approvedBySpaceDate,
            Map<Long, String> names, Map<SpaceDate, VisitorReservation> visitorBySpaceDate,
            Map<Long, String> visitorNames) {
        SpaceDate spaceDate = new SpaceDate(spaceId, day);
        RequestEntity approved = approvedBySpaceDate.get(spaceDate);
        if (approved != null) {
            return new CalendarCellResponse(day, CalendarCellState.REQUEST_APPROVED,
                    approved.getEmployeeId(), names.get(approved.getEmployeeId()), approved.getId());
        }
        FixedAssignmentEntity assignment = fixedBySpaceDow.get(new SpaceDow(spaceId, day.getDayOfWeek().getValue()));
        if (assignment != null) {
            CalendarCellState state = releasedKeys.contains(spaceDate)
                    ? CalendarCellState.RELEASED : CalendarCellState.ASSIGNED;
            return new CalendarCellResponse(day, state, assignment.getEmployeeId(),
                    names.get(assignment.getEmployeeId()), null);
        }
        VisitorReservation reservation = visitorBySpaceDate.get(spaceDate);
        if (reservation != null) {
            // Ocupante visitante: employeeId null; el nombre viaja en employeeName (ocupante).
            return new CalendarCellResponse(day, CalendarCellState.VISITOR_RESERVATION,
                    null, visitorNames.get(reservation.getVisitorId()), null);
        }
        return new CalendarCellResponse(day, CalendarCellState.FREE, null, null, null);
    }

    // -------------------------------------------------------------------------
    // Ocupacion por fecha (Liberar por fecha)
    // -------------------------------------------------------------------------

    /**
     * Construye la ocupacion de una fecha para el {@code ADMIN}: los recursos (plazas y puestos)
     * OCUPADOS esa fecha, cada uno con su titular y el origen de la ocupacion. Solo devuelve los
     * ocupados (los libres se omiten).
     *
     * <p>Aplica, generalizada a ambos {@link ResourceType}, la misma regla de precedencia que
     * {@link #cell} del calendario admin: una solicitud {@code APPROVED} para la fecha prevalece
     * sobre la asignacion fija; en ausencia de aquella, la asignacion fija vigente ese dia de la
     * semana ocupa el recurso salvo que exista una liberacion para la fecha. La reserva de
     * visitante no entra en esta vista (no es un recurso liberable administrativamente). Carga por
     * rango (una consulta por entidad y tipo) y resuelve los numeros humanos en lote, sin N+1.</p>
     *
     * @param date fecha a consultar
     * @return la ocupacion de la fecha (recursos ocupados, posiblemente vacia)
     */
    @Transactional(readOnly = true)
    public OccupancyResponse occupancyForDate(LocalDate date) {
        List<OccupancyItemResponse> items = Stream
                .concat(occupancyForType(date, ResourceType.PARKING).stream(),
                        occupancyForType(date, ResourceType.DESK).stream())
                .toList();
        return new OccupancyResponse(date, items);
    }

    private List<OccupancyItemResponse> occupancyForType(LocalDate date, ResourceType resourceType) {
        List<BookableResource> resources = activeBookableResources(resourceType);
        if (resources.isEmpty()) {
            return List.of();
        }
        List<Long> resourceIds = resources.stream().map(BookableResource::getResourceId).toList();
        int dow = date.getDayOfWeek().getValue();

        List<FixedAssignmentEntity> fixedForDay = activeFixed(resourceIds, resourceType).stream()
                .filter(fa -> fa.getDayOfWeek() == dow)
                .toList();
        Map<Long, FixedAssignmentEntity> fixedByResource = fixedForDay.stream()
                .collect(Collectors.toMap(
                        FixedAssignmentEntity::getResourceId, Function.identity(), (a, b) -> a));
        Set<Long> released = spaceIds(
                releaseRepository.findByResourceTypeAndReleaseDateBetween(resourceType, date, date),
                ReleaseEntity::getResourceId);
        List<RequestEntity> approved = requestRepository
                .findByStatusAndResourceTypeAndRequestedDateBetween(
                        RequestStatus.APPROVED, resourceType, date, date);
        Map<Long, RequestEntity> approvedByResource = approved.stream()
                .collect(Collectors.toMap(
                        RequestEntity::getResourceId, Function.identity(), (a, b) -> a));
        Map<Long, String> names = employeeNames(fixedForDay, approved);

        // Reservas de visitante que ocupan un recurso del tipo esa fecha, con el nombre del
        // visitante como ocupante (origen VISITOR_RESERVATION).
        List<VisitorReservation> visitorReservations = visitorReservationRepository
                .findByResourceTypeAndReservationDateBetween(resourceType, date, date);
        Map<Long, VisitorReservation> visitorByResource = visitorReservations.stream()
                .collect(Collectors.toMap(
                        VisitorReservation::getResourceId, Function.identity(), (a, b) -> a));
        Map<Long, String> visitorNames = visitorNames(visitorReservations);

        return resources.stream()
                .map(resource -> occupancyItem(
                        resource, fixedByResource, released, approvedByResource, names,
                        visitorByResource, visitorNames))
                .filter(Objects::nonNull)
                .toList();
    }

    private OccupancyItemResponse occupancyItem(
            BookableResource resource, Map<Long, FixedAssignmentEntity> fixedByResource,
            Set<Long> released, Map<Long, RequestEntity> approvedByResource, Map<Long, String> names,
            Map<Long, VisitorReservation> visitorByResource, Map<Long, String> visitorNames) {
        Long resourceId = resource.getResourceId();
        RequestEntity approved = approvedByResource.get(resourceId);
        if (approved != null) {
            return occupancyItemOf(resource, OccupancyOrigin.REQUEST_APPROVED,
                    approved.getEmployeeId(), names, approved.getId());
        }
        FixedAssignmentEntity assignment = fixedByResource.get(resourceId);
        if (assignment != null && !released.contains(resourceId)) {
            return occupancyItemOf(resource, OccupancyOrigin.FIXED_ASSIGNMENT,
                    assignment.getEmployeeId(), names, null);
        }
        VisitorReservation reservation = visitorByResource.get(resourceId);
        if (reservation != null) {
            // Ocupante = visitante (no un empleado): employeeId null, el nombre viaja en
            // employeeName (nombre de ocupante mostrado por el frontend).
            return new OccupancyItemResponse(
                    resource.getResourceType(), resource.getResourceId(), resource.getNumber(),
                    resource.getFloor(), null, visitorNames.get(reservation.getVisitorId()),
                    OccupancyOrigin.VISITOR_RESERVATION, null);
        }
        return null;
    }

    private Map<Long, String> visitorNames(List<VisitorReservation> reservations) {
        Set<Long> ids = reservations.stream()
                .map(VisitorReservation::getVisitorId)
                .collect(Collectors.toCollection(HashSet::new));
        if (ids.isEmpty()) {
            return Map.of();
        }
        return visitorRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(
                        Visitor::getId, v -> (v.getFirstName() + " " + v.getLastName()).trim()));
    }

    private static OccupancyItemResponse occupancyItemOf(
            BookableResource resource, OccupancyOrigin origin, Long employeeId,
            Map<Long, String> names, Long requestId) {
        return new OccupancyItemResponse(
                resource.getResourceType(), resource.getResourceId(), resource.getNumber(),
                resource.getFloor(), employeeId, names.get(employeeId), origin, requestId);
    }

    private List<BookableResource> activeBookableResources(ResourceType resourceType) {
        List<? extends BookableResource> resources = resourceType == ResourceType.DESK
                ? deskRepository.findByActiveTrueOrderByNumberAsc()
                : parkingSpaceRepository.findByActiveTrueOrderByIdAsc();
        return List.copyOf(resources);
    }

    // -------------------------------------------------------------------------
    // Ocupacion de un empleado por semana (Liberar por empleado/semana)
    // -------------------------------------------------------------------------

    /**
     * Construye la ocupacion de un empleado para una semana ({@code ADMIN}/{@code AGENCIA}): por
     * cada uno de los siete dias, las reservas del empleado en plaza y puesto, cada una con su
     * fecha (la del dia), tipo de recurso, recurso (numero/planta), origen
     * ({@link OccupancyOrigin#FIXED_ASSIGNMENT} o {@link OccupancyOrigin#REQUEST_APPROVED}) y el
     * {@code requestId} cuando proviene de una solicitud aprobada.
     *
     * <p>Generaliza a "por empleado y semana" la misma regla de precedencia que
     * {@link #occupancyForType} usa "por fecha": una solicitud {@code APPROVED} para la fecha
     * prevalece sobre la asignacion fija; en su ausencia, la asignacion fija vigente ese dia de la
     * semana ocupa el recurso salvo que exista una liberacion para la fecha. La reserva de
     * visitante no entra (no es un recurso con asignacion fija liberable administrativamente).
     * Carga por rango (una consulta por entidad: asignaciones fijas, solicitudes y liberaciones del
     * empleado, mas los recursos referenciados en lote) y ensambla en memoria, sin N+1.</p>
     *
     * @param employeeId     empleado a consultar
     * @param weekStartInput lunes de la semana solicitada (se normaliza al lunes de esa semana)
     * @return la ocupacion semanal del empleado (siempre los siete dias, con listas posiblemente vacias)
     * @throws EntityNotFoundException si el empleado no existe
     */
    @Transactional(readOnly = true)
    public EmployeeWeekOccupancyResponse employeeWeekOccupancy(Long employeeId, LocalDate weekStartInput) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EntityNotFoundException(MSG_EMPLOYEE_NOT_FOUND + employeeId));
        LocalDate weekStart = mondayOf(weekStartInput);
        LocalDate weekEnd = weekStart.plusDays(WEEK_DAYS - 1L);

        List<FixedAssignmentEntity> fixed = fixedAssignmentRepository
                .findByEmployeeIdAndActiveTrueOrderByDayOfWeekAsc(employeeId);
        List<RequestEntity> approved = requestRepository
                .findByEmployeeIdAndRequestedDateBetween(employeeId, weekStart, weekEnd).stream()
                .filter(r -> r.getStatus() == RequestStatus.APPROVED)
                .toList();
        List<ReleaseEntity> releases = releaseRepository
                .findByEmployeeIdAndReleaseDateBetween(employeeId, weekStart, weekEnd);

        WeekOccupancyContext context = new WeekOccupancyContext(
                employeeId, fullName(employee),
                fixed.stream().collect(Collectors.toMap(
                        fa -> new TypeDow(fa.getResourceType(), fa.getDayOfWeek()),
                        Function.identity(), (a, b) -> a)),
                approved.stream().collect(Collectors.toMap(
                        r -> new TypeDate(r.getResourceType(), r.getRequestedDate()),
                        Function.identity(), (a, b) -> a)),
                releases.stream()
                        .map(r -> new TypeResourceDate(r.getResourceType(), r.getResourceId(), r.getReleaseDate()))
                        .collect(Collectors.toSet()),
                resolveResources(fixed, approved));

        List<EmployeeWeekDayResponse> days = weekDays(weekStart).stream()
                .map(day -> new EmployeeWeekDayResponse(day, dayReservations(day, context)))
                .toList();
        return new EmployeeWeekOccupancyResponse(
                employeeId, context.employeeName(), weekStart, days);
    }

    private List<OccupancyItemResponse> dayReservations(LocalDate day, WeekOccupancyContext context) {
        return Stream.of(ResourceType.PARKING, ResourceType.DESK)
                .map(type -> reservationFor(type, day, context))
                .filter(Objects::nonNull)
                .toList();
    }

    private OccupancyItemResponse reservationFor(
            ResourceType type, LocalDate day, WeekOccupancyContext context) {
        RequestEntity approved = context.approvedByTypeDate().get(new TypeDate(type, day));
        if (approved != null) {
            return reservationItem(
                    type, approved.getResourceId(), OccupancyOrigin.REQUEST_APPROVED, approved.getId(), context);
        }
        FixedAssignmentEntity assignment = context.fixedByTypeDow()
                .get(new TypeDow(type, day.getDayOfWeek().getValue()));
        if (assignment != null
                && !context.releasedKeys().contains(new TypeResourceDate(type, assignment.getResourceId(), day))) {
            return reservationItem(
                    type, assignment.getResourceId(), OccupancyOrigin.FIXED_ASSIGNMENT, null, context);
        }
        return null;
    }

    private static OccupancyItemResponse reservationItem(
            ResourceType type, Long resourceId, OccupancyOrigin origin, Long requestId,
            WeekOccupancyContext context) {
        BookableResource resource = context.resourcesByType()
                .getOrDefault(type, Map.of()).get(resourceId);
        Integer number = resource != null ? resource.getNumber() : null;
        Integer floor = resource != null ? resource.getFloor() : null;
        return new OccupancyItemResponse(
                type, resourceId, number, floor,
                context.employeeId(), context.employeeName(), origin, requestId);
    }

    private Map<ResourceType, Map<Long, BookableResource>> resolveResources(
            List<FixedAssignmentEntity> fixed, List<RequestEntity> approved) {
        Set<Long> parkingIds = new HashSet<>();
        Set<Long> deskIds = new HashSet<>();
        for (FixedAssignmentEntity fa : fixed) {
            (fa.getResourceType() == ResourceType.DESK ? deskIds : parkingIds).add(fa.getResourceId());
        }
        for (RequestEntity request : approved) {
            (request.getResourceType() == ResourceType.DESK ? deskIds : parkingIds).add(request.getResourceId());
        }
        Map<ResourceType, Map<Long, BookableResource>> result = new EnumMap<>(ResourceType.class);
        result.put(ResourceType.PARKING, bookableById(parkingSpaceRepository.findAllById(parkingIds)));
        result.put(ResourceType.DESK, bookableById(deskRepository.findAllById(deskIds)));
        return result;
    }

    private static Map<Long, BookableResource> bookableById(List<? extends BookableResource> resources) {
        return resources.stream()
                .collect(Collectors.toMap(BookableResource::getResourceId, resource -> resource));
    }

    // -------------------------------------------------------------------------
    // Mi Semana
    // -------------------------------------------------------------------------

    /**
     * Contexto por tipo de recurso de "Mi Semana": los indices en memoria (asignacion fija por dia
     * de la semana, solicitudes aprobadas/pendientes por fecha, liberaciones y etiquetas de
     * recurso) cargados por rango para un {@link ResourceType} del solicitante, base del estado
     * diario de ese tipo (change {@code restructure-admin-workflows}, design §D4: "Mi Semana"
     * multi-recurso).
     */
    private record MyWeekTypeContext(
            Map<Integer, FixedAssignmentEntity> fixedByDow,
            Map<LocalDate, RequestEntity> approvedByDate,
            Map<LocalDate, RequestEntity> pendingByDate,
            Set<SpaceDate> releasedKeys,
            Map<Long, String> labels) {
    }

    /**
     * Estado diario de un unico tipo de recurso en "Mi Semana": estado, etiqueta del recurso,
     * estado/id de la solicitud propia que lo origina (si aplica) y si esa solicitud esta en
     * lista de espera (change {@code waitlist-requests}; solo relevante cuando {@code state} es
     * {@link MyWeekDayState#REQUEST_PENDING}).
     */
    private record MyWeekResourceState(
            MyWeekDayState state, String label, RequestStatus requestStatus, Long requestId,
            boolean waitlisted) {
    }

    /**
     * Construye la vista personal de la semana del solicitante: el estado diario referido
     * unicamente a sus recursos propios (plaza Y puesto, en paralelo), sin exponer identidad de
     * terceros (change {@code restructure-admin-workflows}, design §D4).
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

        // Se cargan por separado por tipo (una consulta por entidad y tipo, sin N+1) para que un
        // recurso DESK del empleado con el mismo resource_id que una plaza no contamine la vista.
        MyWeekTypeContext parkingContext =
                loadMyWeekTypeContext(employeeId, ResourceType.PARKING, weekStart, weekEnd);
        MyWeekTypeContext deskContext =
                loadMyWeekTypeContext(employeeId, ResourceType.DESK, weekStart, weekEnd);

        List<MyWeekDayResponse> dayViews = days.stream()
                .map(day -> myWeekDay(day, parkingContext, deskContext))
                .toList();
        return new MyWeekResponse(weekStart, dayViews);
    }

    private MyWeekTypeContext loadMyWeekTypeContext(
            Long employeeId, ResourceType resourceType, LocalDate weekStart, LocalDate weekEnd) {
        List<FixedAssignmentEntity> myFixed = fixedAssignmentRepository
                .findByEmployeeIdAndResourceTypeAndActiveTrueOrderByDayOfWeekAsc(employeeId, resourceType);
        List<RequestEntity> myRequests = requestRepository
                .findByEmployeeIdAndResourceTypeAndRequestedDateBetween(
                        employeeId, resourceType, weekStart, weekEnd);
        List<ReleaseEntity> myReleases = releaseRepository
                .findByEmployeeIdAndResourceTypeAndReleaseDateBetween(
                        employeeId, resourceType, weekStart, weekEnd);

        Map<Integer, FixedAssignmentEntity> fixedByDow = myFixed.stream()
                .collect(Collectors.toMap(FixedAssignmentEntity::getDayOfWeek, Function.identity(), (a, b) -> a));
        Map<LocalDate, RequestEntity> approvedByDate = requestsByDate(myRequests, RequestStatus.APPROVED);
        Map<LocalDate, RequestEntity> pendingByDate = requestsByDate(myRequests, RequestStatus.PENDING);
        Set<SpaceDate> releasedKeys = myReleases.stream()
                .map(r -> new SpaceDate(r.getResourceId(), r.getReleaseDate()))
                .collect(Collectors.toSet());
        Map<Long, String> labels = resourceLabels(fixedByDow, approvedByDate, resourceType);
        return new MyWeekTypeContext(fixedByDow, approvedByDate, pendingByDate, releasedKeys, labels);
    }

    private MyWeekDayResponse myWeekDay(LocalDate day, MyWeekTypeContext parking, MyWeekTypeContext desk) {
        MyWeekResourceState parkingState = myWeekResourceState(day, parking);
        MyWeekResourceState deskState = myWeekResourceState(day, desk);
        return new MyWeekDayResponse(
                day, parkingState.state(), parkingState.label(),
                parkingState.requestStatus(), parkingState.requestId(),
                deskState.state(), deskState.label(),
                deskState.requestStatus(), deskState.requestId(),
                parkingState.waitlisted(), deskState.waitlisted());
    }

    private MyWeekResourceState myWeekResourceState(LocalDate day, MyWeekTypeContext context) {
        RequestEntity approved = context.approvedByDate().get(day);
        if (approved != null) {
            return new MyWeekResourceState(MyWeekDayState.ASSIGNED,
                    context.labels().get(approved.getResourceId()), RequestStatus.APPROVED,
                    approved.getId(), false);
        }
        FixedAssignmentEntity assignment = context.fixedByDow().get(day.getDayOfWeek().getValue());
        if (assignment != null) {
            boolean released = context.releasedKeys().contains(new SpaceDate(assignment.getResourceId(), day));
            MyWeekDayState state = released ? MyWeekDayState.RELEASED : MyWeekDayState.ASSIGNED;
            return new MyWeekResourceState(
                    state, context.labels().get(assignment.getResourceId()), null, null, false);
        }
        RequestEntity pending = context.pendingByDate().get(day);
        if (pending != null) {
            return new MyWeekResourceState(
                    MyWeekDayState.REQUEST_PENDING, null, RequestStatus.PENDING, pending.getId(),
                    pending.isWaitlisted());
        }
        return new MyWeekResourceState(MyWeekDayState.FREE, null, null, null, false);
    }

    // -------------------------------------------------------------------------
    // Helpers de carga y utilidades
    // -------------------------------------------------------------------------

    private Set<Long> activeFixedResourceIdsForDay(
            List<Long> resourceIds, ResourceType resourceType, int dow) {
        return activeFixed(resourceIds, resourceType).stream()
                .filter(fa -> fa.getDayOfWeek() == dow)
                .map(FixedAssignmentEntity::getResourceId)
                .collect(Collectors.toSet());
    }

    private List<FixedAssignmentEntity> activeFixed(List<Long> resourceIds, ResourceType resourceType) {
        if (resourceIds.isEmpty()) {
            return List.of();
        }
        return fixedAssignmentRepository.findByResourceIdInAndResourceTypeAndActiveTrue(
                resourceIds, resourceType);
    }

    private Map<Long, String> employeeNames(
            List<FixedAssignmentEntity> fixed, Collection<RequestEntity> approved) {
        Set<Long> ids = fixed.stream().map(FixedAssignmentEntity::getEmployeeId)
                .collect(Collectors.toCollection(HashSet::new));
        approved.forEach(r -> ids.add(r.getEmployeeId()));
        if (ids.isEmpty()) {
            return Map.of();
        }
        return employeeRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Employee::getId, AvailabilityService::fullName));
    }

    private Map<Long, String> resourceLabels(
            Map<Integer, FixedAssignmentEntity> fixedByDow, Map<LocalDate, RequestEntity> approvedByDate,
            ResourceType resourceType) {
        Set<Long> ids = fixedByDow.values().stream().map(FixedAssignmentEntity::getResourceId)
                .collect(Collectors.toCollection(HashSet::new));
        approvedByDate.values().forEach(r -> ids.add(r.getResourceId()));
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<? extends BookableResource> resources = resourceType == ResourceType.DESK
                ? deskRepository.findAllById(ids)
                : parkingSpaceRepository.findAllById(ids);
        return resources.stream()
                .collect(Collectors.toMap(BookableResource::getResourceId, BookableResource::getLabel));
    }

    private static Map<LocalDate, RequestEntity> requestsByDate(List<RequestEntity> requests, RequestStatus status) {
        return requests.stream()
                .filter(r -> r.getStatus() == status)
                .collect(Collectors.toMap(RequestEntity::getRequestedDate, Function.identity(), (a, b) -> a));
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
