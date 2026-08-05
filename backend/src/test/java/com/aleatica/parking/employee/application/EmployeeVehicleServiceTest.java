package com.aleatica.parking.employee.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.employee.EmployeeVehicle;
import com.aleatica.parking.employee.EmployeeVehicleRepository;
import com.aleatica.parking.employee.dto.EmployeeVehicleRequest;
import com.aleatica.parking.employee.dto.EmployeeVehicleResponse;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests unitarios de {@link EmployeeVehicleService} con repositorios mockeados: normalizacion de
 * matricula (trim + mayusculas), unicidad por empleado (409), pertenencia vehiculo↔empleado (404),
 * existencia del empleado (404) y saneado a null de marca/modelo/color en blanco. No toca BD
 * (change {@code employee-vehicles}).
 */
@ExtendWith(MockitoExtension.class)
class EmployeeVehicleServiceTest {

    private static final Long EMPLOYEE_ID = 15L;
    private static final Long VEHICLE_ID = 3L;

    @Mock
    private EmployeeVehicleRepository vehicleRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private EmployeeVehicleHistoryRecorder historyRecorder;

    private EmployeeVehicleService service() {
        return new EmployeeVehicleService(vehicleRepository, employeeRepository, historyRecorder);
    }

    @Test
    void shouldListVehicles_whenEmployeeExists() {
        given(employeeRepository.existsById(EMPLOYEE_ID)).willReturn(true);
        given(vehicleRepository.findByEmployeeIdOrderByIdAsc(EMPLOYEE_ID))
                .willReturn(List.of(vehicle("1234ABC", "Seat")));

        List<EmployeeVehicleResponse> result = service().list(EMPLOYEE_ID);

        assertThat(result).singleElement()
                .satisfies(v -> assertThat(v.licensePlate()).isEqualTo("1234ABC"));
    }

    @Test
    void shouldThrowNotFound_whenListingForUnknownEmployee() {
        given(employeeRepository.existsById(EMPLOYEE_ID)).willReturn(false);

        assertThatThrownBy(() -> service().list(EMPLOYEE_ID))
                .isInstanceOf(EntityNotFoundException.class);
        verify(vehicleRepository, never()).findByEmployeeIdOrderByIdAsc(any());
    }

    @Test
    void shouldNormalizePlateAndNullifyBlanks_onCreate() {
        given(employeeRepository.existsById(EMPLOYEE_ID)).willReturn(true);
        given(vehicleRepository.existsByEmployeeIdAndLicensePlate(EMPLOYEE_ID, "1234ABC"))
                .willReturn(false);
        given(vehicleRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        service().create(EMPLOYEE_ID, new EmployeeVehicleRequest("  1234abc  ", "  ", "Leon", ""));

        ArgumentCaptor<EmployeeVehicle> captor = ArgumentCaptor.forClass(EmployeeVehicle.class);
        verify(vehicleRepository).save(captor.capture());
        EmployeeVehicle saved = captor.getValue();
        assertThat(saved.getLicensePlate()).isEqualTo("1234ABC");
        assertThat(saved.getBrand()).isNull();
        assertThat(saved.getModel()).isEqualTo("Leon");
        assertThat(saved.getColor()).isNull();
    }

    @Test
    void shouldCreateApproved_whenAdminCreates() {
        given(employeeRepository.existsById(EMPLOYEE_ID)).willReturn(true);
        given(vehicleRepository.existsByEmployeeIdAndLicensePlate(EMPLOYEE_ID, "1234ABC")).willReturn(false);
        given(vehicleRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        service().create(EMPLOYEE_ID, new EmployeeVehicleRequest("1234ABC", null, null, null));

        ArgumentCaptor<EmployeeVehicle> captor = ArgumentCaptor.forClass(EmployeeVehicle.class);
        verify(vehicleRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus())
                .isEqualTo(com.aleatica.parking.employee.VehicleStatus.APPROVED);
    }

    @Test
    void shouldCreatePending_whenEmployeeSelfCreates() {
        given(employeeRepository.existsById(EMPLOYEE_ID)).willReturn(true);
        given(vehicleRepository.existsByEmployeeIdAndLicensePlate(EMPLOYEE_ID, "1234ABC")).willReturn(false);
        given(vehicleRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        service().createForEmployee(EMPLOYEE_ID, new EmployeeVehicleRequest("1234ABC", null, null, null));

        ArgumentCaptor<EmployeeVehicle> captor = ArgumentCaptor.forClass(EmployeeVehicle.class);
        verify(vehicleRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus())
                .isEqualTo(com.aleatica.parking.employee.VehicleStatus.PENDING);
    }

    @Test
    void shouldRevalidateToPending_whenEmployeeSelfEdits() {
        EmployeeVehicle approved = EmployeeVehicle.create(
                EMPLOYEE_ID, "OLD123", "Seat", "Leon", "Gris",
                com.aleatica.parking.employee.VehicleStatus.APPROVED);
        approved.approve(9L, java.time.Instant.parse("2026-01-01T00:00:00Z"));
        given(employeeRepository.existsById(EMPLOYEE_ID)).willReturn(true);
        given(vehicleRepository.findByIdAndEmployeeId(VEHICLE_ID, EMPLOYEE_ID)).willReturn(Optional.of(approved));
        given(vehicleRepository.existsByEmployeeIdAndLicensePlateAndIdNot(EMPLOYEE_ID, "5678XYZ", VEHICLE_ID))
                .willReturn(false);
        given(vehicleRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        service().updateForEmployee(
                EMPLOYEE_ID, VEHICLE_ID, new EmployeeVehicleRequest("5678XYZ", null, null, null));

        assertThat(approved.getStatus()).isEqualTo(com.aleatica.parking.employee.VehicleStatus.PENDING);
        assertThat(approved.getRejectionReason()).isNull();
    }

    @Test
    void shouldThrowConflict_whenCreatingDuplicatePlate() {
        given(employeeRepository.existsById(EMPLOYEE_ID)).willReturn(true);
        given(vehicleRepository.existsByEmployeeIdAndLicensePlate(EMPLOYEE_ID, "1234ABC"))
                .willReturn(true);

        assertThatThrownBy(() ->
                service().create(EMPLOYEE_ID, new EmployeeVehicleRequest("1234ABC", null, null, null)))
                .isInstanceOf(EmployeeVehicleConflictException.class);
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void shouldThrowNotFound_whenCreatingForUnknownEmployee() {
        given(employeeRepository.existsById(EMPLOYEE_ID)).willReturn(false);

        assertThatThrownBy(() ->
                service().create(EMPLOYEE_ID, new EmployeeVehicleRequest("1234ABC", null, null, null)))
                .isInstanceOf(EntityNotFoundException.class);
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void shouldUpdateVehicle_whenBelongsToEmployee() {
        given(employeeRepository.existsById(EMPLOYEE_ID)).willReturn(true);
        given(vehicleRepository.findByIdAndEmployeeId(VEHICLE_ID, EMPLOYEE_ID))
                .willReturn(Optional.of(vehicle("OLD123", "Seat")));
        given(vehicleRepository.existsByEmployeeIdAndLicensePlateAndIdNot(EMPLOYEE_ID, "5678XYZ", VEHICLE_ID))
                .willReturn(false);
        given(vehicleRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        EmployeeVehicleResponse result = service().update(
                EMPLOYEE_ID, VEHICLE_ID, new EmployeeVehicleRequest("5678xyz", "Audi", null, "Negro"));

        assertThat(result.licensePlate()).isEqualTo("5678XYZ");
        assertThat(result.brand()).isEqualTo("Audi");
    }

    @Test
    void shouldThrowConflict_whenUpdatingToPlateOfAnotherVehicle() {
        given(employeeRepository.existsById(EMPLOYEE_ID)).willReturn(true);
        given(vehicleRepository.findByIdAndEmployeeId(VEHICLE_ID, EMPLOYEE_ID))
                .willReturn(Optional.of(vehicle("OLD123", "Seat")));
        given(vehicleRepository.existsByEmployeeIdAndLicensePlateAndIdNot(EMPLOYEE_ID, "5678XYZ", VEHICLE_ID))
                .willReturn(true);

        assertThatThrownBy(() -> service().update(
                EMPLOYEE_ID, VEHICLE_ID, new EmployeeVehicleRequest("5678XYZ", null, null, null)))
                .isInstanceOf(EmployeeVehicleConflictException.class);
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void shouldThrowNotFound_whenUpdatingForeignOrUnknownVehicle() {
        given(employeeRepository.existsById(EMPLOYEE_ID)).willReturn(true);
        given(vehicleRepository.findByIdAndEmployeeId(VEHICLE_ID, EMPLOYEE_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service().update(
                EMPLOYEE_ID, VEHICLE_ID, new EmployeeVehicleRequest("5678XYZ", null, null, null)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void shouldDeleteVehicle_whenBelongsToEmployee() {
        EmployeeVehicle existing = vehicle("1234ABC", "Seat");
        given(employeeRepository.existsById(EMPLOYEE_ID)).willReturn(true);
        given(vehicleRepository.findByIdAndEmployeeId(VEHICLE_ID, EMPLOYEE_ID))
                .willReturn(Optional.of(existing));

        service().delete(EMPLOYEE_ID, VEHICLE_ID);

        verify(vehicleRepository).delete(existing);
    }

    @Test
    void shouldThrowNotFound_whenDeletingForeignOrUnknownVehicle() {
        given(employeeRepository.existsById(EMPLOYEE_ID)).willReturn(true);
        given(vehicleRepository.findByIdAndEmployeeId(VEHICLE_ID, EMPLOYEE_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service().delete(EMPLOYEE_ID, VEHICLE_ID))
                .isInstanceOf(EntityNotFoundException.class);
        verify(vehicleRepository, never()).delete(any());
    }

    private EmployeeVehicle vehicle(String plate, String brand) {
        return EmployeeVehicle.create(
                EMPLOYEE_ID, plate, brand, "Leon", "Gris", com.aleatica.parking.employee.VehicleStatus.APPROVED);
    }
}
