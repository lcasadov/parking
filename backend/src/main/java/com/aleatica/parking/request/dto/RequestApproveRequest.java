package com.aleatica.parking.request.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de entrada de {@code POST approveRequest} (schema {@code RequestApproveRequest}
 * de la API).
 *
 * <p>La plaza es obligatoria; la nota es opcional (maximo 500 caracteres) y viaja en
 * el email de aprobacion. La disponibilidad real de la plaza para la fecha la verifica
 * el caso de uso dentro de la transaccion. Las claves contractuales se fijan con
 * {@link JsonProperty}.</p>
 *
 * @param parkingSpaceId plaza a asignar (obligatoria)
 * @param approvalNote   nota libre del administrador (opcional, &le;500)
 */
@Schema(description = "Peticion de aprobacion: plaza a asignar + nota opcional")
public record RequestApproveRequest(
        @Schema(description = "Plaza a asignar", example = "8")
        @JsonProperty("parkingSpaceId")
        @NotNull(message = "La plaza es obligatoria")
        Long parkingSpaceId,

        @Schema(description = "Nota opcional del admin; viaja en el email de aprobacion")
        @JsonProperty("approvalNote")
        @Size(max = 500, message = "La nota no puede superar los 500 caracteres")
        String approvalNote) {
}
