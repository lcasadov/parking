package com.aleatica.parking.systemsettings.dto;

import com.aleatica.parking.systemsettings.domain.ApprovalMode;
import com.aleatica.parking.systemsettings.domain.SystemSettings;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Representacion de salida del ajuste global (schema {@code SystemSettings} de la API).
 *
 * <p>Es un DTO, no la entidad JPA (S4684 / OWASP API3). Cada componente viaja bajo su clave
 * contractual exacta fijada con {@link JsonProperty}. Expone el modo vigente y la trazabilidad
 * de la ultima modificacion (actor y marca de tiempo).</p>
 *
 * @param approvalMode modo de aprobacion global vigente
 * @param updatedById  empleado (ADMIN) que hizo el ultimo cambio; {@code null} si nunca
 * @param updatedAt    instante del ultimo cambio (ISO-8601, UTC); {@code null} si nunca
 */
@Schema(description = "Ajuste global del sistema (modo de aprobacion de solicitudes)")
public record SystemSettingsResponse(
        @Schema(description = "Modo de aprobacion global", example = "MANUAL")
        @JsonProperty("approvalMode") ApprovalMode approvalMode,

        @Schema(description = "Empleado (ADMIN) que hizo el ultimo cambio; null si nunca",
                example = "1")
        @JsonProperty("updatedById") Long updatedById,

        @Schema(description = "Instante del ultimo cambio (ISO-8601); null si nunca")
        @JsonProperty("updatedAt") Instant updatedAt) {

    /**
     * Mapea el modelo de dominio a su DTO de salida.
     *
     * @param settings modelo de dominio origen
     * @return el DTO equivalente
     */
    public static SystemSettingsResponse from(SystemSettings settings) {
        return new SystemSettingsResponse(
                settings.getApprovalMode(), settings.getUpdatedById(), settings.getUpdatedAt());
    }
}
