package com.aleatica.parking.visitor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Cuerpo de entrada de {@code POST createVisitorReservation} (schema
 * {@code VisitorReservationCreateRequest} de la API, autoridad {@code docs/openapi.yaml}).
 *
 * <p>La validacion sintactica ({@code @NotNull} sobre los campos obligatorios) es la
 * primera capa (UX y 400); la disponibilidad de la plaza para la fecha la verifica el
 * caso de uso transaccionalmente (409, frontera de seguridad OWASP A04). Las claves
 * contractuales se fijan con {@link JsonProperty}.</p>
 *
 * @param visitorId       visitante para el que se reserva (obligatorio)
 * @param parkingSpaceId  plaza a reservar (obligatorio)
 * @param reservationDate fecha reservada (obligatorio)
 * @param notes           anotaciones opcionales (max 500)
 */
@Schema(description = "Peticion de creacion de una reserva de plaza para un visitante")
public record VisitorReservationCreateRequest(
        @Schema(description = "Visitante para el que se reserva", example = "42")
        @JsonProperty("visitorId")
        @NotNull(message = "El visitante es obligatorio")
        Long visitorId,

        @Schema(description = "Plaza a reservar", example = "8")
        @JsonProperty("parkingSpaceId")
        @NotNull(message = "La plaza es obligatoria")
        Long parkingSpaceId,

        @Schema(description = "Fecha reservada (ISO-8601)", example = "2026-07-10")
        @JsonProperty("reservationDate")
        @NotNull(message = "La fecha de la reserva es obligatoria")
        LocalDate reservationDate,

        @Schema(description = "Anotaciones", example = "Acceso puerta norte", maxLength = 500)
        @JsonProperty("notes")
        @Size(max = 500, message = "Las anotaciones no pueden superar 500 caracteres")
        String notes) {
}
