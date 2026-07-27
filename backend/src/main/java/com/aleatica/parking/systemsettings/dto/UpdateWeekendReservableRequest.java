package com.aleatica.parking.systemsettings.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo de entrada de {@code PUT updateWeekendReservable} (schema
 * {@code UpdateWeekendReservableRequest} de la API, change
 * {@code reservas-employee-admin-reassign}).
 *
 * <p>Se usa el envoltorio {@link Boolean} con {@code @NotNull} para distinguir "ausente" de
 * {@code false}: el cliente debe enviar el valor explicitamente (400 si se omite).</p>
 *
 * @param weekendReservable {@code true} para permitir reservas en fin de semana (obligatorio)
 */
@Schema(description = "Peticion de cambio del permiso de reservas en fin de semana")
public record UpdateWeekendReservableRequest(
        @Schema(description = "Si se permiten reservas en sabado/domingo", example = "true")
        @JsonProperty("weekendReservable")
        @NotNull(message = "El valor de weekendReservable es obligatorio")
        Boolean weekendReservable) {
}
