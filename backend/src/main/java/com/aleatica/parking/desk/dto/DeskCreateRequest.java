package com.aleatica.parking.desk.dto;

import com.aleatica.parking.desk.DeskCategory;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Datos de alta de un puesto (schema {@code DeskCreate} de la API).
 *
 * <p>La validacion sintactica es la primera barrera (UX) y la frontera de seguridad
 * (OWASP A04): {@code number} obligatorio en el rango 1-65 ({@code @Min}/{@code @Max} →
 * 400 con {@code fields.number}), {@code category} obligatoria, y coordenadas en el
 * porcentaje 0-100 cuando se indican. Las coordenadas son opcionales: si se omiten se
 * centran (50/50) hasta posicionarlas en {@code floor-plan}. La unicidad del
 * {@code number} se valida en el caso de uso (409). Las claves contractuales se fijan con
 * {@link JsonProperty}.</p>
 *
 * @param number   numero unico del puesto (obligatorio, 1-65)
 * @param category categoria del puesto (obligatoria)
 * @param coordX   coordenada X (porcentaje 0-100); {@code null} = centrada (50)
 * @param coordY   coordenada Y (porcentaje 0-100); {@code null} = centrada (50)
 */
@Schema(description = "Datos para crear un puesto de oficina")
public record DeskCreateRequest(
        @Schema(description = "Numero unico del puesto (entero mayor que 0)", example = "12")
        @JsonProperty("number")
        @NotNull(message = "El numero del puesto es obligatorio")
        @Min(value = 1, message = "El numero del puesto debe ser un entero mayor que 0")
        Integer number,

        @Schema(description = "Categoria del puesto", example = "STANDARD")
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

    /** Coordenada por defecto (centro del plano) cuando el alta la omite. */
    private static final BigDecimal DEFAULT_COORD = new BigDecimal("50");

    /**
     * @return la coordenada X indicada, o el centro (50) si se omite
     */
    public BigDecimal coordXOrDefault() {
        return coordX == null ? DEFAULT_COORD : coordX;
    }

    /**
     * @return la coordenada Y indicada, o el centro (50) si se omite
     */
    public BigDecimal coordYOrDefault() {
        return coordY == null ? DEFAULT_COORD : coordY;
    }
}
