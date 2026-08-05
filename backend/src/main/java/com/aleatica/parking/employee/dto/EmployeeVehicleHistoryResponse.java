package com.aleatica.parking.employee.dto;

import com.aleatica.parking.employee.VehicleHistoryEventType;
import com.aleatica.parking.employee.VehicleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Entrada del histórico de un vehículo para la capa web (change
 * {@code employee-vehicle-self-service}, Fase 2). DTO, nunca la entidad JPA (S4684).
 *
 * @param id           identificador de la entrada
 * @param eventType    tipo de evento (alta, edición, cambio de estado, solicitud de borrado)
 * @param actorRole    rol de quien lo hizo ({@code EMPLOYEE} / {@code ADMIN})
 * @param fromStatus   estado previo (en cambios de estado / solicitud de borrado)
 * @param toStatus     estado resultante
 * @param note         nota (p.ej. motivo del rechazo)
 * @param previousData datos previos del vehículo (solo en una edición); {@code null} si no aplica
 * @param createdAt    instante del evento
 */
@Schema(description = "Entrada del histórico de un vehículo de empleado")
public record EmployeeVehicleHistoryResponse(
        Long id,
        VehicleHistoryEventType eventType,
        String actorRole,
        VehicleStatus fromStatus,
        VehicleStatus toStatus,
        String note,
        PreviousData previousData,
        Instant createdAt) {

    /** Foto de los datos del vehículo antes de una edición. */
    public record PreviousData(String licensePlate, String brand, String model, String color) {
    }
}
