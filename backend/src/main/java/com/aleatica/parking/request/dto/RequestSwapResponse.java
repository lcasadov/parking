package com.aleatica.parking.request.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Representacion de salida de un intercambio (swap) administrativo (schema
 * {@code RequestSwapResponse} de la API, change {@code reservas-employee-admin-reassign},
 * capability {@code admin-resource-reassignment}).
 *
 * <p>Transporta las dos solicitudes ya intercambiadas (ambas {@code APPROVED} con su nuevo
 * recurso), de modo que el frontend admin pueda refrescar las dos filas afectadas con una unica
 * respuesta. Son DTOs, nunca entidades JPA (S4684 / OWASP API3).</p>
 *
 * @param requestA primera solicitud, ya con el recurso de la segunda
 * @param requestB segunda solicitud, ya con el recurso de la primera
 */
@Schema(description = "Resultado de un intercambio de recursos: ambas solicitudes actualizadas")
public record RequestSwapResponse(
        @Schema(description = "Primera solicitud, ya con el recurso de la segunda")
        @JsonProperty("requestA") RequestResponse requestA,

        @Schema(description = "Segunda solicitud, ya con el recurso de la primera")
        @JsonProperty("requestB") RequestResponse requestB) {
}
