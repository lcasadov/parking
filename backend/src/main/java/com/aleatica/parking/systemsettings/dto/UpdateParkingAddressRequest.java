package com.aleatica.parking.systemsettings.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de entrada de {@code PUT updateParkingAddress} (schema
 * {@code UpdateParkingAddressRequest} de la API, change
 * {@code reservas-employee-admin-reassign}).
 *
 * <p>La direccion es <strong>opcional</strong>: enviar {@code null} o cadena en blanco borra la
 * direccion configurada (el boton "Ir al parking" del empleado desaparece). El servicio normaliza
 * el valor (recorta espacios; blanco &rarr; {@code null}). La longitud maxima ({@code @Size}) es la
 * primera capa de validacion, coherente con la columna {@code VARCHAR(500)}.</p>
 *
 * <p>Opcionalmente incluye las coordenadas del punto exacto fijado en el mapa (Mapbox). Solo se
 * persisten junto a una direccion no vacia; borrar la direccion descarta las coordenadas (change
 * {@code admin-improvements}).</p>
 *
 * @param parkingAddress nueva direccion postal del parking; {@code null}/blanco para borrarla
 * @param parkingLat     latitud del punto exacto ([-90, 90]); {@code null} si sin punto
 * @param parkingLng     longitud del punto exacto ([-180, 180]); {@code null} si sin punto
 */
@Schema(description = "Peticion de cambio de la ubicacion del parking (null/blanco borra la "
        + "direccion y sus coordenadas)")
public record UpdateParkingAddressRequest(
        @Schema(description = "Direccion postal del parking; null o blanco para borrarla",
                example = "Av. de Europa 18, 28108 Alcobendas, Madrid")
        @JsonProperty("parkingAddress")
        @Size(max = 500, message = "La direccion no puede superar los 500 caracteres")
        String parkingAddress,

        @Schema(description = "Latitud del punto exacto del parking; null si sin punto",
                example = "40.5405")
        @JsonProperty("parkingLat")
        @DecimalMin(value = "-90.0", message = "La latitud debe estar entre -90 y 90")
        @DecimalMax(value = "90.0", message = "La latitud debe estar entre -90 y 90")
        Double parkingLat,

        @Schema(description = "Longitud del punto exacto del parking; null si sin punto",
                example = "-3.6510")
        @JsonProperty("parkingLng")
        @DecimalMin(value = "-180.0", message = "La longitud debe estar entre -180 y 180")
        @DecimalMax(value = "180.0", message = "La longitud debe estar entre -180 y 180")
        Double parkingLng) {
}
