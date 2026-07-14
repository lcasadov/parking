package com.aleatica.parking.availability.dto;

import com.aleatica.parking.availability.OccupancyOrigin;
import com.aleatica.parking.resource.ResourceType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Recurso OCUPADO para una fecha en la vista "Liberar por fecha" (plaza o puesto), con su
 * titular y el origen de la ocupacion.
 *
 * <p>Es un DTO, no la entidad JPA (S4684 / OWASP API3). Cada componente viaja bajo su clave
 * contractual exacta fijada con {@link JsonProperty}. {@code floor} es {@code null} para los
 * tipos sin planta derivada (puestos); {@code requestId} es {@code null} cuando el origen es
 * una asignacion fija.</p>
 *
 * @param resourceType   tipo del recurso ({@code PARKING}/{@code DESK})
 * @param resourceId     identificador interno del recurso ({@code resource_id})
 * @param resourceNumber numero humano del recurso (p. ej. {@code 3005} plaza / {@code 12} puesto)
 * @param floor          planta del recurso; {@code null} si el tipo no la define (puestos)
 * @param employeeId     empleado que ocupa el recurso esa fecha
 * @param employeeName   nombre del empleado que ocupa el recurso
 * @param origin         origen de la ocupacion (asignacion fija / solicitud aprobada)
 * @param requestId      solicitud aprobada que ocupa el recurso; {@code null} si es asignacion fija
 */
@Schema(description = "Recurso ocupado para una fecha (plaza o puesto) con titular y origen")
public record OccupancyItemResponse(
        @Schema(description = "Tipo del recurso", example = "PARKING")
        @JsonProperty("resourceType") ResourceType resourceType,

        @Schema(description = "Identificador interno del recurso", example = "8")
        @JsonProperty("resourceId") Long resourceId,

        @Schema(description = "Numero humano del recurso", example = "3005")
        @JsonProperty("resourceNumber") Integer resourceNumber,

        @Schema(description = "Planta del recurso; null si no aplica (puestos)", example = "3")
        @JsonProperty("floor") Integer floor,

        @Schema(description = "Empleado que ocupa el recurso esa fecha", example = "15")
        @JsonProperty("employeeId") Long employeeId,

        @Schema(description = "Nombre del empleado que ocupa el recurso", example = "Ada Lovelace")
        @JsonProperty("employeeName") String employeeName,

        @Schema(description = "Origen de la ocupacion", example = "FIXED_ASSIGNMENT")
        @JsonProperty("origin") OccupancyOrigin origin,

        @Schema(description = "Solicitud aprobada que ocupa el recurso; null si es asignacion fija",
                example = "42")
        @JsonProperty("requestId") Long requestId) {
}
