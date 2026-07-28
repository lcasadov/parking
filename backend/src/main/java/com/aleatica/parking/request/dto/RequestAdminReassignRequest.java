package com.aleatica.parking.request.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo de entrada de {@code POST adminReassignRequest} (schema
 * {@code RequestAdminReassignRequest} de la API, change
 * {@code reservas-employee-admin-reassign}, capability {@code admin-resource-reassignment}).
 *
 * <p>Reasignacion administrativa: cambia el recurso de una asignacion {@code APPROVED} de fecha
 * futura de un empleado a otro recurso libre del mismo tipo, para una fecha concreta (design §D7).
 * Ambos campos son obligatorios; el tipo del recurso destino se deriva de la propia solicitud (no
 * se acepta cruzar plaza con puesto).</p>
 *
 * @param requestId     solicitud {@code APPROVED} a reasignar (obligatorio)
 * @param newResourceId recurso destino (plaza/puesto) al que se reasigna (obligatorio)
 */
@Schema(description = "Peticion de reasignacion administrativa: solicitud y recurso destino")
public record RequestAdminReassignRequest(
        @Schema(description = "Solicitud APPROVED a reasignar", example = "42")
        @JsonProperty("requestId")
        @NotNull(message = "La solicitud es obligatoria")
        Long requestId,

        @Schema(description = "Recurso destino (plaza/puesto) al que se reasigna", example = "8")
        @JsonProperty("newResourceId")
        @NotNull(message = "El recurso destino es obligatorio")
        Long newResourceId) {
}
