package com.aleatica.parking.floorplan.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Cuerpo de entrada de {@code PUT updateDeskPosition} (schema {@code DeskPositionRequest} de
 * la API): las coordenadas relativas a persistir en el editor de arrastre del admin.
 *
 * <p>Ambas coordenadas son obligatorias y se validan en el porcentaje 0-100 (frontera de
 * validacion sintactica: 400 {@code VALIDATION_ERROR} con {@code fields.coordX}/
 * {@code fields.coordY}). Las claves contractuales se fijan con {@link JsonProperty}.</p>
 *
 * @param coordX coordenada X (porcentaje 0-100)
 * @param coordY coordenada Y (porcentaje 0-100)
 */
@Schema(description = "Nueva posicion relativa de un puesto en el plano")
public record DeskPositionRequest(
        @Schema(description = "Coordenada X (porcentaje 0-100)", example = "30.5")
        @JsonProperty("coordX")
        @NotNull(message = "La coordenada X es obligatoria")
        @DecimalMin(value = "0", message = "La coordenada X debe estar entre 0 y 100")
        @DecimalMax(value = "100", message = "La coordenada X debe estar entre 0 y 100")
        BigDecimal coordX,

        @Schema(description = "Coordenada Y (porcentaje 0-100)", example = "47.0")
        @JsonProperty("coordY")
        @NotNull(message = "La coordenada Y es obligatoria")
        @DecimalMin(value = "0", message = "La coordenada Y debe estar entre 0 y 100")
        @DecimalMax(value = "100", message = "La coordenada Y debe estar entre 0 y 100")
        BigDecimal coordY) {
}
