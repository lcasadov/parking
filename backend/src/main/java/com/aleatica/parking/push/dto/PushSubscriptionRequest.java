package com.aleatica.parking.push.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo de alta de una suscripcion Web Push (change {@code push-notifications}): el objeto que
 * produce {@code PushManager.subscribe(...).toJSON()} en el navegador (endpoint + claves).
 *
 * @param endpoint URL del push service (unica)
 * @param keys     claves publicas del cliente ({@code p256dh}/{@code auth})
 */
@Schema(description = "Suscripcion Web Push del navegador (endpoint + claves)")
public record PushSubscriptionRequest(
        @Schema(description = "Endpoint del push service", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String endpoint,

        @Schema(description = "Claves del cliente", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Valid Keys keys) {

    /**
     * Claves criptograficas del cliente para cifrar el payload.
     *
     * @param p256dh clave publica (base64url)
     * @param auth   secreto de autenticacion (base64url)
     */
    public record Keys(
            @NotBlank String p256dh,
            @NotBlank String auth) {
    }
}
