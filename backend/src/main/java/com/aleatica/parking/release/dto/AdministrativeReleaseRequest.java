package com.aleatica.parking.release.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Cuerpo de entrada de {@code POST createAdministrativeRelease} (schema
 * {@code AdministrativeReleaseRequest} de la API).
 *
 * <p>A diferencia de la liberacion voluntaria, el {@code employeeId} y el
 * {@code parkingSpaceId} son explicitos (el {@code ADMIN} libera el recurso de otro) y
 * el {@code reason} es obligatorio ({@code @NotBlank}): la ausencia de motivo se traduce
 * a {@code 400} con {@code fields.reason}. La ventana temporal y la existencia de una
 * asignacion fija activa las verifica el caso de uso. Las claves contractuales se fijan
 * con {@link JsonProperty}.</p>
 *
 * @param employeeId     empleado titular cuyo recurso se libera (obligatorio)
 * @param parkingSpaceId recurso a liberar (obligatorio)
 * @param releaseDate    fecha a liberar (obligatoria, presente o futura)
 * @param reason         motivo de la liberacion administrativa (obligatorio)
 */
@Schema(description = "Peticion de liberacion administrativa de un recurso ajeno")
public record AdministrativeReleaseRequest(
        @Schema(description = "Empleado titular cuyo recurso se libera", example = "15")
        @JsonProperty("employeeId")
        @NotNull(message = "El empleado es obligatorio")
        Long employeeId,

        @Schema(description = "Recurso a liberar", example = "8")
        @JsonProperty("parkingSpaceId")
        @NotNull(message = "El recurso es obligatorio")
        Long parkingSpaceId,

        @Schema(description = "Fecha a liberar (ISO-8601)", example = "2026-07-10")
        @JsonProperty("releaseDate")
        @NotNull(message = "La fecha de liberacion es obligatoria")
        LocalDate releaseDate,

        @Schema(description = "Motivo de la liberacion administrativa", example = "Ausencia justificada")
        @JsonProperty("reason")
        @NotBlank(message = "El motivo es obligatorio en la liberacion administrativa")
        @Size(max = 500, message = "El motivo no puede exceder 500 caracteres")
        String reason) {
}
