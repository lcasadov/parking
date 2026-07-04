package com.aleatica.parking.availability.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.aleatica.parking.auth.domain.ClockPort;
import com.aleatica.parking.availability.CalendarCellState;
import com.aleatica.parking.availability.MyWeekDayState;
import com.aleatica.parking.availability.dto.AdminWeeklyCalendarResponse;
import com.aleatica.parking.availability.dto.AvailabilityResponse;
import com.aleatica.parking.availability.dto.CalendarCellResponse;
import com.aleatica.parking.availability.dto.MyWeekDayResponse;
import com.aleatica.parking.availability.dto.MyWeekResponse;
import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.employee.Role;
import com.aleatica.parking.fixedassignment.FixedAssignment;
import com.aleatica.parking.fixedassignment.FixedAssignmentRepository;
import com.aleatica.parking.parkingspace.ParkingSpace;
import com.aleatica.parking.parkingspace.ParkingSpaceRepository;
import com.aleatica.parking.release.Release;
import com.aleatica.parking.release.ReleaseRepository;
import com.aleatica.parking.request.Request;
import com.aleatica.parking.request.RequestRepository;
import com.aleatica.parking.request.RequestStatus;
import com.aleatica.parking.support.EmployeeTestFactory;
import com.aleatica.parking.visitor.VisitorReservation;
import com.aleatica.parking.visitor.VisitorReservationRepository;
import jakarta.persistence.EntityNotFoundException;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests unitarios de {@link AvailabilityService} con repositorios y {@code ClockPort}
 * mockeados: la tabla de verdad de disponibilidad (plaza libre, liberada, ocupada por
 * solicitud aprobada, ocupada por reserva de visitante, asignacion fija vigente sin
 * liberar), los estados de celda del calendario admin y los estados de "Mi Semana",
 * incluida la privacidad (sin identidad de terceros). No toca la base de datos.
 *
 * <p>La logica de disponibilidad replicada aqui es identica a la que aplican en linea
 * {@code RequestService#approve} y {@code VisitorReservationService#create} (mismo mapeo
 * {@code getDayOfWeek().getValue()}, 1=Lunes..7=Domingo).</p>
 */
@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-06T10:00:00Z");
    // 2026-07-10 es viernes -> dayOfWeek = 5
    private static final LocalDate DATE = LocalDate.of(2026, 7, 10);
    private static final int DATE_DOW = DATE.getDayOfWeek().getValue();
    private static final LocalDate MONDAY = LocalDate.of(2026, 7, 6);

    private static final String EMP_LOGIN = "empleado";
    private static final Long EMP_ID = 15L;
    private static final Long OTHER_ID = 99L;
    private static final Long SPACE_ID = 8L;
    private static final String SPACE_LABEL = "P-08";

    @Mock
    private ParkingSpaceRepository parkingSpaceRepository;
    @Mock
    private FixedAssignmentRepository fixedAssignmentRepository;
    @Mock
    private ReleaseRepository releaseRepository;
    @Mock
    private RequestRepository requestRepository;
    @Mock
    private VisitorReservationRepository visitorReservationRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private ClockPort clock;

    private AvailabilityService service() {
        return new AvailabilityService(parkingSpaceRepository, fixedAssignmentRepository,
                releaseRepository, requestRepository, visitorReservationRepository, employeeRepository,
                clock);
    }

    // ---- Tabla de verdad de disponibilidad puntual ----

    @Test
    void shouldIncludeSpace_whenFreeForDate() {
        // Arrange: plaza activa sin asignacion, solicitud ni reserva
        givenActiveSpaces(space(SPACE_ID, SPACE_LABEL));
        givenNoFixedAssignments();
        givenReleasesOnDate();
        givenApprovedRequestsOnDate();
        givenReservationsOnDate();

        // Act
        AvailabilityResponse response = service().availabilityForDate(DATE);

        // Assert
        assertThat(response.date()).isEqualTo(DATE);
        assertThat(response.availableResources()).singleElement()
                .satisfies(item -> {
                    assertThat(item.parkingSpaceId()).isEqualTo(SPACE_ID);
                    assertThat(item.label()).isEqualTo(SPACE_LABEL);
                });
    }

    @Test
    void shouldIncludeSpace_whenFixedAssignmentReleasedForDate() {
        // Arrange: asignacion fija ese dia PERO liberada para DATE -> disponible
        givenActiveSpaces(space(SPACE_ID, SPACE_LABEL));
        givenFixedAssignments(assignment(SPACE_ID, EMP_ID, DATE_DOW));
        givenReleasesOnDate(release(SPACE_ID, DATE));
        givenApprovedRequestsOnDate();
        givenReservationsOnDate();

        // Act
        AvailabilityResponse response = service().availabilityForDate(DATE);

        // Assert
        assertThat(response.availableResources()).hasSize(1);
    }

    @Test
    void shouldExcludeSpace_whenRequestApprovedForDate() {
        // Arrange: solicitud APPROVED sobre la plaza para DATE
        givenActiveSpaces(space(SPACE_ID, SPACE_LABEL));
        givenNoFixedAssignments();
        givenReleasesOnDate();
        givenApprovedRequestsOnDate(approvedRequest(SPACE_ID, EMP_ID, DATE));
        givenReservationsOnDate();

        // Act
        AvailabilityResponse response = service().availabilityForDate(DATE);

        // Assert
        assertThat(response.availableResources()).isEmpty();
    }

    @Test
    void shouldExcludeSpace_whenVisitorReservationForDate() {
        // Arrange: reserva de visitante sobre la plaza para DATE
        givenActiveSpaces(space(SPACE_ID, SPACE_LABEL));
        givenNoFixedAssignments();
        givenReleasesOnDate();
        givenApprovedRequestsOnDate();
        givenReservationsOnDate(reservation(SPACE_ID, DATE));

        // Act
        AvailabilityResponse response = service().availabilityForDate(DATE);

        // Assert
        assertThat(response.availableResources()).isEmpty();
    }

    @Test
    void shouldExcludeSpace_whenInactive() {
        // Arrange: el repositorio de plazas activas no devuelve la plaza inactiva
        given(parkingSpaceRepository.findByActiveTrueOrderByIdAsc()).willReturn(List.of());

        // Act
        AvailabilityResponse response = service().availabilityForDate(DATE);

        // Assert: nunca aparece una plaza inactiva
        assertThat(response.availableResources()).isEmpty();
    }

    @Test
    void shouldExcludeSpace_whenFixedAssignmentActiveWithoutRelease() {
        // Arrange: asignacion fija vigente ese dia y NO liberada -> no disponible
        givenActiveSpaces(space(SPACE_ID, SPACE_LABEL));
        givenFixedAssignments(assignment(SPACE_ID, EMP_ID, DATE_DOW));
        givenReleasesOnDate();
        givenApprovedRequestsOnDate();
        givenReservationsOnDate();

        // Act
        AvailabilityResponse response = service().availabilityForDate(DATE);

        // Assert
        assertThat(response.availableResources()).isEmpty();
    }

    @Test
    void shouldRemainUnavailable_whenReleasedButApprovedRequestExists() {
        // Arrange (edge): liberada pero cubierta por una solicitud aprobada -> prevalece APPROVED
        givenActiveSpaces(space(SPACE_ID, SPACE_LABEL));
        givenFixedAssignments(assignment(SPACE_ID, EMP_ID, DATE_DOW));
        givenReleasesOnDate(release(SPACE_ID, DATE));
        givenApprovedRequestsOnDate(approvedRequest(SPACE_ID, OTHER_ID, DATE));
        givenReservationsOnDate();

        // Act
        AvailabilityResponse response = service().availabilityForDate(DATE);

        // Assert
        assertThat(response.availableResources()).isEmpty();
    }

    // ---- Regla consolidada de ocupacion (issue #43): isSpaceTakenForDate ----

    @Test
    void shouldReportTaken_whenVisitorReservationForDate() {
        // Arrange (integridad #43): una reserva de visitante ocupa la plaza esa fecha
        given(fixedAssignmentRepository
                .existsByParkingSpaceIdAndDayOfWeekAndActiveTrue(SPACE_ID, DATE_DOW)).willReturn(false);
        given(requestRepository.existsByParkingSpaceIdAndRequestedDateAndStatus(
                SPACE_ID, DATE, RequestStatus.APPROVED)).willReturn(false);
        given(visitorReservationRepository.existsByParkingSpaceIdAndReservationDate(SPACE_ID, DATE))
                .willReturn(true);

        // Act / Assert
        assertThat(service().isSpaceTakenForDate(SPACE_ID, DATE)).isTrue();
    }

    @Test
    void shouldReportNotTaken_whenFixedAssignmentReleasedForDate() {
        // Arrange (#43): asignacion fija ese dia PERO liberada para DATE -> no ocupada
        given(fixedAssignmentRepository
                .existsByParkingSpaceIdAndDayOfWeekAndActiveTrue(SPACE_ID, DATE_DOW)).willReturn(true);
        given(releaseRepository.existsByParkingSpaceIdAndReleaseDate(SPACE_ID, DATE)).willReturn(true);
        given(requestRepository.existsByParkingSpaceIdAndRequestedDateAndStatus(
                SPACE_ID, DATE, RequestStatus.APPROVED)).willReturn(false);
        given(visitorReservationRepository.existsByParkingSpaceIdAndReservationDate(SPACE_ID, DATE))
                .willReturn(false);

        // Act / Assert
        assertThat(service().isSpaceTakenForDate(SPACE_ID, DATE)).isFalse();
    }

    @Test
    void shouldReportTaken_whenFixedAssignmentActiveWithoutRelease() {
        // Arrange: asignacion fija vigente ese dia y NO liberada -> ocupada
        given(fixedAssignmentRepository
                .existsByParkingSpaceIdAndDayOfWeekAndActiveTrue(SPACE_ID, DATE_DOW)).willReturn(true);
        given(releaseRepository.existsByParkingSpaceIdAndReleaseDate(SPACE_ID, DATE)).willReturn(false);

        // Act / Assert (corto-circuito: fixedTaken ya es true)
        assertThat(service().isSpaceTakenForDate(SPACE_ID, DATE)).isTrue();
    }

    @Test
    void shouldReportTaken_whenApprovedRequestForDate() {
        // Arrange: solicitud APPROVED sobre la plaza esa fecha -> ocupada
        given(fixedAssignmentRepository
                .existsByParkingSpaceIdAndDayOfWeekAndActiveTrue(SPACE_ID, DATE_DOW)).willReturn(false);
        given(requestRepository.existsByParkingSpaceIdAndRequestedDateAndStatus(
                SPACE_ID, DATE, RequestStatus.APPROVED)).willReturn(true);

        // Act / Assert
        assertThat(service().isSpaceTakenForDate(SPACE_ID, DATE)).isTrue();
    }

    @Test
    void shouldReportNotTaken_whenFreeForDate() {
        // Arrange: sin asignacion, sin solicitud aprobada, sin reserva -> libre
        given(fixedAssignmentRepository
                .existsByParkingSpaceIdAndDayOfWeekAndActiveTrue(SPACE_ID, DATE_DOW)).willReturn(false);
        given(requestRepository.existsByParkingSpaceIdAndRequestedDateAndStatus(
                SPACE_ID, DATE, RequestStatus.APPROVED)).willReturn(false);
        given(visitorReservationRepository.existsByParkingSpaceIdAndReservationDate(SPACE_ID, DATE))
                .willReturn(false);

        // Act / Assert
        assertThat(service().isSpaceTakenForDate(SPACE_ID, DATE)).isFalse();
    }

    // ---- Calendario admin: estados de celda ----

    @Test
    void shouldReturnAdminCalendar_withCellStatesPerDay() {
        // Arrange: una plaza; asignada el lunes (dow=1), liberada el martes, aprobada el miercoles
        LocalDate tuesday = MONDAY.plusDays(1);
        LocalDate wednesday = MONDAY.plusDays(2);
        givenActiveSpaces(space(SPACE_ID, SPACE_LABEL));
        given(fixedAssignmentRepository.findByParkingSpaceIdInAndActiveTrue(anyCollection()))
                .willReturn(List.of(assignment(SPACE_ID, EMP_ID, 1), assignment(SPACE_ID, EMP_ID, 2)));
        given(releaseRepository.findByReleaseDateBetween(MONDAY, MONDAY.plusDays(6)))
                .willReturn(List.of(release(SPACE_ID, tuesday)));
        given(requestRepository.findByStatusAndRequestedDateBetween(
                RequestStatus.APPROVED, MONDAY, MONDAY.plusDays(6)))
                .willReturn(List.of(approvedRequestWithId(7L, SPACE_ID, OTHER_ID, wednesday)));
        given(employeeRepository.findAllById(any()))
                .willReturn(List.of(employee(EMP_ID, "Ada", "Lovelace"), employee(OTHER_ID, "Grace", "Hopper")));

        // Act
        AdminWeeklyCalendarResponse response = service().adminCalendar(MONDAY);

        // Assert: 7 dias, una fila, estados por dia
        assertThat(response.weekStart()).isEqualTo(MONDAY);
        assertThat(response.days()).hasSize(7);
        List<CalendarCellResponse> cells = response.rows().get(0).cells();
        assertThat(cells.get(0).state()).isEqualTo(CalendarCellState.ASSIGNED);
        assertThat(cells.get(0).employeeName()).isEqualTo("Ada Lovelace");
        assertThat(cells.get(1).state()).isEqualTo(CalendarCellState.RELEASED);
        assertThat(cells.get(2).state()).isEqualTo(CalendarCellState.REQUEST_APPROVED);
        assertThat(cells.get(2).requestId()).isEqualTo(7L);
        assertThat(cells.get(2).employeeName()).isEqualTo("Grace Hopper");
        assertThat(cells.get(3).state()).isEqualTo(CalendarCellState.FREE);
    }

    @Test
    void shouldNormalizeWeekStartToMonday_whenAdminCalendarWeekStartIsNotMonday() {
        // Arrange: weekStart en miercoles -> normaliza al lunes de esa semana
        LocalDate wednesday = MONDAY.plusDays(2);
        givenActiveSpaces(space(SPACE_ID, SPACE_LABEL));
        given(fixedAssignmentRepository.findByParkingSpaceIdInAndActiveTrue(anyCollection()))
                .willReturn(List.of());
        given(releaseRepository.findByReleaseDateBetween(MONDAY, MONDAY.plusDays(6))).willReturn(List.of());
        given(requestRepository.findByStatusAndRequestedDateBetween(
                RequestStatus.APPROVED, MONDAY, MONDAY.plusDays(6))).willReturn(List.of());

        // Act
        AdminWeeklyCalendarResponse response = service().adminCalendar(wednesday);

        // Assert
        assertThat(response.weekStart()).isEqualTo(MONDAY);
        assertThat(response.days().get(0)).isEqualTo(MONDAY);
    }

    // ---- Mi Semana ----

    @Test
    void shouldReturnOwnWeekWithoutOtherNames_whenEmployeeCallsMyWeek() {
        // Arrange: asignacion fija el lunes; liberada el martes; solicitud aprobada el jueves; pendiente el viernes
        LocalDate tuesday = MONDAY.plusDays(1);
        LocalDate thursday = MONDAY.plusDays(3);
        LocalDate friday = MONDAY.plusDays(4);
        givenActor();
        given(fixedAssignmentRepository.findByEmployeeIdAndActiveTrueOrderByDayOfWeekAsc(EMP_ID))
                .willReturn(List.of(assignment(SPACE_ID, EMP_ID, 1), assignment(SPACE_ID, EMP_ID, 2)));
        given(requestRepository.findByEmployeeIdAndRequestedDateBetween(EMP_ID, MONDAY, MONDAY.plusDays(6)))
                .willReturn(List.of(
                        approvedRequestWithId(3L, SPACE_ID, EMP_ID, thursday),
                        pendingRequest(EMP_ID, friday)));
        given(releaseRepository.findByEmployeeIdAndReleaseDateBetween(EMP_ID, MONDAY, MONDAY.plusDays(6)))
                .willReturn(List.of(release(SPACE_ID, tuesday)));
        given(parkingSpaceRepository.findAllById(any()))
                .willReturn(List.of(space(SPACE_ID, SPACE_LABEL)));

        // Act
        MyWeekResponse response = service().myWeek(EMP_LOGIN, MONDAY);

        // Assert: estados por dia y ausencia total de identidad de terceros
        List<MyWeekDayResponse> days = response.days();
        assertThat(days).hasSize(7);
        assertThat(days.get(0).state()).isEqualTo(MyWeekDayState.ASSIGNED);
        assertThat(days.get(0).parkingSpaceLabel()).isEqualTo(SPACE_LABEL);
        assertThat(days.get(1).state()).isEqualTo(MyWeekDayState.RELEASED);
        assertThat(days.get(2).state()).isEqualTo(MyWeekDayState.FREE);
        assertThat(days.get(3).state()).isEqualTo(MyWeekDayState.ASSIGNED);
        assertThat(days.get(3).requestStatus()).isEqualTo(RequestStatus.APPROVED);
        assertThat(days.get(4).state()).isEqualTo(MyWeekDayState.REQUEST_PENDING);
        assertThat(days.get(4).requestStatus()).isEqualTo(RequestStatus.PENDING);
        // MyWeekResponse no expone ningun campo de identidad por diseno (privacidad)
        assertThat(response).hasNoNullFieldsOrProperties();
    }

    @Test
    void shouldUseCurrentWeek_whenMyWeekWeekStartOmitted() {
        // Arrange: sin weekStart -> semana actual via ClockPort (NOW = lunes 2026-07-06)
        givenActor();
        given(clock.now()).willReturn(NOW);
        given(fixedAssignmentRepository.findByEmployeeIdAndActiveTrueOrderByDayOfWeekAsc(EMP_ID))
                .willReturn(List.of());
        given(requestRepository.findByEmployeeIdAndRequestedDateBetween(EMP_ID, MONDAY, MONDAY.plusDays(6)))
                .willReturn(List.of());
        given(releaseRepository.findByEmployeeIdAndReleaseDateBetween(EMP_ID, MONDAY, MONDAY.plusDays(6)))
                .willReturn(List.of());

        // Act
        MyWeekResponse response = service().myWeek(EMP_LOGIN, null);

        // Assert
        assertThat(response.weekStart()).isEqualTo(MONDAY);
        assertThat(response.days()).allSatisfy(day ->
                assertThat(day.state()).isEqualTo(MyWeekDayState.FREE));
    }

    @Test
    void shouldThrowNotFound_whenMyWeekActorUnknown() {
        // Arrange
        given(employeeRepository.findByLogin(EMP_LOGIN)).willReturn(Optional.empty());

        // Act / Assert
        assertThatThrownBy(() -> service().myWeek(EMP_LOGIN, MONDAY))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ---- Helpers de arreglo (given*) ----

    private void givenActiveSpaces(ParkingSpace... spaces) {
        given(parkingSpaceRepository.findByActiveTrueOrderByIdAsc()).willReturn(List.of(spaces));
    }

    private void givenNoFixedAssignments() {
        given(fixedAssignmentRepository.findByParkingSpaceIdInAndActiveTrue(anyCollection()))
                .willReturn(List.of());
    }

    private void givenFixedAssignments(FixedAssignment... assignments) {
        given(fixedAssignmentRepository.findByParkingSpaceIdInAndActiveTrue(anyCollection()))
                .willReturn(List.of(assignments));
    }

    private void givenReleasesOnDate(Release... releases) {
        given(releaseRepository.findByReleaseDateBetween(DATE, DATE)).willReturn(List.of(releases));
    }

    private void givenApprovedRequestsOnDate(Request... requests) {
        given(requestRepository.findByStatusAndRequestedDateBetween(RequestStatus.APPROVED, DATE, DATE))
                .willReturn(List.of(requests));
    }

    private void givenReservationsOnDate(VisitorReservation... reservations) {
        given(visitorReservationRepository.findByReservationDateBetween(DATE, DATE))
                .willReturn(List.of(reservations));
    }

    private void givenActor() {
        given(employeeRepository.findByLogin(EMP_LOGIN))
                .willReturn(Optional.of(employee(EMP_ID, "Test", "User")));
    }

    // ---- Fabricas de entidades ----

    private static ParkingSpace space(Long id, String label) {
        ParkingSpace space = ParkingSpace.create(label);
        setField(space, "id", id);
        return space;
    }

    private static FixedAssignment assignment(Long spaceId, Long employeeId, int dow) {
        return FixedAssignment.create(spaceId, employeeId, dow, 1L, NOW);
    }

    private static Release release(Long spaceId, LocalDate date) {
        return Release.voluntary(spaceId, EMP_ID, date, NOW);
    }

    private static Request approvedRequest(Long spaceId, Long employeeId, LocalDate date) {
        Request request = Request.create(employeeId, date, NOW);
        request.approve(spaceId, 1L, null, NOW);
        return request;
    }

    private static Request approvedRequestWithId(Long id, Long spaceId, Long employeeId, LocalDate date) {
        Request request = approvedRequest(spaceId, employeeId, date);
        setField(request, "id", id);
        return request;
    }

    private static Request pendingRequest(Long employeeId, LocalDate date) {
        return Request.create(employeeId, date, NOW);
    }

    private static VisitorReservation reservation(Long spaceId, LocalDate date) {
        return VisitorReservation.create(1L, spaceId, date, null, 1L, NOW);
    }

    private static Employee employee(Long id, String first, String last) {
        Employee employee = EmployeeTestFactory.active(id, "u" + id, "u" + id + "@x.com", null, Role.EMPLOYEE);
        EmployeeTestFactory.set(employee, "firstName", first);
        EmployeeTestFactory.set(employee, "lastName", last);
        return employee;
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("No se pudo fijar el campo " + name, ex);
        }
    }
}
