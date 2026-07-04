package com.aleatica.parking.availability.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Fila del calendario semanal admin: una plaza con sus celdas por dia (schema
 * {@code CalendarRow} de la API, autoridad {@code docs/openapi.yaml}).
 *
 * <p>Es un DTO, no la entidad JPA (S4684 / OWASP API3). Cada componente viaja bajo su
 * clave contractual exacta fijada con {@link JsonProperty}.</p>
 *
 * @param parkingSpaceId identificador de la plaza de la fila
 * @param label          etiqueta humana de la plaza
 * @param cells          celdas por dia (una por dia de la semana)
 */
@Schema(description = "Fila del calendario semanal admin (una plaza y sus dias)")
public record CalendarRowResponse(
        @Schema(description = "Identificador de la plaza", example = "8")
        @JsonProperty("parkingSpaceId") Long parkingSpaceId,

        @Schema(description = "Etiqueta de la plaza", example = "P-08")
        @JsonProperty("label") String label,

        @Schema(description = "Celdas por dia de la semana")
        @JsonProperty("cells") List<CalendarCellResponse> cells) {
}
