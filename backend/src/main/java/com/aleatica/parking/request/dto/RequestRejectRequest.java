package com.aleatica.parking.request.dto;

import com.aleatica.parking.request.domain.RejectionReasonCode;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de entrada de {@code POST rejectRequest} (schema {@code RequestRejectRequest}
 * de la API).
 *
 * <p>El rechazo lleva siempre un {@code reasonCode} del catalogo. Cuando el codigo es
 * {@link RejectionReasonCode#OTHER} el texto libre {@code rejectionReason} (&ge;5
 * caracteres) es obligatorio; en otro caso es opcional. Esa regla cruzada la verifica
 * el caso de uso (400 sobre {@code rejectionReason}); aqui solo se acota la longitud
 * maxima. Las claves contractuales se fijan con {@link JsonProperty}: el codigo viaja
 * como {@code reasonCode} (no {@code rejectionReasonCode}, que es la clave de salida).</p>
 *
 * @param reasonCode      codigo del catalogo de rechazo (obligatorio)
 * @param rejectionReason texto libre; obligatorio (&ge;5) solo si {@code reasonCode = OTHER}
 */
@Schema(description = "Peticion de rechazo: codigo del catalogo + texto libre opcional")
public record RequestRejectRequest(
        @Schema(description = "Codigo del catalogo de rechazo", example = "NO_AVAILABILITY")
        @JsonProperty("reasonCode")
        @NotNull(message = "El codigo de motivo es obligatorio")
        RejectionReasonCode reasonCode,

        @Schema(description = "Texto libre; obligatorio (>=5) si reasonCode = OTHER")
        @JsonProperty("rejectionReason")
        @Size(max = 500, message = "El motivo no puede superar los 500 caracteres")
        String rejectionReason) {
}
