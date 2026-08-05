package com.aleatica.parking.systemsettings.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo de entrada de {@code PUT updateNotificationChannels} (change {@code push-notifications}):
 * los dos interruptores GLOBALES de canal (email/push), independientes.
 *
 * <p>Se usan envoltorios {@link Boolean} con {@code @NotNull} para distinguir "ausente" de
 * {@code false}: el cliente debe enviar ambos valores explicitamente.</p>
 *
 * @param emailNotificationsEnabled {@code true} para que el canal email envie (obligatorio)
 * @param pushNotificationsEnabled  {@code true} para que el canal push envie (obligatorio)
 */
@Schema(description = "Peticion de cambio de los interruptores globales de canal (email/push)")
public record UpdateNotificationChannelsRequest(
        @Schema(description = "Si el canal email envia notificaciones", example = "true")
        @JsonProperty("emailNotificationsEnabled")
        @NotNull(message = "emailNotificationsEnabled es obligatorio")
        Boolean emailNotificationsEnabled,

        @Schema(description = "Si el canal push envia notificaciones", example = "true")
        @JsonProperty("pushNotificationsEnabled")
        @NotNull(message = "pushNotificationsEnabled es obligatorio")
        Boolean pushNotificationsEnabled) {
}
