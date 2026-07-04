package com.aleatica.parking.audit.dto;

import com.aleatica.parking.audit.LoginLog;
import com.aleatica.parking.auth.domain.LoginPhase;
import com.aleatica.parking.auth.domain.LoginResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Representacion de salida de una entrada del registro de logins (schema
 * {@code LoginLogEntry} de la API, autoridad {@code docs/openapi.yaml}).
 *
 * <p>Es un DTO, no la entidad JPA (S4684 / OWASP API3). Cada componente viaja bajo su clave
 * contractual exacta fijada con {@link JsonProperty}. No expone contrasenas ni datos
 * sensibles: solo el login intentado, el resultado, la fase y los metadatos de la peticion.</p>
 *
 * @param id             identificador
 * @param loginAttempted login tal cual lo intento el usuario
 * @param employeeId     empleado resuelto; {@code null} si el login no existia
 * @param result         resultado del intento
 * @param phase          fase de autenticacion
 * @param ipAddress      IP de origen; {@code null} si no se registro
 * @param userAgent      user-agent del cliente; {@code null} si no se registro
 * @param occurredAt     instante del intento (UTC)
 */
@Schema(description = "Entrada del registro de intentos de login")
public record LoginLogEntryResponse(
        @Schema(description = "Identificador unico", example = "42")
        @JsonProperty("id") Long id,

        @Schema(description = "Login intentado", example = "jperez")
        @JsonProperty("loginAttempted") String loginAttempted,

        @Schema(description = "Empleado resuelto; null si el login no existia", example = "7")
        @JsonProperty("employeeId") Long employeeId,

        @Schema(description = "Resultado del intento", example = "INVALID_CREDENTIALS")
        @JsonProperty("result") LoginResult result,

        @Schema(description = "Fase de autenticacion", example = "PHASE_1")
        @JsonProperty("phase") LoginPhase phase,

        @Schema(description = "IP de origen; null si no se registro", example = "10.0.0.5")
        @JsonProperty("ipAddress") String ipAddress,

        @Schema(description = "User-agent del cliente; null si no se registro")
        @JsonProperty("userAgent") String userAgent,

        @Schema(description = "Instante del intento (ISO-8601)")
        @JsonProperty("occurredAt") Instant occurredAt) {

    /**
     * Mapea la entidad de persistencia a su DTO de salida.
     *
     * @param entry entrada del registro de logins origen
     * @return el DTO equivalente
     */
    public static LoginLogEntryResponse from(LoginLog entry) {
        return new LoginLogEntryResponse(
                entry.getId(), entry.getLoginAttempted(), entry.getEmployeeId(),
                entry.getResult(), entry.getPhase(), entry.getIpAddress(),
                entry.getUserAgent(), entry.getOccurredAt());
    }
}
