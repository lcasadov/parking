package com.aleatica.parking.systemsettings.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
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
 * @param parkingAddress nueva direccion postal del parking; {@code null}/blanco para borrarla
 */
@Schema(description = "Peticion de cambio de la direccion del parking (null/blanco la borra)")
public record UpdateParkingAddressRequest(
        @Schema(description = "Direccion postal del parking; null o blanco para borrarla",
                example = "Av. de Europa 18, 28108 Alcobendas, Madrid")
        @JsonProperty("parkingAddress")
        @Size(max = 500, message = "La direccion no puede superar los 500 caracteres")
        String parkingAddress) {
}
