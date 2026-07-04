package com.aleatica.parking.request.dto;

import com.aleatica.parking.request.RejectionReasonCode;
import com.aleatica.parking.request.Request;
import com.aleatica.parking.request.RequestStatus;
import com.aleatica.parking.resource.ResourceType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Representacion de salida de una solicitud (schema {@code Request} de la API,
 * autoridad {@code docs/openapi.yaml}).
 *
 * <p>Es un DTO, no la entidad JPA (S4684 / OWASP API3). Cada componente viaja bajo
 * su clave contractual exacta fijada con {@link JsonProperty}, de modo que Jackson
 * no derive otra clave del record (blindaje del bug #21 con {@code isCorporate}).
 * Los enums {@code status} y {@code rejectionReasonCode} se serializan por su nombre
 * ({@code PENDING}, {@code NO_AVAILABILITY}, …).</p>
 *
 * @param id                  identificador
 * @param employeeId          empleado solicitante
 * @param requestedDate       fecha solicitada (ISO-8601 date)
 * @param status              estado del ciclo de vida
 * @param parkingSpaceId      plaza asignada; {@code null} mientras {@code PENDING}
 * @param approvalNote        nota del administrador al aprobar; {@code null} si no aplica
 * @param rejectionReasonCode codigo del catalogo de rechazo; {@code null} si no rechazada
 * @param rejectionReason     texto libre del rechazo; {@code null} si no aplica
 * @param resolvedById        empleado (ADMIN) que resolvio; {@code null} si sin resolver
 * @param resolvedAt          instante de resolucion (UTC); {@code null} si sin resolver
 * @param createdAt           instante de creacion (UTC)
 */
@Schema(description = "Datos de una solicitud puntual de plaza")
public record RequestResponse(
        @Schema(description = "Identificador unico", example = "42")
        @JsonProperty("id") Long id,

        @Schema(description = "Empleado solicitante", example = "15")
        @JsonProperty("employeeId") Long employeeId,

        @Schema(description = "Fecha solicitada (ISO-8601)", example = "2026-07-10")
        @JsonProperty("requestedDate") LocalDate requestedDate,

        @Schema(description = "Estado del ciclo de vida", example = "PENDING")
        @JsonProperty("status") RequestStatus status,

        @Schema(description = "Plaza asignada; null mientras PENDING", example = "8")
        @JsonProperty("parkingSpaceId") Long parkingSpaceId,

        @Schema(description = "Nota del administrador al aprobar; null si no aplica")
        @JsonProperty("approvalNote") String approvalNote,

        @Schema(description = "Codigo del catalogo de rechazo; null si no rechazada")
        @JsonProperty("rejectionReasonCode") RejectionReasonCode rejectionReasonCode,

        @Schema(description = "Texto libre del rechazo; null si no aplica")
        @JsonProperty("rejectionReason") String rejectionReason,

        @Schema(description = "Empleado (ADMIN) que resolvio; null si sin resolver", example = "1")
        @JsonProperty("resolvedById") Long resolvedById,

        @Schema(description = "Instante de resolucion (ISO-8601); null si sin resolver")
        @JsonProperty("resolvedAt") Instant resolvedAt,

        @Schema(description = "Instante de creacion (ISO-8601)")
        @JsonProperty("createdAt") Instant createdAt,

        @Schema(description = "Tipo de recurso solicitado", example = "PARKING")
        @JsonProperty("resourceType") ResourceType resourceType) {

    /**
     * Mapea la entidad de persistencia a su DTO de salida.
     *
     * @param request entidad origen
     * @return el DTO equivalente
     */
    public static RequestResponse from(Request request) {
        return new RequestResponse(
                request.getId(),
                request.getEmployeeId(),
                request.getRequestedDate(),
                request.getStatus(),
                request.getResourceId(),
                request.getApprovalNote(),
                request.getRejectionReasonCode(),
                request.getRejectionReason(),
                request.getResolvedById(),
                request.getResolvedAt(),
                request.getCreatedAt(),
                request.getResourceType());
    }
}
