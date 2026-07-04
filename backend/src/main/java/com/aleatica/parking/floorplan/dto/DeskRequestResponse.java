package com.aleatica.parking.floorplan.dto;

import com.aleatica.parking.floorplan.FloorPlanDeskState;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resultado de solicitar un puesto desde el plano (schema {@code DeskRequestResponse} de la
 * API): el identificador de la solicitud creada y el nuevo estado del puesto.
 *
 * <p>Es un DTO, no la entidad JPA (S4684 / OWASP API3). Tras crear la solicitud pendiente el
 * puesto pasa a {@link FloorPlanDeskState#MINE} para el solicitante (lo tiene solicitado);
 * el resto de empleados lo veran como {@link FloorPlanDeskState#REQUESTED}. Las claves
 * contractuales se fijan con {@link JsonProperty}.</p>
 *
 * @param requestId identificador de la solicitud creada
 * @param deskId    identificador del puesto solicitado
 * @param state     nuevo estado del puesto para el solicitante ({@code MINE})
 */
@Schema(description = "Resultado de solicitar un puesto desde el plano")
public record DeskRequestResponse(
        @Schema(description = "Identificador de la solicitud creada", example = "128")
        @JsonProperty("requestId") Long requestId,

        @Schema(description = "Identificador del puesto solicitado", example = "42")
        @JsonProperty("deskId") Long deskId,

        @Schema(description = "Nuevo estado del puesto para el solicitante", example = "MINE")
        @JsonProperty("state") FloorPlanDeskState state) {
}
