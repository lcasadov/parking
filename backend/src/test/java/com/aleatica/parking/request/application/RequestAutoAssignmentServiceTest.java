package com.aleatica.parking.request.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.aleatica.parking.audit.AuditRecorder;
import com.aleatica.parking.auth.domain.ClockPort;
import com.aleatica.parking.availability.application.AvailabilityService;
import com.aleatica.parking.availability.dto.AvailabilityResponse;
import com.aleatica.parking.desk.Desk;
import com.aleatica.parking.desk.DeskCategory;
import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeCategory;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.fixedassignment.infrastructure.FixedAssignmentEntity;
import com.aleatica.parking.fixedassignment.infrastructure.FixedAssignmentJpaRepository;
import com.aleatica.parking.notification.event.RequestApprovedEvent;
import com.aleatica.parking.parkingspace.ParkingSpace;
import com.aleatica.parking.request.domain.RejectionReasonCode;
import com.aleatica.parking.request.domain.Request;
import com.aleatica.parking.request.domain.RequestRepositoryPort;
import com.aleatica.parking.request.domain.RequestStatus;
import com.aleatica.parking.request.dto.RequestCreateRequest;
import com.aleatica.parking.request.dto.RequestRejectRequest;
import com.aleatica.parking.request.dto.RequestResponse;
import com.aleatica.parking.request.dto.SuggestedResourceResponse;
import com.aleatica.parking.resource.BookableResource;
import com.aleatica.parking.resource.ResourceResolvers;
import com.aleatica.parking.resource.ResourceType;
import com.aleatica.parking.systemsettings.application.SystemSettingsService;
import com.aleatica.parking.systemsettings.domain.ApprovalMode;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/**
 * Tests unitarios del modo <strong>automatico</strong> de {@link RequestService} (change
 * {@code request-auto-assignment}): el algoritmo de auto-asignacion de plaza por categoria/planta
 * (grupo ALTO vs BASE, fallback total, orden determinista, sin disponibilidad) y la ramificacion
 * del alta ({@code AUTOMATIC} plaza/puesto, 409 {@code NO_AVAILABILITY}, 409 puesto no disponible).
 * Usa un fake in-memory del puerto y mockea el resto de colaboradores; no toca la base de datos.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RequestAutoAssignmentServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-04T10:00:00Z");
    private static final LocalDate DATE = LocalDate.of(2026, 7, 10);

    private static final String EMP_LOGIN = "employee";
    private static final Long EMP_ID = 15L;
    private static final Long DESK_ID = 55L;

    private final InMemoryRequestRepository requestRepository = new InMemoryRequestRepository();

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ResourceResolvers resourceResolvers;

    @Mock
    private AvailabilityService availabilityService;

    @Mock
    private SystemSettingsService systemSettingsService;

    @Mock
    private FixedAssignmentJpaRepository fixedAssignmentRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AuditRecorder auditRecorder;

    @Captor
    private ArgumentCaptor<Object> eventCaptor;

    private final ClockPort clock = () -> NOW;

    private RequestService newService() {
        return new RequestService(
                requestRepository, employeeRepository, resourceResolvers,
                availabilityService, systemSettingsService, fixedAssignmentRepository,
                eventPublisher, auditRecorder, clock);
    }

    // ---- Algoritmo de auto-asignacion ----

    @Test
    void shouldAssignHighestPhysicalFloor_whenCategoryIsHigh() {
        // Arrange: garaje subterraneo, plazas libres en plantas -1 (1xxx), -3 (3xxx) y -5 (5xxx)
        given(availabilityService.freeParkingSpacesForDate(DATE))
                .willReturn(List.of(space(1001), space(3001), space(5001)));

        // Act
        Optional<ParkingSpace> chosen = newService()
                .autoAssignParkingSpace(EmployeeCategory.DIRECTOR_N1, DATE);

        // Assert: categoria alta -> planta fisica mas alta = planta -1 = 1xxx (1001)
        assertThat(chosen).map(ParkingSpace::getNumber).contains(1001);
    }

    @Test
    void shouldAssignLowestPhysicalFloor_whenCategoryIsBase() {
        // Arrange: garaje subterraneo, plazas libres en plantas -5 (5xxx), -3 (3xxx) y -1 (1xxx)
        given(availabilityService.freeParkingSpacesForDate(DATE))
                .willReturn(List.of(space(5001), space(3001), space(1001)));

        // Act
        Optional<ParkingSpace> chosen = newService()
                .autoAssignParkingSpace(EmployeeCategory.EMPLEADO, DATE);

        // Assert: categoria base -> planta fisica mas baja = planta -5 = 5xxx (5001)
        assertThat(chosen).map(ParkingSpace::getNumber).contains(5001);
    }

    @Test
    void shouldFallBackToNextFloor_whenPreferredFloorHasNoFreeSpace() {
        // Arrange: categoria alta sin libres en la planta -1 (1xxx); la mas alta disponible es -2 (2xxx)
        given(availabilityService.freeParkingSpacesForDate(DATE))
                .willReturn(List.of(space(3002), space(2002)));

        // Act
        Optional<ParkingSpace> chosen = newService()
                .autoAssignParkingSpace(EmployeeCategory.DIRECTOR_N1, DATE);

        // Assert: fallback a la siguiente planta preferida por una categoria alta = planta -2 (2002)
        assertThat(chosen).map(ParkingSpace::getNumber).contains(2002);
    }

    @Test
    void shouldChooseLowestNumberWithinFloor_deterministically() {
        // Arrange: varias plazas libres en la misma planta 2
        given(availabilityService.freeParkingSpacesForDate(DATE))
                .willReturn(List.of(space(2003), space(2001), space(2002)));

        // Act
        Optional<ParkingSpace> chosen = newService()
                .autoAssignParkingSpace(EmployeeCategory.EMPLEADO, DATE);

        // Assert: orden estable dentro de la planta -> menor numero (2001)
        assertThat(chosen).map(ParkingSpace::getNumber).contains(2001);
    }

    @Test
    void shouldReturnEmpty_whenNoFreeSpace() {
        // Arrange
        given(availabilityService.freeParkingSpacesForDate(DATE)).willReturn(List.of());

        // Act / Assert
        assertThat(newService().autoAssignParkingSpace(EmployeeCategory.GERENTE, DATE)).isEmpty();
    }

    // ---- Algoritmo de auto-asignacion de PUESTO por categoria (change desk-auto-assignment) ----

    @Test
    void shouldAssignExecutiveDesk_whenCategoryIsHighAndExecutiveFree() {
        // Arrange: alto con EXECUTIVE y STANDARD libres -> prefiere el EXECUTIVE
        given(availabilityService.freeDesksForDate(DATE)).willReturn(List.of(
                desk(1L, 3, DeskCategory.STANDARD), desk(2L, 7, DeskCategory.EXECUTIVE)));

        // Act
        Optional<Desk> chosen = newService().autoAssignDesk(EmployeeCategory.DIRECTOR_N1, DATE);

        // Assert
        assertThat(chosen).map(Desk::getNumber).contains(7);
    }

    @Test
    void shouldFallBackToStandardDesk_whenCategoryIsHighAndNoExecutiveFree() {
        // Arrange: alto sin EXECUTIVE libre, pero con STANDARD libre -> cae a STANDARD
        given(availabilityService.freeDesksForDate(DATE)).willReturn(List.of(
                desk(1L, 3, DeskCategory.STANDARD)));

        // Act
        Optional<Desk> chosen = newService().autoAssignDesk(EmployeeCategory.CEO, DATE);

        // Assert
        assertThat(chosen).map(Desk::getNumber).contains(3);
    }

    @Test
    void shouldAssignOnlyStandardDesk_whenCategoryIsNotHigh() {
        // Arrange: no-alto con STANDARD y EXECUTIVE libres -> solo candidata STANDARD
        given(availabilityService.freeDesksForDate(DATE)).willReturn(List.of(
                desk(1L, 3, DeskCategory.EXECUTIVE), desk(2L, 9, DeskCategory.STANDARD)));

        // Act
        Optional<Desk> chosen = newService().autoAssignDesk(EmployeeCategory.EMPLEADO, DATE);

        // Assert
        assertThat(chosen).map(Desk::getNumber).contains(9);
    }

    @Test
    void shouldReturnEmpty_whenNotHighCategoryAndOnlyExecutiveDeskFree() {
        // Arrange: no-alto, unico puesto libre es EXECUTIVE -> nunca se le asigna
        given(availabilityService.freeDesksForDate(DATE)).willReturn(List.of(
                desk(1L, 3, DeskCategory.EXECUTIVE)));

        // Act / Assert
        assertThat(newService().autoAssignDesk(EmployeeCategory.MANDO_INTERMEDIO, DATE)).isEmpty();
    }

    @Test
    void shouldChooseLowestDeskNumberWithinCategory_deterministically() {
        // Arrange: varios STANDARD libres para un no-alto
        given(availabilityService.freeDesksForDate(DATE)).willReturn(List.of(
                desk(1L, 9, DeskCategory.STANDARD), desk(2L, 4, DeskCategory.STANDARD),
                desk(3L, 6, DeskCategory.STANDARD)));

        // Act
        Optional<Desk> chosen = newService().autoAssignDesk(EmployeeCategory.EMPLEADO, DATE);

        // Assert: orden estable -> menor numero (4)
        assertThat(chosen).map(Desk::getNumber).contains(4);
    }

    @Test
    void shouldReturnEmpty_whenNoFreeDesk() {
        // Arrange
        given(availabilityService.freeDesksForDate(DATE)).willReturn(List.of());

        // Act / Assert
        assertThat(newService().autoAssignDesk(EmployeeCategory.GERENTE, DATE)).isEmpty();
    }

    // ---- Alta AUTOMATIC / PLAZA ----

    @Test
    void shouldCreateApprovedParking_whenAutomaticAndSpaceFree() {
        // Arrange
        givenAutomaticActor(EmployeeCategory.EMPLEADO);
        ParkingSpace assigned = mock(ParkingSpace.class);
        given(assigned.getId()).willReturn(8L);
        given(assigned.getNumber()).willReturn(1001);
        given(assigned.floor()).willReturn(1);
        given(availabilityService.freeParkingSpacesForDate(DATE)).willReturn(List.of(assigned));

        // Act
        RequestResponse result = newService().create(EMP_LOGIN, new RequestCreateRequest(DATE, null));

        // Assert: nace APPROVED con la plaza auto-asignada
        assertThat(result.status()).isEqualTo(RequestStatus.APPROVED);
        assertThat(result.parkingSpaceId()).isEqualTo(8L);
        verifyEventPublished(RequestApprovedEvent.class);
    }

    @Test
    void shouldThrowNoAvailability_whenAutomaticParkingAndNoFreeSpace() {
        // Arrange
        givenAutomaticActor(EmployeeCategory.EMPLEADO);
        given(availabilityService.freeParkingSpacesForDate(DATE)).willReturn(List.of());

        // Act / Assert: 409 NO_AVAILABILITY y la solicitud NO se crea
        assertThatThrownBy(() -> newService().create(EMP_LOGIN, new RequestCreateRequest(DATE, null)))
                .isInstanceOf(NoAvailabilityException.class);
        assertThat(requestRepository.saves()).isZero();
    }

    // ---- Alta AUTOMATIC / PUESTO ----

    @Test
    void shouldCreateApprovedDesk_whenAutomaticAndChosenDeskFree() {
        // Arrange: puesto elegido, disponible
        givenAutomaticActor(EmployeeCategory.EMPLEADO);
        given(resourceResolvers.exists(DESK_ID, ResourceType.DESK)).willReturn(true);
        given(availabilityService.isSpaceTakenForDate(DESK_ID, ResourceType.DESK, DATE))
                .willReturn(false);

        // Act
        RequestResponse result = newService().create(
                EMP_LOGIN, new RequestCreateRequest(DATE, ResourceType.DESK, DESK_ID));

        // Assert: nace APPROVED con el puesto elegido
        assertThat(result.status()).isEqualTo(RequestStatus.APPROVED);
        assertThat(result.parkingSpaceId()).isEqualTo(DESK_ID);
        assertThat(result.resourceType()).isEqualTo(ResourceType.DESK);
        verifyEventPublished(RequestApprovedEvent.class);
    }

    @Test
    void shouldThrowUnavailable_whenAutomaticDeskAlreadyTaken() {
        // Arrange: el puesto elegido no esta disponible esa fecha
        givenAutomaticActor(EmployeeCategory.EMPLEADO);
        given(resourceResolvers.exists(DESK_ID, ResourceType.DESK)).willReturn(true);
        given(availabilityService.isSpaceTakenForDate(DESK_ID, ResourceType.DESK, DATE))
                .willReturn(true);

        // Act / Assert
        assertThatThrownBy(() -> newService().create(
                EMP_LOGIN, new RequestCreateRequest(DATE, ResourceType.DESK, DESK_ID)))
                .isInstanceOf(SpaceUnavailableException.class);
        assertThat(requestRepository.saves()).isZero();
    }

    // ---- Alta AUTOMATIC / PUESTO sin elegir (auto-asignacion por categoria, desk-auto-assignment) ----

    @Test
    void shouldCreateApprovedExecutiveDesk_whenAutomaticHighCategoryWithoutResourceId() {
        // Arrange: alto, sin resourceId, EXECUTIVE libre
        givenAutomaticActor(EmployeeCategory.DIRECTOR_N2);
        given(availabilityService.freeDesksForDate(DATE))
                .willReturn(List.of(desk(DESK_ID, 7, DeskCategory.EXECUTIVE)));

        // Act
        RequestResponse result = newService().create(
                EMP_LOGIN, new RequestCreateRequest(DATE, ResourceType.DESK));

        // Assert: auto-asigna el EXECUTIVE, nace APPROVED, sin exigir eleccion en el plano
        assertThat(result.status()).isEqualTo(RequestStatus.APPROVED);
        assertThat(result.parkingSpaceId()).isEqualTo(DESK_ID);
        verifyEventPublished(RequestApprovedEvent.class);
    }

    @Test
    void shouldCreateApprovedStandardDesk_whenAutomaticNotHighCategoryWithoutResourceId() {
        // Arrange: no-alto, sin resourceId, solo STANDARD libre
        givenAutomaticActor(EmployeeCategory.EMPLEADO);
        given(availabilityService.freeDesksForDate(DATE))
                .willReturn(List.of(desk(DESK_ID, 3, DeskCategory.STANDARD)));

        // Act
        RequestResponse result = newService().create(
                EMP_LOGIN, new RequestCreateRequest(DATE, ResourceType.DESK));

        // Assert
        assertThat(result.status()).isEqualTo(RequestStatus.APPROVED);
        assertThat(result.parkingSpaceId()).isEqualTo(DESK_ID);
    }

    @Test
    void shouldCreatePendingDesk_whenAutomaticNotHighCategoryOnlyExecutiveFree() {
        // Arrange: no-alto, sin resourceId, unico puesto libre es EXECUTIVE -> nunca se le asigna
        givenAutomaticActor(EmployeeCategory.EMPLEADO);
        given(availabilityService.freeDesksForDate(DATE))
                .willReturn(List.of(desk(DESK_ID, 7, DeskCategory.EXECUTIVE)));
        given(availabilityService.availabilityForDate(DATE, ResourceType.DESK))
                .willReturn(new AvailabilityResponse(DATE, List.of()));

        // Act
        RequestResponse result = newService().create(
                EMP_LOGIN, new RequestCreateRequest(DATE, ResourceType.DESK));

        // Assert: cae al flujo manual (PENDING), NO 409 ni RESOURCE_SELECTION_REQUIRED
        assertThat(result.status()).isEqualTo(RequestStatus.PENDING);
        assertThat(result.parkingSpaceId()).isNull();
    }

    @Test
    void shouldCreatePendingDesk_whenAutomaticAndNoDeskFreeAtAll() {
        // Arrange: sin resourceId y ningun puesto libre
        givenAutomaticActor(EmployeeCategory.GERENTE);
        given(availabilityService.freeDesksForDate(DATE)).willReturn(List.of());
        given(availabilityService.availabilityForDate(DATE, ResourceType.DESK))
                .willReturn(new AvailabilityResponse(DATE, List.of()));

        // Act
        RequestResponse result = newService().create(
                EMP_LOGIN, new RequestCreateRequest(DATE, ResourceType.DESK));

        // Assert: PENDING (fallback), no lanza excepcion
        assertThat(result.status()).isEqualTo(RequestStatus.PENDING);
    }

    @Test
    void shouldRejectApprovedAndFreeResource_whenAdminRejectsAutoApproved() {
        // Arrange: solicitud auto-aprobada (nace APPROVED, resolutor sistema)
        Request approved = Request.restore(
                77L, EMP_ID, DATE, RequestStatus.APPROVED, 8L, ResourceType.PARKING,
                "auto", null, null, null, NOW, NOW);
        requestRepository.seed(approved);
        Employee admin = mockEmployee(1L, EmployeeCategory.EMPLEADO);
        given(employeeRepository.findByLogin("admin")).willReturn(Optional.of(admin));

        // Act
        RequestResponse result = newService().reject(
                77L, new RequestRejectRequest(RejectionReasonCode.OUTSIDE_POLICY, null), "admin");

        // Assert: REJECTED -> la fila deja de contar como APPROVED (recurso liberado)
        assertThat(result.status()).isEqualTo(RequestStatus.REJECTED);
        assertThat(requestRepository.byId(77L).getStatus()).isEqualTo(RequestStatus.REJECTED);
    }

    // ---- Feature A: preferencia por el recurso FIJO propio antes de la categoria ----

    @Test
    void shouldPreferOwnFixedParking_whenFixedResourceFreeOnThatDate() {
        // Arrange: el empleado tiene plaza fija 3005 ese dia de la semana y esta libre esa fecha
        givenAutomaticActor(EmployeeCategory.EMPLEADO);
        int dayOfWeek = DATE.getDayOfWeek().getValue();
        FixedAssignmentEntity ownFixed = fixed(3005L);
        given(fixedAssignmentRepository.findByEmployeeIdAndResourceTypeAndDayOfWeekAndActiveTrue(
                EMP_ID, ResourceType.PARKING, dayOfWeek)).willReturn(List.of(ownFixed));
        given(availabilityService.isSpaceTakenForDate(3005L, ResourceType.PARKING, DATE))
                .willReturn(false);

        // Act
        RequestResponse result = newService().create(EMP_LOGIN, new RequestCreateRequest(DATE, null));

        // Assert: recupera SU fijo (3005), sin pasar por la auto-asignacion por categoria
        assertThat(result.status()).isEqualTo(RequestStatus.APPROVED);
        assertThat(result.parkingSpaceId()).isEqualTo(3005L);
        verifyEventPublished(RequestApprovedEvent.class);
    }

    @Test
    void shouldFallBackToCategoryParking_whenOwnFixedTakenByAnother() {
        // Arrange: tiene fijo 3005 ese dia pero ya lo ocupa otro esa fecha -> cae a categoria
        givenAutomaticActor(EmployeeCategory.EMPLEADO);
        int dayOfWeek = DATE.getDayOfWeek().getValue();
        FixedAssignmentEntity ownFixed = fixed(3005L);
        given(fixedAssignmentRepository.findByEmployeeIdAndResourceTypeAndDayOfWeekAndActiveTrue(
                EMP_ID, ResourceType.PARKING, dayOfWeek)).willReturn(List.of(ownFixed));
        given(availabilityService.isSpaceTakenForDate(3005L, ResourceType.PARKING, DATE))
                .willReturn(true);
        ParkingSpace fromCategory = mock(ParkingSpace.class);
        given(fromCategory.getId()).willReturn(8L);
        given(fromCategory.getNumber()).willReturn(5001);
        given(fromCategory.floor()).willReturn(5);
        given(availabilityService.freeParkingSpacesForDate(DATE)).willReturn(List.of(fromCategory));

        // Act
        RequestResponse result = newService().create(EMP_LOGIN, new RequestCreateRequest(DATE, null));

        // Assert: no recupera su fijo (ocupado); recibe la plaza por categoria (8)
        assertThat(result.status()).isEqualTo(RequestStatus.APPROVED);
        assertThat(result.parkingSpaceId()).isEqualTo(8L);
    }

    @Test
    void shouldPreferOwnFixedDesk_whenFixedDeskFreeOnThatDate() {
        // Arrange: el empleado tiene puesto fijo DESK_ID ese dia y esta libre esa fecha
        givenAutomaticActor(EmployeeCategory.EMPLEADO);
        int dayOfWeek = DATE.getDayOfWeek().getValue();
        FixedAssignmentEntity ownFixed = fixed(DESK_ID);
        given(fixedAssignmentRepository.findByEmployeeIdAndResourceTypeAndDayOfWeekAndActiveTrue(
                EMP_ID, ResourceType.DESK, dayOfWeek)).willReturn(List.of(ownFixed));
        given(availabilityService.isSpaceTakenForDate(DESK_ID, ResourceType.DESK, DATE))
                .willReturn(false);

        // Act
        RequestResponse result = newService().create(
                EMP_LOGIN, new RequestCreateRequest(DATE, ResourceType.DESK));

        // Assert: recupera SU puesto fijo, sin pasar por la auto-asignacion por categoria
        assertThat(result.status()).isEqualTo(RequestStatus.APPROVED);
        assertThat(result.parkingSpaceId()).isEqualTo(DESK_ID);
        assertThat(result.resourceType()).isEqualTo(ResourceType.DESK);
    }

    private static FixedAssignmentEntity fixed(Long resourceId) {
        FixedAssignmentEntity entity = mock(FixedAssignmentEntity.class);
        given(entity.getResourceId()).willReturn(resourceId);
        return entity;
    }

    // ---- Task 3: preview de auto-asignacion para el propio empleado (ambos tipos) ----

    @Test
    void shouldSuggestOwnFixedParking_whenPreviewAndFixedFree() {
        // Arrange: el empleado tiene plaza fija 3005 libre ese dia -> se sugiere SU fija
        Employee employee = mockEmployee(EMP_ID, EmployeeCategory.EMPLEADO);
        given(employeeRepository.findByLogin(EMP_LOGIN)).willReturn(Optional.of(employee));
        int dayOfWeek = DATE.getDayOfWeek().getValue();
        FixedAssignmentEntity ownFixed = fixed(3005L);
        given(fixedAssignmentRepository.findByEmployeeIdAndResourceTypeAndDayOfWeekAndActiveTrue(
                EMP_ID, ResourceType.PARKING, dayOfWeek)).willReturn(List.of(ownFixed));
        given(availabilityService.isSpaceTakenForDate(3005L, ResourceType.PARKING, DATE))
                .willReturn(false);
        BookableResource resource = mock(BookableResource.class);
        given(resource.getNumber()).willReturn(3005);
        given(resourceResolvers.resolveAll(List.of(3005L), ResourceType.PARKING))
                .willReturn(Map.of(3005L, resource));

        // Act
        SuggestedResourceResponse result =
                newService().suggestForEmployee(EMP_LOGIN, DATE, ResourceType.PARKING);

        // Assert
        assertThat(result.available()).isTrue();
        assertThat(result.resourceLabel()).isEqualTo("Plaza 3005");
    }

    @Test
    void shouldSuggestCategoryDesk_whenPreviewWithoutOwnFixed() {
        // Arrange: sin fijo (repo vacio por defecto), se sugiere un puesto por categoria
        Employee employee = mockEmployee(EMP_ID, EmployeeCategory.EMPLEADO);
        given(employeeRepository.findByLogin(EMP_LOGIN)).willReturn(Optional.of(employee));
        given(availabilityService.freeDesksForDate(DATE))
                .willReturn(List.of(desk(DESK_ID, 12, DeskCategory.STANDARD)));

        // Act
        SuggestedResourceResponse result =
                newService().suggestForEmployee(EMP_LOGIN, DATE, ResourceType.DESK);

        // Assert
        assertThat(result.available()).isTrue();
        assertThat(result.resourceLabel()).isEqualTo("Puesto 12");
    }

    @Test
    void shouldSuggestUnavailable_whenPreviewAndNoResourceFree() {
        // Arrange: sin fijo y sin plazas libres
        Employee employee = mockEmployee(EMP_ID, EmployeeCategory.EMPLEADO);
        given(employeeRepository.findByLogin(EMP_LOGIN)).willReturn(Optional.of(employee));
        given(availabilityService.freeParkingSpacesForDate(DATE)).willReturn(List.of());

        // Act
        SuggestedResourceResponse result =
                newService().suggestForEmployee(EMP_LOGIN, DATE, ResourceType.PARKING);

        // Assert
        assertThat(result.available()).isFalse();
        assertThat(result.resourceLabel()).isNull();
    }

    private void givenAutomaticActor(EmployeeCategory category) {
        given(systemSettingsService.approvalMode()).willReturn(ApprovalMode.AUTOMATIC);
        // El mock del empleado se crea (y estubea) ANTES del given externo para no
        // anidar stubbing (evita UnfinishedStubbingException de Mockito).
        Employee employee = mockEmployee(EMP_ID, category);
        given(employeeRepository.findByLogin(EMP_LOGIN)).willReturn(Optional.of(employee));
    }

    private Employee mockEmployee(Long id, EmployeeCategory category) {
        Employee employee = mock(Employee.class);
        given(employee.getId()).willReturn(id);
        given(employee.getCategory()).willReturn(category);
        return employee;
    }

    private void verifyEventPublished(Class<?> eventType) {
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(eventType);
    }

    private static ParkingSpace space(int number) {
        return ParkingSpace.create(number);
    }

    private static Desk desk(Long id, int number, DeskCategory category) {
        Desk desk = Desk.create(number, category, new BigDecimal("10.00"), new BigDecimal("20.00"));
        setField(desk, "id", id);
        return desk;
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

    /**
     * Fake in-memory del puerto de persistencia de solicitudes para los tests del modo automatico
     * (mismo patron que {@code RequestServiceTest}): {@link #seed(Request)} precarga sin contar
     * como escritura; {@code save}/{@code saveAndFlush} cuentan la invocacion.
     */
    private static final class InMemoryRequestRepository implements RequestRepositoryPort {

        private final java.util.Map<Long, Request> store = new java.util.HashMap<>();
        private long sequence = 1000L;
        private int saves;

        void seed(Request request) {
            store.put(request.getId(), request);
        }

        Request byId(Long id) {
            return store.get(id);
        }

        int saves() {
            return saves;
        }

        @Override
        public Optional<Request> findById(Long id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Request save(Request request) {
            return persist(request);
        }

        @Override
        public Request saveAndFlush(Request request) {
            return persist(request);
        }

        private Request persist(Request request) {
            saves++;
            Long id = request.getId() != null ? request.getId() : ++sequence;
            Request stored = Request.restore(
                    id, request.getEmployeeId(), request.getRequestedDate(), request.getStatus(),
                    request.getResourceId(), request.getResourceType(), request.getApprovalNote(),
                    request.getRejectionReasonCode(), request.getRejectionReason(),
                    request.getResolvedById(), request.getResolvedAt(), request.getCreatedAt(),
                    request.getLastRemindedAt(), request.isWaitlisted());
            store.put(id, stored);
            return stored;
        }

        @Override
        public boolean existsByEmployeeIdAndResourceTypeAndRequestedDateAndStatus(
                Long employeeId, ResourceType resourceType, LocalDate requestedDate, RequestStatus status) {
            return store.values().stream().anyMatch(r ->
                    employeeId.equals(r.getEmployeeId())
                            && resourceType == r.getResourceType()
                            && requestedDate.equals(r.getRequestedDate())
                            && status == r.getStatus());
        }

        @Override
        public Page<Request> findByEmployeeId(Long employeeId, Pageable pageable) {
            return new PageImpl<>(List.copyOf(store.values()));
        }

        @Override
        public Page<Request> findByEmployeeIdAndStatus(
                Long employeeId, RequestStatus status, Pageable pageable) {
            return new PageImpl<>(List.copyOf(store.values()));
        }

        @Override
        public Page<Request> findByEmployeeIdAndRequestedDateBetween(
                Long employeeId, LocalDate from, LocalDate to, Pageable pageable) {
            return new PageImpl<>(List.copyOf(store.values()));
        }

        @Override
        public Page<Request> findByEmployeeIdAndStatusAndRequestedDateBetween(
                Long employeeId, RequestStatus status, LocalDate from, LocalDate to, Pageable pageable) {
            return new PageImpl<>(List.copyOf(store.values()));
        }

        @Override
        public Page<Request> findByStatusOrderByCreatedAtAsc(RequestStatus status, Pageable pageable) {
            return new PageImpl<>(List.copyOf(store.values()));
        }

        @Override
        public Page<Request> findByStatusOrderByCreatedAtDesc(RequestStatus status, Pageable pageable) {
            return new PageImpl<>(List.copyOf(store.values()));
        }

        @Override
        public Page<Request> findAllByOrderByCreatedAtDesc(Pageable pageable) {
            return new PageImpl<>(List.copyOf(store.values()));
        }

        @Override
        public List<Request> findByStatusAndWaitlistedTrueAndResourceTypeAndRequestedDateOrderByCreatedAtAsc(
                RequestStatus status, ResourceType resourceType, LocalDate requestedDate) {
            return List.of();
        }
    }
}
