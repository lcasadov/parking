package com.aleatica.parking.floorplan.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.aleatica.parking.auth.domain.ClockPort;
import com.aleatica.parking.desk.Desk;
import com.aleatica.parking.desk.DeskCategory;
import com.aleatica.parking.desk.DeskRepository;
import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.fixedassignment.FixedAssignment;
import com.aleatica.parking.fixedassignment.FixedAssignmentRepository;
import com.aleatica.parking.floorplan.FloorPlanDeskState;
import com.aleatica.parking.floorplan.dto.FloorPlanDeskResponse;
import com.aleatica.parking.floorplan.dto.FloorPlanResponse;
import com.aleatica.parking.release.Release;
import com.aleatica.parking.release.ReleaseRepository;
import com.aleatica.parking.request.infrastructure.RequestEntity;
import com.aleatica.parking.request.infrastructure.RequestJpaRepository;
import com.aleatica.parking.request.domain.RequestStatus;
import com.aleatica.parking.request.application.OutsideRequestWindowException;
import com.aleatica.parking.resource.ResourceType;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests unitarios del caso de uso de lectura del plano ({@link FloorPlanQueryService}):
 * derivacion del estado por puesto/fecha (FREE / ASSIGNED / REQUESTED / MINE / RELEASED),
 * privacidad (MINE solo para el propio, sin revelar titulares ajenos) y ventana temporal. Los
 * repositorios se mockean pero las entidades son objetos reales (via sus factorias, con id por
 * reflexion), evitando el stubbing de entidades; el ensamblado por rango contra BD real lo
 * cubre {@code FloorPlanIT}.
 */
class FloorPlanQueryServiceTest {

    private static final String LOGIN = "empleado.uno";
    private static final long REQUESTER_ID = 7L;
    private static final long OTHER_ID = 99L;
    private static final long ADMIN_ID = 1L;
    private static final Instant NOW = Instant.parse("2026-07-04T00:00:00Z");
    private static final LocalDate TODAY = LocalDate.ofInstant(NOW, ZoneOffset.UTC);
    private static final LocalDate WITHIN = TODAY.plusDays(3);
    private static final int WITHIN_DOW = WITHIN.getDayOfWeek().getValue();

    private DeskRepository deskRepository;
    private FixedAssignmentRepository fixedAssignmentRepository;
    private ReleaseRepository releaseRepository;
    private RequestJpaRepository requestRepository;
    private EmployeeRepository employeeRepository;
    private ClockPort clock;

    private FloorPlanQueryService service;

    @BeforeEach
    void setUp() {
        deskRepository = mock(DeskRepository.class);
        fixedAssignmentRepository = mock(FixedAssignmentRepository.class);
        releaseRepository = mock(ReleaseRepository.class);
        requestRepository = mock(RequestJpaRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        clock = mock(ClockPort.class);
        given(clock.now()).willReturn(NOW);
        service = new FloorPlanQueryService(deskRepository, fixedAssignmentRepository,
                releaseRepository, requestRepository, employeeRepository, clock);
    }

    @Test
    void shouldThrowOutsideWindow_whenDateIsInThePast() {
        assertThatThrownBy(() -> service.floorPlanForDate(LOGIN, TODAY.minusDays(1)))
                .isInstanceOf(OutsideRequestWindowException.class);
    }

    @Test
    void shouldThrowNotFound_whenSessionLoginIsUnknown() {
        // Arrange: dentro de ventana pero el login no resuelve a empleado
        given(employeeRepository.findByLogin(LOGIN)).willReturn(Optional.empty());

        // Act / Assert
        assertThatThrownBy(() -> service.floorPlanForDate(LOGIN, WITHIN))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void shouldDeriveEachState_andNotRevealOtherHolders() {
        // Arrange
        stubRequester();
        given(deskRepository.findByActiveTrueOrderByNumberAsc())
                .willReturn(List.of(desk(1L, 1), desk(2L, 2), desk(3L, 3), desk(4L, 4), desk(5L, 5)));
        // Aprobadas: puesto 1 del solicitante (MINE), puesto 2 de un tercero (ASSIGNED)
        given(requestRepository.findByStatusAndResourceTypeAndRequestedDateBetween(
                RequestStatus.APPROVED, ResourceType.DESK, WITHIN, WITHIN))
                .willReturn(List.of(deskRequest(1L, REQUESTER_ID), deskRequest(2L, OTHER_ID)));
        // Pendientes: puesto 4 de un tercero (REQUESTED) + una generica sin recurso (se ignora)
        given(requestRepository.findByStatusAndResourceTypeAndRequestedDateBetween(
                RequestStatus.PENDING, ResourceType.DESK, WITHIN, WITHIN))
                .willReturn(List.of(deskRequest(4L, OTHER_ID), genericRequest(OTHER_ID)));
        // Fija: puesto 3 de un tercero ese dia, pero liberado esa fecha (RELEASED)
        given(fixedAssignmentRepository.findByResourceIdInAndResourceTypeAndActiveTrue(
                anyList(), eq(ResourceType.DESK)))
                .willReturn(List.of(deskFixed(3L, OTHER_ID, WITHIN_DOW)));
        given(releaseRepository.findByResourceTypeAndReleaseDateBetween(
                ResourceType.DESK, WITHIN, WITHIN)).willReturn(List.of(deskRelease(3L)));

        // Act
        FloorPlanResponse plan = service.floorPlanForDate(LOGIN, WITHIN);

        // Assert
        Map<Long, FloorPlanDeskState> states = plan.desks().stream()
                .collect(Collectors.toMap(FloorPlanDeskResponse::deskId, FloorPlanDeskResponse::state));
        assertThat(states.get(1L)).isEqualTo(FloorPlanDeskState.MINE);
        assertThat(states.get(2L)).isEqualTo(FloorPlanDeskState.ASSIGNED);
        assertThat(states.get(3L)).isEqualTo(FloorPlanDeskState.RELEASED);
        assertThat(states.get(4L)).isEqualTo(FloorPlanDeskState.REQUESTED);
        assertThat(states.get(5L)).isEqualTo(FloorPlanDeskState.FREE);
    }

    @Test
    void shouldMarkFixedAssignmentAsMine_whenHeldByRequester() {
        // Arrange: puesto 8 con asignacion fija del solicitante ese dia, no liberado
        stubRequester();
        given(deskRepository.findByActiveTrueOrderByNumberAsc()).willReturn(List.of(desk(8L, 8)));
        given(requestRepository.findByStatusAndResourceTypeAndRequestedDateBetween(
                any(), eq(ResourceType.DESK), eq(WITHIN), eq(WITHIN))).willReturn(List.of());
        given(fixedAssignmentRepository.findByResourceIdInAndResourceTypeAndActiveTrue(
                anyList(), eq(ResourceType.DESK)))
                .willReturn(List.of(deskFixed(8L, REQUESTER_ID, WITHIN_DOW)));
        given(releaseRepository.findByResourceTypeAndReleaseDateBetween(
                ResourceType.DESK, WITHIN, WITHIN)).willReturn(List.of());

        // Act
        FloorPlanResponse plan = service.floorPlanForDate(LOGIN, WITHIN);

        // Assert
        assertThat(plan.desks()).singleElement()
                .extracting(FloorPlanDeskResponse::state).isEqualTo(FloorPlanDeskState.MINE);
    }

    private void stubRequester() {
        Employee employee = mock(Employee.class);
        given(employee.getId()).willReturn(REQUESTER_ID);
        given(employeeRepository.findByLogin(LOGIN)).willReturn(Optional.of(employee));
    }

    private static Desk desk(Long id, int number) {
        Desk desk = Desk.create(number, DeskCategory.STANDARD,
                new BigDecimal("50.0"), new BigDecimal("50.0"));
        ReflectionTestUtils.setField(desk, "id", id);
        return desk;
    }

    private static RequestEntity deskRequest(Long resourceId, Long employeeId) {
        return RequestEntity.createForResource(employeeId, ResourceType.DESK, resourceId, WITHIN, NOW);
    }

    private static RequestEntity genericRequest(Long employeeId) {
        return RequestEntity.create(employeeId, ResourceType.DESK, WITHIN, NOW);
    }

    private static FixedAssignment deskFixed(Long resourceId, Long employeeId, int dow) {
        return FixedAssignment.create(
                resourceId, ResourceType.DESK, employeeId, dow, ADMIN_ID, NOW);
    }

    private static Release deskRelease(Long resourceId) {
        return Release.voluntary(resourceId, ResourceType.DESK, OTHER_ID, WITHIN, NOW);
    }
}
