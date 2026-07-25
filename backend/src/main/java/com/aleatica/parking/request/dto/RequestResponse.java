package com.aleatica.parking.request.dto;

import com.aleatica.parking.request.domain.RejectionReasonCode;
import com.aleatica.parking.request.domain.Request;
import com.aleatica.parking.request.domain.RequestStatus;
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
 * @param resourceType        tipo del recurso solicitado ({@code PARKING}/{@code DESK})
 * @param resourceNumber      numero humano del recurso asignado (plaza/puesto); {@code null}
 *                            salvo que la solicitud este {@code APPROVED} y el recurso se resuelva
 * @param floor               planta del recurso (solo {@code PARKING}); {@code null} si no aplica
 * @param lastRemindedAt      instante del ultimo reenvio de aviso al admin (change
 *                            {@code request-resend-notice}); {@code null} si nunca se reenvio
 * @param waitlisted          {@code true} si la solicitud esta en lista de espera (change
 *                            {@code waitlist-requests}); relevante solo mientras {@code PENDING}
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
        @JsonProperty("resourceType") ResourceType resourceType,

        @Schema(description = "Numero humano del recurso asignado (plaza/puesto); null si no "
                + "resuelto o la solicitud no esta APPROVED", example = "3005")
        @JsonProperty("resourceNumber") Integer resourceNumber,

        @Schema(description = "Planta del recurso (solo PARKING); null si no aplica", example = "3")
        @JsonProperty("floor") Integer floor,

        @Schema(description = "Instante del ultimo reenvio de aviso al admin; null si nunca se "
                + "reenvio")
        @JsonProperty("lastRemindedAt") Instant lastRemindedAt,

        @Schema(description = "En lista de espera (relevante solo mientras PENDING)",
                example = "false")
        @JsonProperty("waitlisted") boolean waitlisted) {

    /**
     * Constructor de compatibilidad previo a {@code lastRemindedAt} (change
     * {@code request-resend-notice}): delega en el canonico fijando {@code lastRemindedAt = null}
     * y {@code waitlisted = false}. Evita romper los llamantes existentes que aun construyen el
     * DTO con la aridad anterior.
     *
     * @param id                  identificador
     * @param employeeId          empleado solicitante
     * @param requestedDate       fecha solicitada
     * @param status              estado del ciclo de vida
     * @param parkingSpaceId      plaza asignada
     * @param approvalNote        nota del administrador al aprobar
     * @param rejectionReasonCode codigo del catalogo de rechazo
     * @param rejectionReason     texto libre del rechazo
     * @param resolvedById        empleado (ADMIN) que resolvio
     * @param resolvedAt          instante de resolucion
     * @param createdAt           instante de creacion
     * @param resourceType        tipo del recurso solicitado
     * @param resourceNumber      numero humano del recurso asignado
     * @param floor               planta del recurso
     */
    public RequestResponse(
            Long id, Long employeeId, LocalDate requestedDate, RequestStatus status,
            Long parkingSpaceId, String approvalNote, RejectionReasonCode rejectionReasonCode,
            String rejectionReason, Long resolvedById, Instant resolvedAt, Instant createdAt,
            ResourceType resourceType, Integer resourceNumber, Integer floor) {
        this(id, employeeId, requestedDate, status, parkingSpaceId, approvalNote,
                rejectionReasonCode, rejectionReason, resolvedById, resolvedAt, createdAt,
                resourceType, resourceNumber, floor, null, false);
    }

    /**
     * Constructor de compatibilidad previo a {@code waitlisted} (change
     * {@code waitlist-requests}): delega en el canonico fijando {@code waitlisted = false}. Evita
     * romper los llamantes existentes que aun construyen el DTO con la aridad anterior
     * (incluyendo {@code lastRemindedAt}).
     *
     * @param id                  identificador
     * @param employeeId          empleado solicitante
     * @param requestedDate       fecha solicitada
     * @param status              estado del ciclo de vida
     * @param parkingSpaceId      plaza asignada
     * @param approvalNote        nota del administrador al aprobar
     * @param rejectionReasonCode codigo del catalogo de rechazo
     * @param rejectionReason     texto libre del rechazo
     * @param resolvedById        empleado (ADMIN) que resolvio
     * @param resolvedAt          instante de resolucion
     * @param createdAt           instante de creacion
     * @param resourceType        tipo del recurso solicitado
     * @param resourceNumber      numero humano del recurso asignado
     * @param floor               planta del recurso
     * @param lastRemindedAt      instante del ultimo reenvio de aviso al admin
     */
    public RequestResponse(
            Long id, Long employeeId, LocalDate requestedDate, RequestStatus status,
            Long parkingSpaceId, String approvalNote, RejectionReasonCode rejectionReasonCode,
            String rejectionReason, Long resolvedById, Instant resolvedAt, Instant createdAt,
            ResourceType resourceType, Integer resourceNumber, Integer floor, Instant lastRemindedAt) {
        this(id, employeeId, requestedDate, status, parkingSpaceId, approvalNote,
                rejectionReasonCode, rejectionReason, resolvedById, resolvedAt, createdAt,
                resourceType, resourceNumber, floor, lastRemindedAt, false);
    }

    /**
     * Mapea el modelo de dominio a su DTO de salida (mapeo dominio&rarr;DTO en la capa web,
     * arquitectura hexagonal §D1).
     *
     * @param request modelo de dominio origen
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
                request.getResourceType(),
                null,
                null,
                request.getLastRemindedAt(),
                request.isWaitlisted());
    }

    /**
     * Devuelve una copia de este DTO con el numero humano del recurso (y su planta) ya
     * resueltos, para exponer al empleado el numero real de la plaza/puesto en lugar del
     * {@code resource_id} interno. El resto de campos se conservan intactos.
     *
     * @param resourceNumber numero humano del recurso asignado
     * @param floor          planta del recurso; {@code null} si el tipo no la define
     * @return una nueva instancia con {@code resourceNumber}/{@code floor} fijados
     */
    public RequestResponse withResource(Integer resourceNumber, Integer floor) {
        return new RequestResponse(
                id, employeeId, requestedDate, status, parkingSpaceId, approvalNote,
                rejectionReasonCode, rejectionReason, resolvedById, resolvedAt, createdAt,
                resourceType, resourceNumber, floor, lastRemindedAt, waitlisted);
    }
}
