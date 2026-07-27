package com.aleatica.parking.request.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo de entrada de {@code POST adminSwapRequests} (schema {@code RequestAdminSwapRequest} de
 * la API, change {@code reservas-employee-admin-reassign}, capability
 * {@code admin-resource-reassignment}).
 *
 * <p>Intercambio (swap) administrativo: intercambia los recursos de dos asignaciones
 * {@code APPROVED} de la MISMA fecha y del mismo tipo en una operacion atomica (design §D7). Ambos
 * identificadores son obligatorios y deben ser distintos (se rechaza intercambiar una solicitud
 * consigo misma con 409).</p>
 *
 * @param requestIdA primera solicitud {@code APPROVED} a intercambiar (obligatorio)
 * @param requestIdB segunda solicitud {@code APPROVED} a intercambiar (obligatorio)
 */
@Schema(description = "Peticion de intercambio (swap) administrativo entre dos solicitudes")
public record RequestAdminSwapRequest(
        @Schema(description = "Primera solicitud APPROVED a intercambiar", example = "42")
        @JsonProperty("requestIdA")
        @NotNull(message = "La primera solicitud es obligatoria")
        Long requestIdA,

        @Schema(description = "Segunda solicitud APPROVED a intercambiar", example = "43")
        @JsonProperty("requestIdB")
        @NotNull(message = "La segunda solicitud es obligatoria")
        Long requestIdB) {
}
