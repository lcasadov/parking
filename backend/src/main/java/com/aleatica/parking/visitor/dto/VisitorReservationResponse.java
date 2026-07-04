package com.aleatica.parking.visitor.dto;

import com.aleatica.parking.visitor.VisitorReservation;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Representacion de salida de una reserva de visitante (schema {@code VisitorReservation}
 * de la API, autoridad {@code docs/openapi.yaml}).
 *
 * <p>Es un DTO, no la entidad JPA (S4684 / OWASP API3). Cada componente viaja bajo su
 * clave contractual exacta fijada con {@link JsonProperty}. Referencia al visitante por
 * id (no denormaliza sus campos), de modo que una edicion de la ficha nunca reescribe
 * datos de reservas ya creadas.</p>
 *
 * @param id             identificador
 * @param visitorId      visitante reservado
 * @param parkingSpaceId plaza ocupada
 * @param reservationDate fecha reservada (ISO-8601 date)
 * @param notes          anotaciones; {@code null} si no aplica
 * @param createdById    {@code ADMIN} que creo la reserva
 * @param createdAt      instante de creacion (UTC)
 */
@Schema(description = "Datos de una reserva de plaza para un visitante")
public record VisitorReservationResponse(
        @Schema(description = "Identificador unico", example = "7")
        @JsonProperty("id") Long id,

        @Schema(description = "Visitante reservado", example = "42")
        @JsonProperty("visitorId") Long visitorId,

        @Schema(description = "Plaza ocupada", example = "8")
        @JsonProperty("parkingSpaceId") Long parkingSpaceId,

        @Schema(description = "Fecha reservada (ISO-8601)", example = "2026-07-10")
        @JsonProperty("reservationDate") LocalDate reservationDate,

        @Schema(description = "Anotaciones; null si no aplica", example = "Acceso puerta norte")
        @JsonProperty("notes") String notes,

        @Schema(description = "ADMIN que creo la reserva", example = "1")
        @JsonProperty("createdById") Long createdById,

        @Schema(description = "Instante de creacion (ISO-8601)")
        @JsonProperty("createdAt") Instant createdAt) {

    /**
     * Mapea la entidad de persistencia a su DTO de salida.
     *
     * @param reservation entidad origen
     * @return el DTO equivalente
     */
    public static VisitorReservationResponse from(VisitorReservation reservation) {
        return new VisitorReservationResponse(
                reservation.getId(),
                reservation.getVisitorId(),
                reservation.getParkingSpaceId(),
                reservation.getReservationDate(),
                reservation.getNotes(),
                reservation.getCreatedById(),
                reservation.getCreatedAt());
    }
}
