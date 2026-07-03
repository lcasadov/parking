package com.aleatica.parking.parkingspace.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos de alta o edicion de una plaza (schema {@code ParkingSpaceCreate} de la API).
 *
 * <p>La validacion sintactica ({@code @NotBlank}, {@code @Size}) es la primera
 * barrera (UX); la unicidad del {@code label} se valida en el caso de uso
 * (frontera de seguridad, OWASP A04). El campo {@code active} es opcional y por
 * defecto {@code true} (alta de plaza operativa).</p>
 *
 * @param label  etiqueta unica de la plaza (obligatoria, max 20)
 * @param active estado activo/inactivo; {@code null} = por defecto activa
 */
@Schema(description = "Datos para crear o modificar una plaza")
public record ParkingSpaceRequest(
        @Schema(description = "Etiqueta unica de la plaza", example = "P-08")
        @NotBlank @Size(max = 20) String label,

        @Schema(description = "Estado activo de la plaza", example = "true", defaultValue = "true")
        @JsonProperty("active") Boolean active) {

    /**
     * @return el estado {@code active} indicado, o {@code true} si se omite
     */
    public boolean activeOrDefault() {
        return active == null || active;
    }
}
