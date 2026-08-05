package com.aleatica.parking.employee.application;

import com.aleatica.parking.employee.EmployeeVehicle;
import com.aleatica.parking.employee.EmployeeVehicleHistory;
import com.aleatica.parking.employee.EmployeeVehicleHistoryRepository;
import com.aleatica.parking.employee.VehicleHistoryEventType;
import com.aleatica.parking.employee.VehicleStatus;
import com.aleatica.parking.employee.dto.EmployeeVehicleHistoryResponse;
import com.aleatica.parking.employee.dto.EmployeeVehicleHistoryResponse.PreviousData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra y consulta el histórico de cambios de un vehículo de empleado (change
 * {@code employee-vehicle-self-service}, Fase 2). Lo invocan los servicios que mutan el vehículo
 * (self-service del empleado y revisión del administrador). La foto de los datos previos de una
 * edición se serializa a JSON.
 */
@Service
public class EmployeeVehicleHistoryRecorder {

    private static final Logger log = LoggerFactory.getLogger(EmployeeVehicleHistoryRecorder.class);
    private static final String ROLE_EMPLOYEE = "EMPLOYEE";
    private static final String ROLE_ADMIN = "ADMIN";

    private final EmployeeVehicleHistoryRepository historyRepository;
    private final ObjectMapper objectMapper;

    public EmployeeVehicleHistoryRecorder(
            EmployeeVehicleHistoryRepository historyRepository, ObjectMapper objectMapper) {
        this.historyRepository = historyRepository;
        this.objectMapper = objectMapper;
    }

    /** Alta del vehículo (por el empleado o por el admin). */
    public void recordCreated(Long vehicleId, VehicleStatus toStatus, Long actorId, boolean byAdmin) {
        save(EmployeeVehicleHistory.of(
                vehicleId, VehicleHistoryEventType.CREATED, actorId, role(byAdmin),
                null, toStatus, null, null));
    }

    /** Edición del vehículo por el empleado, guardando la foto de los datos previos. */
    public void recordEdited(Long vehicleId, PreviousData previous, VehicleStatus toStatus, Long actorId) {
        save(EmployeeVehicleHistory.of(
                vehicleId, VehicleHistoryEventType.EDITED, actorId, ROLE_EMPLOYEE,
                null, toStatus, null, serialize(previous)));
    }

    /** Cambio de estado por el administrador (en trámite / aprobado / rechazado / restaurado). */
    public void recordStatusChange(
            Long vehicleId, VehicleStatus from, VehicleStatus to, String note, Long adminId) {
        save(EmployeeVehicleHistory.of(
                vehicleId, VehicleHistoryEventType.STATUS_CHANGED, adminId, ROLE_ADMIN,
                from, to, note, null));
    }

    /** El empleado ha solicitado el borrado (queda pendiente de borrado). */
    public void recordDeletionRequested(Long vehicleId, VehicleStatus from, Long actorId) {
        save(EmployeeVehicleHistory.of(
                vehicleId, VehicleHistoryEventType.DELETION_REQUESTED, actorId, ROLE_EMPLOYEE,
                from, VehicleStatus.PENDING_DELETION, null, null));
    }

    /** Foto de los datos actuales de un vehículo (para snapshot previo a una edición). */
    public PreviousData snapshotOf(EmployeeVehicle vehicle) {
        return new PreviousData(
                vehicle.getLicensePlate(), vehicle.getBrand(), vehicle.getModel(), vehicle.getColor());
    }

    @Transactional(readOnly = true)
    public List<EmployeeVehicleHistoryResponse> list(Long vehicleId) {
        return historyRepository.findByVehicleIdOrderByCreatedAtAscIdAsc(vehicleId).stream()
                .map(this::toResponse)
                .toList();
    }

    private void save(EmployeeVehicleHistory entry) {
        historyRepository.save(entry);
    }

    private static String role(boolean byAdmin) {
        return byAdmin ? ROLE_ADMIN : ROLE_EMPLOYEE;
    }

    private String serialize(PreviousData previous) {
        if (previous == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(previous);
        } catch (JsonProcessingException ex) {
            log.warn("[vehicle-history] No se pudo serializar el snapshot previo", ex);
            return null;
        }
    }

    private PreviousData deserialize(String snapshotJson) {
        if (snapshotJson == null) {
            return null;
        }
        try {
            return objectMapper.readValue(snapshotJson, PreviousData.class);
        } catch (JsonProcessingException ex) {
            log.warn("[vehicle-history] No se pudo leer el snapshot previo", ex);
            return null;
        }
    }

    private EmployeeVehicleHistoryResponse toResponse(EmployeeVehicleHistory entry) {
        return new EmployeeVehicleHistoryResponse(
                entry.getId(),
                entry.getEventType(),
                entry.getActorRole(),
                entry.getFromStatus(),
                entry.getToStatus(),
                entry.getNote(),
                deserialize(entry.getSnapshotJson()),
                entry.getCreatedAt());
    }
}
