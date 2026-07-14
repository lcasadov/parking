package com.aleatica.parking.systemsettings.dto;

import com.aleatica.parking.systemsettings.domain.ApprovalMode;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo de entrada de {@code PUT updateSystemSettings} (schema
 * {@code UpdateApprovalModeRequest} de la API).
 *
 * <p>La validacion sintactica ({@code @NotNull}) es la primera capa (UX); un valor fuera del
 * dominio del enum se rechaza con {@code 400} en la deserializacion (manejador global de
 * cuerpo no legible). La clave contractual {@code approvalMode} se fija con
 * {@link JsonProperty}.</p>
 *
 * @param approvalMode nuevo modo de aprobacion global (obligatorio)
 */
@Schema(description = "Peticion de cambio del modo de aprobacion global")
public record UpdateApprovalModeRequest(
        @Schema(description = "Nuevo modo de aprobacion global", example = "AUTOMATIC")
        @JsonProperty("approvalMode")
        @NotNull(message = "El modo de aprobacion es obligatorio")
        ApprovalMode approvalMode) {
}
