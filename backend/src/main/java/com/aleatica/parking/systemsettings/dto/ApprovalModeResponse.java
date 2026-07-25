package com.aleatica.parking.systemsettings.dto;

import com.aleatica.parking.systemsettings.domain.ApprovalMode;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Representacion de salida minima del modo de aprobacion global (schema
 * {@code ApprovalModeResponse} de la API), legible por <strong>cualquier</strong> empleado
 * autenticado (change {@code request-auto-assignment}: un {@code EMPLOYEE} necesita saber si su
 * solicitud sera automatica o pendiente).
 *
 * <p>A diferencia de {@link SystemSettingsResponse} (reservado a {@code ADMIN}), este DTO
 * <strong>no</strong> expone la trazabilidad del ultimo cambio ({@code updatedById}/
 * {@code updatedAt}): solo el dato que necesita un no-admin.</p>
 *
 * @param approvalMode modo de aprobacion global vigente
 */
@Schema(description = "Modo de aprobacion global vigente, sin trazabilidad (legible por "
        + "cualquier empleado autenticado)")
public record ApprovalModeResponse(
        @Schema(description = "Modo de aprobacion global", example = "MANUAL")
        @JsonProperty("approvalMode") ApprovalMode approvalMode) {

    /**
     * Crea la respuesta a partir del modo vigente.
     *
     * @param approvalMode modo de aprobacion global vigente
     * @return el DTO equivalente
     */
    public static ApprovalModeResponse of(ApprovalMode approvalMode) {
        return new ApprovalModeResponse(approvalMode);
    }
}
