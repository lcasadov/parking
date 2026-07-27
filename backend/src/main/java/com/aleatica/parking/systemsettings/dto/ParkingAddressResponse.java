package com.aleatica.parking.systemsettings.dto;

import com.aleatica.parking.systemsettings.domain.SystemSettings;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Representacion de salida minima de la direccion del parking (schema
 * {@code ParkingAddressResponse} de la API), legible por <strong>cualquier</strong> empleado
 * autenticado (change {@code reservas-employee-admin-reassign}): "Mi Semana" la usa para el boton
 * "Ir al parking" que abre Google Maps.
 *
 * <p>A diferencia de {@link SystemSettingsResponse} (reservado a {@code ADMIN}), este DTO expone
 * SOLO la direccion (nunca el modo de aprobacion ni la trazabilidad del ultimo cambio).</p>
 *
 * @param parkingAddress direccion postal del parking; {@code null} si aun no se ha configurado
 */
@Schema(description = "Direccion del parking, legible por cualquier empleado autenticado")
public record ParkingAddressResponse(
        @Schema(description = "Direccion postal del parking; null si sin configurar",
                example = "Av. de Europa 18, 28108 Alcobendas, Madrid")
        @JsonProperty("parkingAddress") String parkingAddress) {

    /**
     * Crea la respuesta a partir del ajuste global vigente.
     *
     * @param settings ajuste global de dominio
     * @return el DTO con la direccion vigente
     */
    public static ParkingAddressResponse from(SystemSettings settings) {
        return new ParkingAddressResponse(settings.getParkingAddress());
    }
}
