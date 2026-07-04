package com.aleatica.parking.desk.dto;

import com.aleatica.parking.desk.DeskCategory;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Datos de edicion de un puesto (schema {@code DeskUpdate} de la API).
 *
 * <p>El {@code number} es inmutable (identifica el puesto fisico) y no viaja en el cuerpo.
 * {@code category} es obligatoria; las coordenadas son opcionales y, cuando se indican, se
 * validan en el porcentaje 0-100 (400 con {@code fields.coordX}/{@code fields.coordY}). Si
 * se omiten, se conserva el valor actual. Las claves contractuales se fijan con
 * {@link JsonProperty}.</p>
 *
 * @param category categoria del puesto (obligatoria)
 * @param coordX   coordenada X (porcentaje 0-100); {@code null} conserva la actual
 * @param coordY   coordenada Y (porcentaje 0-100); {@code null} conserva la actual
 */
@Schema(description = "Datos para modificar un puesto de oficina")
public record DeskUpdateRequest(
        @Schema(description = "Categoria del puesto", example = "EXECUTIVE")
        @JsonProperty("category")
        @NotNull(message = "La categoria es obligatoria")
        DeskCategory category,

        @Schema(description = "Coordenada X (porcentaje 0-100)", example = "30.5")
        @JsonProperty("coordX")
        @DecimalMin(value = "0", message = "La coordenada X debe estar entre 0 y 100")
        @DecimalMax(value = "100", message = "La coordenada X debe estar entre 0 y 100")
        BigDecimal coordX,

        @Schema(description = "Coordenada Y (porcentaje 0-100)", example = "47.0")
        @JsonProperty("coordY")
        @DecimalMin(value = "0", message = "La coordenada Y debe estar entre 0 y 100")
        @DecimalMax(value = "100", message = "La coordenada Y debe estar entre 0 y 100")
        BigDecimal coordY) {
}
