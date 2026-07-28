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
 * SOLO la ubicacion del parking (nunca el modo de aprobacion ni la trazabilidad del ultimo
 * cambio). Incluye las coordenadas del punto exacto fijado en el mapa (change
 * {@code admin-improvements}): si estan presentes, "Ir al parking" navega a ellas; si no, cae a la
 * direccion postal.</p>
 *
 * @param parkingAddress direccion postal del parking; {@code null} si aun no se ha configurado
 * @param parkingLat     latitud del punto exacto del parking; {@code null} si sin fijar
 * @param parkingLng     longitud del punto exacto del parking; {@code null} si sin fijar
 */
@Schema(description = "Ubicacion del parking, legible por cualquier empleado autenticado")
public record ParkingAddressResponse(
        @Schema(description = "Direccion postal del parking; null si sin configurar",
                example = "Av. de Europa 18, 28108 Alcobendas, Madrid")
        @JsonProperty("parkingAddress") String parkingAddress,

        @Schema(description = "Latitud del punto exacto del parking; null si sin fijar",
                example = "40.5405")
        @JsonProperty("parkingLat") Double parkingLat,

        @Schema(description = "Longitud del punto exacto del parking; null si sin fijar",
                example = "-3.6510")
        @JsonProperty("parkingLng") Double parkingLng) {

    /**
     * Crea la respuesta a partir del ajuste global vigente.
     *
     * @param settings ajuste global de dominio
     * @return el DTO con la ubicacion vigente
     */
    public static ParkingAddressResponse from(SystemSettings settings) {
        return new ParkingAddressResponse(
                settings.getParkingAddress(), settings.getParkingLat(), settings.getParkingLng());
    }
}
