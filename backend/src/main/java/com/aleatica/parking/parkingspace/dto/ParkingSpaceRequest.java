package com.aleatica.parking.parkingspace.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Datos de alta o edicion de una plaza (schema {@code ParkingSpaceCreate} de la API).
 *
 * <p>La plaza se identifica por su {@code number} entero ({@code >= 1000}); la
 * planta y el {@code label} son <em>derivados</em> del numero y no viajan como
 * entrada (design §Decision 4). La validacion sintactica ({@code @NotNull},
 * {@code @Min}) es la primera barrera (UX); la unicidad del {@code number} se
 * valida en el caso de uso (frontera de seguridad, OWASP A04). El campo
 * {@code active} es opcional y por defecto {@code true} (alta operativa).</p>
 *
 * @param number numero unico de la plaza (obligatorio, {@code >= 1000})
 * @param active estado activo/inactivo; {@code null} = por defecto activa
 */
@Schema(description = "Datos para crear o modificar una plaza")
public record ParkingSpaceRequest(
        @Schema(description = "Numero unico de la plaza (>= 1000); la planta se deriva"
                + " como number/1000", example = "1007")
        @NotNull @Min(1000) Integer number,

        @Schema(description = "Estado activo de la plaza", example = "true", defaultValue = "true")
        @JsonProperty("active") Boolean active) {

    /**
     * @return el estado {@code active} indicado, o {@code true} si se omite
     */
    public boolean activeOrDefault() {
        return active == null || active;
    }
}
