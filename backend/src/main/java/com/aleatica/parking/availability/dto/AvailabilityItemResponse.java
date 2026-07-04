package com.aleatica.parking.availability.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Recurso disponible para una fecha (schema {@code AvailabilityItem} de la API,
 * autoridad {@code docs/openapi.yaml}).
 *
 * <p>Es un DTO, no la entidad JPA (S4684 / OWASP API3). Cada componente viaja bajo su
 * clave contractual exacta fijada con {@link JsonProperty}, de modo que Jackson no derive
 * otra clave del record.</p>
 *
 * @param parkingSpaceId identificador de la plaza disponible
 * @param label          etiqueta humana de la plaza
 */
@Schema(description = "Recurso (plaza) disponible para una fecha")
public record AvailabilityItemResponse(
        @Schema(description = "Identificador de la plaza", example = "8")
        @JsonProperty("parkingSpaceId") Long parkingSpaceId,

        @Schema(description = "Etiqueta de la plaza", example = "P-08")
        @JsonProperty("label") String label) {
}
