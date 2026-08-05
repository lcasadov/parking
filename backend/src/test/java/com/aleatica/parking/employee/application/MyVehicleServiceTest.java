package com.aleatica.parking.employee.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.employee.VehicleStatus;
import com.aleatica.parking.employee.dto.EmployeeVehicleRequest;
import com.aleatica.parking.employee.dto.EmployeeVehicleResponse;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests unitarios de {@link MyVehicleService} (self-service): resuelve el empleado por su login,
 * delega el CRUD en {@link EmployeeVehicleService} (alta/edición -> PENDING) y avisa a los
 * administradores en alta y edición (no en borrado). Repositorio/servicio/notificador mockeados.
 */
@ExtendWith(MockitoExtension.class)
class MyVehicleServiceTest {

    private static final String LOGIN = "jperez";
    private static final Long EMPLOYEE_ID = 7L;
    private static final Long VEHICLE_ID = 3L;

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private EmployeeVehicleService vehicleService;
    @Mock
    private EmployeeVehicleAdminNotifier adminNotifier;

    private MyVehicleService service() {
        return new MyVehicleService(employeeRepository, vehicleService, adminNotifier);
    }

    private Employee employee() {
        Employee employee = org.mockito.Mockito.mock(Employee.class);
        given(employee.getId()).willReturn(EMPLOYEE_ID);
        return employee;
    }

    private EmployeeVehicleResponse response(String plate) {
        return new EmployeeVehicleResponse(
                VEHICLE_ID, EMPLOYEE_ID, plate, "Seat", "Leon", "Gris",
                VehicleStatus.PENDING, null, Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void shouldThrowNotFound_whenAuthenticatedEmployeeDoesNotExist() {
        given(employeeRepository.findByLogin(LOGIN)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service().list(LOGIN))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void shouldCreateForOwnEmployeeAndNotifyAdmins() {
        Employee employee = employee();
        given(employee.getFirstName()).willReturn("Juan");
        given(employee.getLastName()).willReturn("Perez");
        given(employeeRepository.findByLogin(LOGIN)).willReturn(Optional.of(employee));
        given(vehicleService.createForEmployee(eq(EMPLOYEE_ID), any())).willReturn(response("1234ABC"));

        EmployeeVehicleResponse result =
                service().create(LOGIN, new EmployeeVehicleRequest("1234ABC", null, null, null));

        assertThat(result.status()).isEqualTo(VehicleStatus.PENDING);
        verify(vehicleService).createForEmployee(eq(EMPLOYEE_ID), any());
        verify(adminNotifier).vehicleSubmitted("Juan Perez", "1234ABC");
    }

    @Test
    void shouldUpdateOwnVehicleAndNotifyAdmins() {
        Employee employee = employee();
        given(employee.getFirstName()).willReturn("Juan");
        given(employee.getLastName()).willReturn("Perez");
        given(employeeRepository.findByLogin(LOGIN)).willReturn(Optional.of(employee));
        given(vehicleService.updateForEmployee(eq(EMPLOYEE_ID), eq(VEHICLE_ID), any()))
                .willReturn(response("5678XYZ"));

        service().update(LOGIN, VEHICLE_ID, new EmployeeVehicleRequest("5678XYZ", null, null, null));

        verify(vehicleService).updateForEmployee(eq(EMPLOYEE_ID), eq(VEHICLE_ID), any());
        verify(adminNotifier).vehicleSubmitted("Juan Perez", "5678XYZ");
    }

    @Test
    void shouldHardDeleteWithoutNotifying_whenPendingOrRejected() {
        Employee employee = employee();
        given(employeeRepository.findByLogin(LOGIN)).willReturn(Optional.of(employee));
        given(vehicleService.requestDeletionForEmployee(EMPLOYEE_ID, VEHICLE_ID))
                .willReturn(Optional.empty());

        service().delete(LOGIN, VEHICLE_ID);

        verify(adminNotifier, never()).deletionRequested(any(), any());
    }

    @Test
    void shouldMarkPendingDeletionAndNotifyAdmins_whenInProgressOrApproved() {
        Employee employee = employee();
        given(employee.getFirstName()).willReturn("Juan");
        given(employee.getLastName()).willReturn("Perez");
        given(employeeRepository.findByLogin(LOGIN)).willReturn(Optional.of(employee));
        given(vehicleService.requestDeletionForEmployee(EMPLOYEE_ID, VEHICLE_ID))
                .willReturn(Optional.of(response("1234ABC")));

        service().delete(LOGIN, VEHICLE_ID);

        verify(adminNotifier).deletionRequested("Juan Perez", "1234ABC");
    }
}
