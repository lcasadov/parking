package com.aleatica.parking.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Motivo de rechazo de un vehículo por el administrador (change
 * {@code employee-vehicle-self-service}, Fase 2). Texto libre obligatorio.
 *
 * @param reason motivo del rechazo (obligatorio, máx. 500 caracteres)
 */
@Schema(description = "Motivo de rechazo de un vehículo (texto libre)")
public record EmployeeVehicleRejectRequest(
        @Schema(description = "Motivo del rechazo", example = "La matrícula no es legible")
        @NotBlank(message = "El motivo es obligatorio")
        @Size(max = 500, message = "El motivo no puede superar 500 caracteres")
        String reason) {
}
