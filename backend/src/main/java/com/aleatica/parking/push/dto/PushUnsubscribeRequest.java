package com.aleatica.parking.push.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Cuerpo de baja de una suscripcion Web Push (change {@code push-notifications}).
 *
 * @param endpoint endpoint de la suscripcion a dar de baja
 */
@Schema(description = "Baja de una suscripcion Web Push por endpoint")
public record PushUnsubscribeRequest(
        @Schema(description = "Endpoint a dar de baja", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String endpoint) {
}
