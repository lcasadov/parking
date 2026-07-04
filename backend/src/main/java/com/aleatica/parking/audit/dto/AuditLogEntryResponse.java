package com.aleatica.parking.audit.dto;

import com.aleatica.parking.audit.AuditLog;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Representacion de salida de una entrada de auditoria (schema {@code AuditLogEntry} de la
 * API, autoridad {@code docs/openapi.yaml}).
 *
 * <p>Es un DTO, no la entidad JPA (S4684 / OWASP API3). Cada componente viaja bajo su clave
 * contractual exacta fijada con {@link JsonProperty}, de modo que Jackson no derive otra
 * clave del record. No expone datos personales: solo identificadores, accion y el
 * {@code details} JSON ya saneado en el registro.</p>
 *
 * @param id              identificador
 * @param actorEmployeeId empleado actor; {@code null} en acciones del sistema
 * @param action          accion funcional registrada
 * @param entityType      tipo de entidad afectada
 * @param entityId        entidad afectada; {@code null} si no aplica
 * @param details         detalle JSON enriquecido; {@code null} si no aplica
 * @param occurredAt      instante de la accion (UTC)
 */
@Schema(description = "Entrada del rastro de auditoria funcional")
public record AuditLogEntryResponse(
        @Schema(description = "Identificador unico", example = "42")
        @JsonProperty("id") Long id,

        @Schema(description = "Empleado que ejecuto la accion; null si la origino el sistema",
                example = "7")
        @JsonProperty("actorEmployeeId") Long actorEmployeeId,

        @Schema(description = "Accion funcional registrada", example = "APPROVE_REQUEST")
        @JsonProperty("action") String action,

        @Schema(description = "Tipo de entidad afectada", example = "Request")
        @JsonProperty("entityType") String entityType,

        @Schema(description = "Entidad afectada; null si no aplica", example = "15")
        @JsonProperty("entityId") Long entityId,

        @Schema(description = "Detalle JSON enriquecido; null si no aplica")
        @JsonProperty("details") String details,

        @Schema(description = "Instante de la accion (ISO-8601)")
        @JsonProperty("occurredAt") Instant occurredAt) {

    /**
     * Mapea la entidad de persistencia a su DTO de salida.
     *
     * @param entry entrada de auditoria origen
     * @return el DTO equivalente
     */
    public static AuditLogEntryResponse from(AuditLog entry) {
        return new AuditLogEntryResponse(
                entry.getId(), entry.getActorEmployeeId(), entry.getAction(),
                entry.getEntityType(), entry.getEntityId(), entry.getDetails(),
                entry.getOccurredAt());
    }
}
