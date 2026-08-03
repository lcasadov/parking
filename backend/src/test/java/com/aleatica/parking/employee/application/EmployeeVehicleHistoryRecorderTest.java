package com.aleatica.parking.employee.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.aleatica.parking.employee.EmployeeVehicle;
import com.aleatica.parking.employee.EmployeeVehicleHistory;
import com.aleatica.parking.employee.EmployeeVehicleHistoryRepository;
import com.aleatica.parking.employee.VehicleHistoryEventType;
import com.aleatica.parking.employee.VehicleStatus;
import com.aleatica.parking.employee.dto.EmployeeVehicleHistoryResponse;
import com.aleatica.parking.employee.dto.EmployeeVehicleHistoryResponse.PreviousData;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests unitarios de {@link EmployeeVehicleHistoryRecorder}: registro de alta/edición/cambio de
 * estado/solicitud de borrado (captura de la entidad guardada), snapshot de datos previos y
 * proyección del histórico a DTO con deserialización del snapshot. Repositorio mockeado;
 * {@link ObjectMapper} real.
 */
@ExtendWith(MockitoExtension.class)
class EmployeeVehicleHistoryRecorderTest {

    private static final Long VEHICLE_ID = 3L;
    private static final Long ACTOR_ID = 7L;

    @Mock
    private EmployeeVehicleHistoryRepository historyRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private EmployeeVehicleHistoryRecorder recorder() {
        return new EmployeeVehicleHistoryRecorder(historyRepository, objectMapper);
    }

    private EmployeeVehicleHistory captureSaved() {
        ArgumentCaptor<EmployeeVehicleHistory> captor = ArgumentCaptor.forClass(EmployeeVehicleHistory.class);
        verify(historyRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void shouldRecordCreatedByEmployee() {
        recorder().recordCreated(VEHICLE_ID, VehicleStatus.PENDING, ACTOR_ID, false);

        EmployeeVehicleHistory saved = captureSaved();
        assertThat(saved.getEventType()).isEqualTo(VehicleHistoryEventType.CREATED);
        assertThat(saved.getToStatus()).isEqualTo(VehicleStatus.PENDING);
        assertThat(saved.getActorRole()).isEqualTo("EMPLOYEE");
    }

    @Test
    void shouldRecordCreatedByAdmin() {
        recorder().recordCreated(VEHICLE_ID, VehicleStatus.APPROVED, ACTOR_ID, true);

        assertThat(captureSaved().getActorRole()).isEqualTo("ADMIN");
    }

    @Test
    void shouldRecordEditedWithPreviousSnapshot() {
        PreviousData previous = new PreviousData("OLD123", "Seat", "Leon", "Gris");

        recorder().recordEdited(VEHICLE_ID, previous, VehicleStatus.PENDING, ACTOR_ID);

        EmployeeVehicleHistory saved = captureSaved();
        assertThat(saved.getEventType()).isEqualTo(VehicleHistoryEventType.EDITED);
        assertThat(saved.getSnapshotJson()).contains("OLD123");
    }

    @Test
    void shouldRecordStatusChangeWithNote() {
        recorder().recordStatusChange(
                VEHICLE_ID, VehicleStatus.PENDING, VehicleStatus.REJECTED, "Matrícula ilegible", ACTOR_ID);

        EmployeeVehicleHistory saved = captureSaved();
        assertThat(saved.getEventType()).isEqualTo(VehicleHistoryEventType.STATUS_CHANGED);
        assertThat(saved.getFromStatus()).isEqualTo(VehicleStatus.PENDING);
        assertThat(saved.getToStatus()).isEqualTo(VehicleStatus.REJECTED);
        assertThat(saved.getNote()).isEqualTo("Matrícula ilegible");
    }

    @Test
    void shouldRecordDeletionRequested() {
        recorder().recordDeletionRequested(VEHICLE_ID, VehicleStatus.APPROVED, ACTOR_ID);

        EmployeeVehicleHistory saved = captureSaved();
        assertThat(saved.getEventType()).isEqualTo(VehicleHistoryEventType.DELETION_REQUESTED);
        assertThat(saved.getToStatus()).isEqualTo(VehicleStatus.PENDING_DELETION);
    }

    @Test
    void shouldBuildSnapshotOfVehicle() {
        EmployeeVehicle vehicle =
                EmployeeVehicle.create(1L, "1234ABC", "Seat", "Leon", "Gris", VehicleStatus.PENDING);

        PreviousData snapshot = recorder().snapshotOf(vehicle);

        assertThat(snapshot.licensePlate()).isEqualTo("1234ABC");
        assertThat(snapshot.brand()).isEqualTo("Seat");
    }

    @Test
    void shouldListHistoryDeserializingSnapshot() {
        EmployeeVehicleHistory edited = EmployeeVehicleHistory.of(
                VEHICLE_ID, VehicleHistoryEventType.EDITED, ACTOR_ID, "EMPLOYEE",
                null, VehicleStatus.PENDING, null,
                "{\"licensePlate\":\"OLD123\",\"brand\":\"Seat\",\"model\":\"Leon\",\"color\":\"Gris\"}");
        given(historyRepository.findByVehicleIdOrderByCreatedAtAscIdAsc(VEHICLE_ID))
                .willReturn(List.of(edited));

        List<EmployeeVehicleHistoryResponse> history = recorder().list(VEHICLE_ID);

        assertThat(history).singleElement().satisfies(entry -> {
            assertThat(entry.eventType()).isEqualTo(VehicleHistoryEventType.EDITED);
            assertThat(entry.previousData()).isNotNull();
            assertThat(entry.previousData().licensePlate()).isEqualTo("OLD123");
        });
    }

    @Test
    void shouldReturnNullPreviousData_whenNoSnapshot() {
        EmployeeVehicleHistory created = EmployeeVehicleHistory.of(
                VEHICLE_ID, VehicleHistoryEventType.CREATED, ACTOR_ID, "EMPLOYEE",
                null, VehicleStatus.PENDING, null, null);
        given(historyRepository.findByVehicleIdOrderByCreatedAtAscIdAsc(VEHICLE_ID))
                .willReturn(List.of(created));

        assertThat(recorder().list(VEHICLE_ID)).singleElement()
                .satisfies(entry -> assertThat(entry.previousData()).isNull());
    }
}
