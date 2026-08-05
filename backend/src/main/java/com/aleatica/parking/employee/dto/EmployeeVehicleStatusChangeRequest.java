package com.aleatica.parking.employee.dto;

import com.aleatica.parking.employee.VehicleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cambio manual de estado de un vehículo por el administrador (change
 * {@code employee-vehicle-self-service}, Fase 2): permite corregir un estado (p.ej. un aprobado por
 * error) pasándolo a otro. El {@code reason} es obligatorio (texto libre) si el destino es
 * {@code REJECTED}.
 *
 * @param status estado destino (PENDING / IN_PROGRESS / APPROVED / REJECTED)
 * @param reason motivo (obligatorio solo si el destino es REJECTED)
 */
@Schema(description = "Cambio manual de estado de un vehículo")
public record EmployeeVehicleStatusChangeRequest(
        @Schema(description = "Estado destino", example = "PENDING")
        @NotNull(message = "El estado es obligatorio")
        VehicleStatus status,

        @Schema(description = "Motivo (obligatorio si el destino es REJECTED)")
        @Size(max = 500, message = "El motivo no puede superar 500 caracteres")
        String reason) {
}
