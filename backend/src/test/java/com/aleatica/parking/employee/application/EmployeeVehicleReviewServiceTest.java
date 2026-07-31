package com.aleatica.parking.employee.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.aleatica.parking.auth.domain.ClockPort;
import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.employee.EmployeeVehicle;
import com.aleatica.parking.employee.EmployeeVehicleRepository;
import com.aleatica.parking.employee.VehicleStatus;
import com.aleatica.parking.employee.dto.EmployeeVehicleReviewResponse;
import com.aleatica.parking.employee.dto.PageResponse;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/**
 * Tests unitarios de {@link EmployeeVehicleReviewService} (Fase 2): transiciones de la bandeja de
 * validación (en trámite / aprobar / rechazar con motivo / confirmar borrado / restaurar), con sus
 * conflictos por estado (409), el aviso al empleado y el registro en el histórico. Repositorios,
 * recorder, notificador y reloj mockeados.
 */
@ExtendWith(MockitoExtension.class)
class EmployeeVehicleReviewServiceTest {

    private static final Long VEHICLE_ID = 3L;
    private static final Long EMPLOYEE_ID = 15L;
    private static final Long ADMIN_ID = 1L;
    private static final String ADMIN_LOGIN = "admin";
    private static final String PLATE = "1234ABC";
    private static final Instant NOW = Instant.parse("2026-07-31T10:00:00Z");

    @Mock
    private EmployeeVehicleRepository vehicleRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private EmployeeVehicleHistoryRecorder historyRecorder;
    @Mock
    private EmployeeVehicleAdminNotifier notifier;
    @Mock
    private ClockPort clock;

    private EmployeeVehicleReviewService service() {
        return new EmployeeVehicleReviewService(
                vehicleRepository, employeeRepository, historyRecorder, notifier, clock);
    }

    private EmployeeVehicle vehicle(VehicleStatus status) {
        return EmployeeVehicle.create(EMPLOYEE_ID, PLATE, "Seat", "Leon", "Gris", status);
    }

    private EmployeeVehicle pendingDeletionVehicle() {
        EmployeeVehicle vehicle = vehicle(VehicleStatus.APPROVED);
        vehicle.requestDeletion();
        return vehicle;
    }

    private void givenAdmin() {
        Employee admin = org.mockito.Mockito.mock(Employee.class);
        // getId() lo usan las transiciones que registran histórico; confirmDeletion no -> lenient.
        org.mockito.Mockito.lenient().when(admin.getId()).thenReturn(ADMIN_ID);
        given(employeeRepository.findByLogin(ADMIN_LOGIN)).willReturn(Optional.of(admin));
    }

    @Test
    void shouldApprovePendingVehicleAndNotifyEmployee() {
        EmployeeVehicle vehicle = vehicle(VehicleStatus.PENDING);
        given(vehicleRepository.findById(VEHICLE_ID)).willReturn(Optional.of(vehicle));
        givenAdmin();

        service().approve(VEHICLE_ID, ADMIN_LOGIN);

        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.APPROVED);
        verify(notifier).vehicleApproved(EMPLOYEE_ID, PLATE);
        verify(historyRecorder)
                .recordStatusChange(VEHICLE_ID, VehicleStatus.PENDING, VehicleStatus.APPROVED, null, ADMIN_ID);
    }

    @Test
    void shouldConflict_whenApprovingAlreadyApproved() {
        given(vehicleRepository.findById(VEHICLE_ID)).willReturn(Optional.of(vehicle(VehicleStatus.APPROVED)));

        assertThatThrownBy(() -> service().approve(VEHICLE_ID, ADMIN_LOGIN))
                .isInstanceOf(VehicleReviewConflictException.class);
        verify(notifier, never()).vehicleApproved(any(), any());
    }

    @Test
    void shouldRejectWithFreeTextReasonAndNotifyEmployee() {
        EmployeeVehicle vehicle = vehicle(VehicleStatus.IN_PROGRESS);
        given(vehicleRepository.findById(VEHICLE_ID)).willReturn(Optional.of(vehicle));
        givenAdmin();

        service().reject(VEHICLE_ID, "  Matrícula ilegible  ", ADMIN_LOGIN);

        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.REJECTED);
        assertThat(vehicle.getRejectionReason()).isEqualTo("Matrícula ilegible");
        verify(notifier).vehicleRejected(EMPLOYEE_ID, PLATE, "Matrícula ilegible");
        verify(historyRecorder).recordStatusChange(
                eq(VEHICLE_ID), eq(VehicleStatus.IN_PROGRESS), eq(VehicleStatus.REJECTED),
                eq("Matrícula ilegible"), eq(ADMIN_ID));
    }

    @Test
    void shouldMarkInProgressAndNotifyEmployee() {
        EmployeeVehicle vehicle = vehicle(VehicleStatus.PENDING);
        given(vehicleRepository.findById(VEHICLE_ID)).willReturn(Optional.of(vehicle));
        givenAdmin();

        service().markInProgress(VEHICLE_ID, ADMIN_LOGIN);

        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.IN_PROGRESS);
        verify(notifier).vehicleInProgress(EMPLOYEE_ID, PLATE);
    }

    @Test
    void shouldConflict_whenMarkingInProgressAnApproved() {
        given(vehicleRepository.findById(VEHICLE_ID)).willReturn(Optional.of(vehicle(VehicleStatus.APPROVED)));

        assertThatThrownBy(() -> service().markInProgress(VEHICLE_ID, ADMIN_LOGIN))
                .isInstanceOf(VehicleReviewConflictException.class);
    }

    @Test
    void shouldConfirmDeletionOfPendingDeletionVehicle() {
        EmployeeVehicle vehicle = pendingDeletionVehicle();
        given(vehicleRepository.findById(VEHICLE_ID)).willReturn(Optional.of(vehicle));
        givenAdmin();

        service().confirmDeletion(VEHICLE_ID, ADMIN_LOGIN);

        verify(vehicleRepository).delete(vehicle);
    }

    @Test
    void shouldConflict_whenConfirmingDeletionOfNonPendingDeletion() {
        given(vehicleRepository.findById(VEHICLE_ID)).willReturn(Optional.of(vehicle(VehicleStatus.APPROVED)));

        assertThatThrownBy(() -> service().confirmDeletion(VEHICLE_ID, ADMIN_LOGIN))
                .isInstanceOf(VehicleReviewConflictException.class);
        verify(vehicleRepository, never()).delete(any());
    }

    @Test
    void shouldRestorePendingDeletionToPreviousStatus() {
        EmployeeVehicle vehicle = pendingDeletionVehicle(); // previo = APPROVED
        given(vehicleRepository.findById(VEHICLE_ID)).willReturn(Optional.of(vehicle));
        givenAdmin();

        service().restore(VEHICLE_ID, ADMIN_LOGIN);

        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.APPROVED);
    }

    @Test
    void shouldChangeApprovedBackToPending_withoutNotifying() {
        EmployeeVehicle vehicle = vehicle(VehicleStatus.APPROVED);
        given(vehicleRepository.findById(VEHICLE_ID)).willReturn(Optional.of(vehicle));
        givenAdmin();

        service().changeStatus(VEHICLE_ID, VehicleStatus.PENDING, null, ADMIN_LOGIN);

        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.PENDING);
        verify(notifier, never()).vehicleApproved(any(), any());
        verify(historyRecorder)
                .recordStatusChange(VEHICLE_ID, VehicleStatus.APPROVED, VehicleStatus.PENDING, null, ADMIN_ID);
    }

    @Test
    void shouldChangeApprovedToRejected_withReasonAndNotify() {
        EmployeeVehicle vehicle = vehicle(VehicleStatus.APPROVED);
        given(vehicleRepository.findById(VEHICLE_ID)).willReturn(Optional.of(vehicle));
        givenAdmin();

        service().changeStatus(VEHICLE_ID, VehicleStatus.REJECTED, "  Aprobado por error  ", ADMIN_LOGIN);

        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.REJECTED);
        assertThat(vehicle.getRejectionReason()).isEqualTo("Aprobado por error");
        verify(notifier).vehicleRejected(EMPLOYEE_ID, PLATE, "Aprobado por error");
    }

    @Test
    void shouldRejectChangeToRejectedWithoutReason() {
        EmployeeVehicle vehicle = vehicle(VehicleStatus.APPROVED);
        given(vehicleRepository.findById(VEHICLE_ID)).willReturn(Optional.of(vehicle));
        givenAdmin();

        assertThatThrownBy(() -> service().changeStatus(VEHICLE_ID, VehicleStatus.REJECTED, "  ", ADMIN_LOGIN))
                .isInstanceOf(com.aleatica.parking.request.application.RejectionReasonRequiredException.class);
    }

    @Test
    void shouldConflict_whenTargetIsPendingDeletion() {
        assertThatThrownBy(() ->
                service().changeStatus(VEHICLE_ID, VehicleStatus.PENDING_DELETION, null, ADMIN_LOGIN))
                .isInstanceOf(VehicleReviewConflictException.class);
    }

    @Test
    void shouldConflict_whenChangingStatusOfPendingDeletionVehicle() {
        given(vehicleRepository.findById(VEHICLE_ID)).willReturn(Optional.of(pendingDeletionVehicle()));

        assertThatThrownBy(() ->
                service().changeStatus(VEHICLE_ID, VehicleStatus.APPROVED, null, ADMIN_LOGIN))
                .isInstanceOf(VehicleReviewConflictException.class);
    }

    @Test
    void shouldCountByStatusWithZeroDefaults() {
        EmployeeVehicleRepository.StatusCount pending =
                org.mockito.Mockito.mock(EmployeeVehicleRepository.StatusCount.class);
        given(pending.getStatus()).willReturn(VehicleStatus.PENDING);
        given(pending.getCount()).willReturn(3L);
        given(vehicleRepository.countGroupedByStatus()).willReturn(List.of(pending));

        var counts = service().countsByStatus();

        assertThat(counts.get(VehicleStatus.PENDING)).isEqualTo(3L);
        assertThat(counts.get(VehicleStatus.APPROVED)).isZero();
        assertThat(counts.get(VehicleStatus.PENDING_DELETION)).isZero();
    }

    @Test
    void shouldCountPendingAndPendingDeletion() {
        given(vehicleRepository.countByStatusIn(
                List.of(VehicleStatus.PENDING, VehicleStatus.PENDING_DELETION))).willReturn(5L);

        assertThat(service().pendingCount()).isEqualTo(5L);
    }

    @Test
    void shouldListByStatusWithEmployeeData() {
        EmployeeVehicle vehicle = vehicle(VehicleStatus.PENDING);
        given(vehicleRepository.findByStatus(eq(VehicleStatus.PENDING), any()))
                .willReturn(new PageImpl<>(List.of(vehicle)));
        Employee owner = org.mockito.Mockito.mock(Employee.class);
        given(owner.getId()).willReturn(EMPLOYEE_ID);
        given(owner.getFirstName()).willReturn("Ana");
        given(owner.getLastName()).willReturn("García");
        given(owner.getDepartment()).willReturn("IT");
        given(employeeRepository.findAllById(List.of(EMPLOYEE_ID))).willReturn(List.of(owner));

        PageResponse<EmployeeVehicleReviewResponse> page =
                service().list(List.of(VehicleStatus.PENDING), PageRequest.of(0, 20));

        assertThat(page.content()).singleElement().satisfies(row -> {
            assertThat(row.licensePlate()).isEqualTo(PLATE);
            assertThat(row.employee().fullName()).isEqualTo("Ana García");
            assertThat(row.status()).isEqualTo(VehicleStatus.PENDING);
        });
    }
}
