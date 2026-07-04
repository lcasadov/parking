package com.aleatica.parking.availability.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/**
 * Disponibilidad de recursos para una fecha (schema {@code AvailabilityResponse} de la
 * API, autoridad {@code docs/openapi.yaml}).
 *
 * <p>Es un DTO, no la entidad JPA (S4684 / OWASP API3). Cada componente viaja bajo su
 * clave contractual exacta fijada con {@link JsonProperty}.</p>
 *
 * @param date               fecha consultada (ISO-8601)
 * @param availableResources plazas disponibles esa fecha (posiblemente vacia)
 */
@Schema(description = "Disponibilidad de recursos para una fecha")
public record AvailabilityResponse(
        @Schema(description = "Fecha consultada (ISO-8601)", example = "2026-07-10")
        @JsonProperty("date") LocalDate date,

        @Schema(description = "Plazas disponibles para la fecha")
        @JsonProperty("availableResources") List<AvailabilityItemResponse> availableResources) {
}
