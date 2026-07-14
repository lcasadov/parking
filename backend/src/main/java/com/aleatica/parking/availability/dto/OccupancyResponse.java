package com.aleatica.parking.availability.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/**
 * Ocupacion de recursos para una fecha: los recursos (plazas y puestos) OCUPADOS esa fecha con
 * su titular y origen. Base de la vista admin "Liberar por fecha".
 *
 * <p>Es un DTO, no la entidad JPA (S4684 / OWASP API3). Solo incluye los recursos ocupados (los
 * libres se omiten). Cada componente viaja bajo su clave contractual exacta fijada con
 * {@link JsonProperty}.</p>
 *
 * @param date              fecha consultada (ISO-8601)
 * @param occupiedResources recursos ocupados esa fecha (posiblemente vacia)
 */
@Schema(description = "Recursos ocupados para una fecha (plazas y puestos)")
public record OccupancyResponse(
        @Schema(description = "Fecha consultada (ISO-8601)", example = "2026-07-10")
        @JsonProperty("date") LocalDate date,

        @Schema(description = "Recursos ocupados para la fecha")
        @JsonProperty("occupiedResources") List<OccupancyItemResponse> occupiedResources) {
}
