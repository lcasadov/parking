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
 * @param approvalMode      modo de aprobacion global vigente
 * @param parkingAddress    direccion postal del parking para "Ir al parking"; {@code null} si sin
 *                          configurar (change {@code reservas-employee-admin-reassign})
 * @param parkingLat        latitud del punto exacto del parking (mapa); {@code null} si sin fijar
 * @param parkingLng        longitud del punto exacto del parking; {@code null} si sin fijar
 * @param weekendReservable si se permiten reservas en fin de semana (change
 *                          {@code reservas-employee-admin-reassign}); por defecto {@code false}
 * @param updatedById       empleado (ADMIN) que hizo el ultimo cambio; {@code null} si nunca
 * @param updatedAt         instante del ultimo cambio (ISO-8601, UTC); {@code null} si nunca
 */
@Schema(description = "Ajuste global del sistema (aprobacion, direccion del parking, fin de semana)")
public record SystemSettingsResponse(
        @Schema(description = "Modo de aprobacion global", example = "MANUAL")
        @JsonProperty("approvalMode") ApprovalMode approvalMode,

        @Schema(description = "Direccion postal del parking (boton 'Ir al parking'); null si sin "
                + "configurar", example = "Av. de Europa 18, 28108 Alcobendas, Madrid")
        @JsonProperty("parkingAddress") String parkingAddress,

        @Schema(description = "Latitud del punto exacto del parking; null si sin fijar",
                example = "40.5405")
        @JsonProperty("parkingLat") Double parkingLat,

        @Schema(description = "Longitud del punto exacto del parking; null si sin fijar",
                example = "-3.6510")
        @JsonProperty("parkingLng") Double parkingLng,

        @Schema(description = "Si se permiten reservas en fin de semana (sabado/domingo)",
                example = "false")
        @JsonProperty("weekendReservable") boolean weekendReservable,

        @Schema(description = "Si el canal email envia notificaciones a nivel global", example = "true")
        @JsonProperty("emailNotificationsEnabled") boolean emailNotificationsEnabled,

        @Schema(description = "Si el canal push envia notificaciones a nivel global", example = "true")
        @JsonProperty("pushNotificationsEnabled") boolean pushNotificationsEnabled,

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
                settings.getApprovalMode(), settings.getParkingAddress(),
                settings.getParkingLat(), settings.getParkingLng(),
                settings.isWeekendReservable(), settings.isEmailNotificationsEnabled(),
                settings.isPushNotificationsEnabled(), settings.getUpdatedById(),
                settings.getUpdatedAt());
    }
}
