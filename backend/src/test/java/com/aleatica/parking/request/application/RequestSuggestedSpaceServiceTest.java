package com.aleatica.parking.request.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.aleatica.parking.audit.AuditRecorder;
import com.aleatica.parking.auth.domain.ClockPort;
import com.aleatica.parking.availability.application.AvailabilityService;
import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeCategory;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.parkingspace.ParkingSpace;
import com.aleatica.parking.request.domain.RequestRepositoryPort;
import com.aleatica.parking.request.dto.SuggestedParkingSpaceResponse;
import com.aleatica.parking.resource.ResourceResolvers;
import com.aleatica.parking.systemsettings.application.SystemSettingsService;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Tests unitarios de {@link RequestService#suggestedSpace(Long, LocalDate)}: la vista previa de
 * solo lectura de la plaza que se auto-asignaria a un empleado (capability
 * {@code admin-punctual-assignment}), consumida por el resumen del asistente de reserva ANTES
 * de confirmar {@code POST /requests/admin}. Verifica que reutiliza {@code autoAssignParkingSpace}
 * (misma regla de categoria/planta alta&rarr;planta alta, base&rarr;planta baja), que degrada a
 * {@code available = false} sin plaza libre, y que un empleado inexistente responde 404. Usa un
 * fake in-memory del puerto de persistencia (no lo necesita, pero mantiene el mismo patron que el
 * resto de tests de {@link RequestService}) y mockea el resto de colaboradores; no toca BD.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RequestSuggestedSpaceServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-04T10:00:00Z");
    private static final LocalDate DATE = LocalDate.of(2026, 7, 10);
    private static final Long EMP_ID = 15L;
    private static final Long UNKNOWN_EMPLOYEE_ID = 999L;

    @Mock
    private RequestRepositoryPort requestRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ResourceResolvers resourceResolvers;

    @Mock
    private AvailabilityService availabilityService;

    @Mock
    private SystemSettingsService systemSettingsService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AuditRecorder auditRecorder;

    private final ClockPort clock = () -> NOW;

    private RequestService newService() {
        return new RequestService(
                requestRepository, employeeRepository, resourceResolvers,
                availabilityService, systemSettingsService, eventPublisher, auditRecorder, clock);
    }

    @Test
    void shouldSuggestHighestPhysicalFloorSpace_whenCategoryIsHigh() {
        // Arrange: categoria alta -> preferencia por la planta fisica mas alta (-1, 1xxx)
        givenTargetEmployee(EmployeeCategory.DIRECTOR_N1);
        given(availabilityService.freeParkingSpacesForDate(DATE))
                .willReturn(List.of(space(5001), space(1001)));

        // Act
        SuggestedParkingSpaceResponse result = newService().suggestedSpace(EMP_ID, DATE);

        // Assert
        assertThat(result.available()).isTrue();
        assertThat(result.number()).isEqualTo(1001);
        assertThat(result.floor()).isEqualTo(1);
    }

    @Test
    void shouldSuggestLowestPhysicalFloorSpace_whenCategoryIsBase() {
        // Arrange: categoria base -> preferencia por la planta fisica mas baja (-5, 5xxx)
        givenTargetEmployee(EmployeeCategory.EMPLEADO);
        given(availabilityService.freeParkingSpacesForDate(DATE))
                .willReturn(List.of(space(1001), space(5001)));

        // Act
        SuggestedParkingSpaceResponse result = newService().suggestedSpace(EMP_ID, DATE);

        // Assert
        assertThat(result.available()).isTrue();
        assertThat(result.number()).isEqualTo(5001);
        assertThat(result.floor()).isEqualTo(5);
    }

    @Test
    void shouldReturnUnavailable_whenNoFreeSpaceForDate() {
        // Arrange
        givenTargetEmployee(EmployeeCategory.EMPLEADO);
        given(availabilityService.freeParkingSpacesForDate(DATE)).willReturn(List.of());

        // Act
        SuggestedParkingSpaceResponse result = newService().suggestedSpace(EMP_ID, DATE);

        // Assert: 200 con available = false, no una excepcion
        assertThat(result.available()).isFalse();
        assertThat(result.parkingSpaceId()).isNull();
        assertThat(result.number()).isNull();
        assertThat(result.floor()).isNull();
    }

    @Test
    void shouldThrowNotFound_whenTargetEmployeeDoesNotExist() {
        // Arrange
        given(employeeRepository.findById(UNKNOWN_EMPLOYEE_ID)).willReturn(Optional.empty());

        // Act / Assert
        assertThatThrownBy(() -> newService().suggestedSpace(UNKNOWN_EMPLOYEE_ID, DATE))
                .isInstanceOf(EntityNotFoundException.class);
    }

    private void givenTargetEmployee(EmployeeCategory category) {
        Employee employee = mock(Employee.class);
        given(employee.getId()).willReturn(EMP_ID);
        given(employee.getCategory()).willReturn(category);
        given(employeeRepository.findById(EMP_ID)).willReturn(Optional.of(employee));
    }

    private static ParkingSpace space(int number) {
        return ParkingSpace.create(number);
    }
}
