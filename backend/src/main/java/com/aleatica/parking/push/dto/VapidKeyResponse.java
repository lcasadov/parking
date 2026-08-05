package com.aleatica.parking.push.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Clave publica VAPID para que el cliente se suscriba (change {@code push-notifications}).
 *
 * @param publicKey clave publica VAPID en base64url; vacia si el canal push no esta configurado
 */
@Schema(description = "Clave publica VAPID")
public record VapidKeyResponse(
        @Schema(description = "Clave publica VAPID (base64url)") String publicKey) {
}
