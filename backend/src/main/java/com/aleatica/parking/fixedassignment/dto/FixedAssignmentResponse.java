package com.aleatica.parking.fixedassignment.dto;

import com.aleatica.parking.fixedassignment.FixedAssignment;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Representacion de salida de una asignacion fija (schema {@code FixedAssignment}
 * de la API, autoridad {@code docs/openapi.yaml}).
 *
 * <p>Es un DTO, no la entidad JPA (S4684 / OWASP API3). Cada componente viaja bajo
 * su clave contractual exacta fijada con {@link JsonProperty}, de modo que Jackson
 * no derive otra clave del record (en particular {@code active}, para no reintroducir
 * la regresion del bug #21 con {@code isCorporate}).</p>
 *
 * @param id             identificador
 * @param parkingSpaceId plaza asignada
 * @param employeeId     empleado titular
 * @param dayOfWeek      dia de la semana (1=Lunes … 7=Domingo)
 * @param active         si la asignacion esta vigente
 * @param createdById    empleado (ADMIN) que la creo
 * @param createdAt      instante de alta (UTC)
 * @param revokedById    empleado (ADMIN) que la revoco; {@code null} si vigente
 * @param revokedAt      instante de revocacion (UTC); {@code null} si vigente
 */
@Schema(description = "Datos de una asignacion fija plaza/empleado por dia de la semana")
public record FixedAssignmentResponse(
        @Schema(description = "Identificador unico", example = "42")
        @JsonProperty("id") Long id,

        @Schema(description = "Plaza asignada", example = "8")
        @JsonProperty("parkingSpaceId") Long parkingSpaceId,

        @Schema(description = "Empleado titular", example = "15")
        @JsonProperty("employeeId") Long employeeId,

        @Schema(description = "Dia de la semana (1=Lunes … 7=Domingo)", example = "1")
        @JsonProperty("dayOfWeek") Integer dayOfWeek,

        @Schema(description = "Estado vigente de la asignacion", example = "true")
        @JsonProperty("active") boolean active,

        @Schema(description = "Empleado (ADMIN) que la creo", example = "1")
        @JsonProperty("createdById") Long createdById,

        @Schema(description = "Instante de alta (ISO-8601)")
        @JsonProperty("createdAt") Instant createdAt,

        @Schema(description = "Empleado (ADMIN) que la revoco; null si vigente")
        @JsonProperty("revokedById") Long revokedById,

        @Schema(description = "Instante de revocacion (ISO-8601); null si vigente")
        @JsonProperty("revokedAt") Instant revokedAt) {

    /**
     * Mapea la entidad de persistencia a su DTO de salida.
     *
     * @param assignment entidad origen
     * @return el DTO equivalente
     */
    public static FixedAssignmentResponse from(FixedAssignment assignment) {
        return new FixedAssignmentResponse(
                assignment.getId(),
                assignment.getResourceId(),
                assignment.getEmployeeId(),
                assignment.getDayOfWeek(),
                assignment.isActive(),
                assignment.getCreatedById(),
                assignment.getCreatedAt(),
                assignment.getRevokedById(),
                assignment.getRevokedAt());
    }
}
