package com.aleatica.parking.request.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de entrada de {@code POST adminCancelRequest} (schema
 * {@code RequestAdminCancelRequest} de la API).
 *
 * <p>La cancelacion administrativa de una solicitud {@code APPROVED} futura lleva siempre un
 * motivo obligatorio ({@code reason}), que queda trazado en auditoria junto al administrador
 * actor (change {@code release-occupied-resource}). El motivo es texto libre no vacio de entre
 * 5 y 500 caracteres, en linea con el motivo libre del rechazo {@code OTHER}. La ausencia o el
 * blanco los rechaza {@code @NotBlank} (400 sobre {@code reason}); la longitud, {@code @Size}.</p>
 *
 * @param reason motivo de la cancelacion administrativa (obligatorio, 5..500 caracteres)
 */
@Schema(description = "Peticion de cancelacion administrativa: motivo obligatorio")
public record RequestAdminCancelRequest(
        @Schema(description = "Motivo de la cancelacion administrativa (obligatorio)",
                example = "El empleado ya no necesita la plaza esa fecha")
        @JsonProperty("reason")
        @NotBlank(message = "El motivo es obligatorio")
        @Size(min = 5, max = 500, message = "El motivo debe tener entre 5 y 500 caracteres")
        String reason) {
}
