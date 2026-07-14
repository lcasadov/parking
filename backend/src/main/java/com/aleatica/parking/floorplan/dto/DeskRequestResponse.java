package com.aleatica.parking.floorplan.dto;

import com.aleatica.parking.floorplan.FloorPlanDeskState;
import com.aleatica.parking.request.domain.RequestStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resultado de solicitar un puesto desde el plano (schema {@code DeskRequestResponse} de la
 * API): el identificador de la solicitud creada, el nuevo estado del puesto y el estado de
 * ciclo de vida resultante de la solicitud.
 *
 * <p>Es un DTO, no la entidad JPA (S4684 / OWASP API3). Tras crear la solicitud el puesto pasa a
 * {@link FloorPlanDeskState#MINE} para el solicitante (lo tiene solicitado o asignado); el resto
 * de empleados lo veran como {@link FloorPlanDeskState#REQUESTED} o {@link FloorPlanDeskState#ASSIGNED}.
 * El campo {@code status} refleja el estado de la solicitud creada, que ramifica por el modo de
 * aprobacion global (change {@code request-desk-selection}): {@link RequestStatus#PENDING} en modo
 * {@code MANUAL} y {@link RequestStatus#APPROVED} en modo {@code AUTOMATIC} (auto-aprobacion del
 * puesto pinchado), para que el plano ajuste el mensaje de confirmacion. Las claves contractuales
 * se fijan con {@link JsonProperty}.</p>
 *
 * @param requestId identificador de la solicitud creada
 * @param deskId    identificador del puesto solicitado
 * @param state     nuevo estado del puesto para el solicitante ({@code MINE})
 * @param status    estado de la solicitud creada ({@code PENDING} en manual, {@code APPROVED} en automatico)
 */
@Schema(description = "Resultado de solicitar un puesto desde el plano")
public record DeskRequestResponse(
        @Schema(description = "Identificador de la solicitud creada", example = "128")
        @JsonProperty("requestId") Long requestId,

        @Schema(description = "Identificador del puesto solicitado", example = "42")
        @JsonProperty("deskId") Long deskId,

        @Schema(description = "Nuevo estado del puesto para el solicitante", example = "MINE")
        @JsonProperty("state") FloorPlanDeskState state,

        @Schema(description = "Estado de la solicitud creada (PENDING en manual, APPROVED en automatico)",
                example = "PENDING")
        @JsonProperty("status") RequestStatus status) {
}
