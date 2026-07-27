package com.aleatica.parking.systemsettings.dto;

import com.aleatica.parking.systemsettings.domain.SystemSettings;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Representacion de salida minima del permiso de reservas en fin de semana (schema
 * {@code WeekendReservableResponse} de la API), legible por <strong>cualquier</strong> empleado
 * autenticado (change {@code reservas-employee-admin-reassign}): la UI necesita saber si puede
 * ofrecer sabado/domingo como fechas reservables antes de enviar la solicitud.
 *
 * <p>A diferencia de {@link SystemSettingsResponse} (reservado a {@code ADMIN}), este DTO expone
 * SOLO el flag (nunca el modo de aprobacion, la direccion ni la trazabilidad).</p>
 *
 * @param weekendReservable si se permiten reservas en fin de semana
 */
@Schema(description = "Permiso de reservas en fin de semana, legible por cualquier autenticado")
public record WeekendReservableResponse(
        @Schema(description = "Si se permiten reservas en sabado/domingo", example = "false")
        @JsonProperty("weekendReservable") boolean weekendReservable) {

    /**
     * Crea la respuesta a partir del ajuste global vigente.
     *
     * @param settings ajuste global de dominio
     * @return el DTO con el flag vigente
     */
    public static WeekendReservableResponse from(SystemSettings settings) {
        return new WeekendReservableResponse(settings.isWeekendReservable());
    }
}
