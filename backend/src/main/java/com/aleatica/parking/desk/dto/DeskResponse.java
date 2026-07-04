package com.aleatica.parking.desk.dto;

import com.aleatica.parking.desk.Desk;
import com.aleatica.parking.desk.DeskCategory;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Representacion de salida de un puesto (schema {@code Desk} de la API, autoridad
 * {@code docs/openapi.yaml}).
 *
 * <p>Es un DTO, no la entidad JPA (S4684 / OWASP API3). Cada componente viaja bajo su
 * clave contractual exacta fijada con {@link JsonProperty}, de modo que Jackson no derive
 * otra clave del record (en particular {@code active}, blindaje del bug #21 con
 * {@code isCorporate}). El {@code label} derivado ({@code D-05}) se incluye como ayuda de
 * presentacion; {@code category} se serializa por su nombre ({@code STANDARD}/
 * {@code EXECUTIVE}).</p>
 *
 * @param id        identificador unico
 * @param number    numero del puesto (1-65)
 * @param label     etiqueta humana derivada del numero ({@code D-05})
 * @param category  categoria del puesto
 * @param coordX    coordenada X (porcentaje 0-100)
 * @param coordY    coordenada Y (porcentaje 0-100)
 * @param active    si el puesto esta activo (entra en disponibilidad)
 * @param createdAt instante de alta (UTC)
 */
@Schema(description = "Datos de un puesto de oficina")
public record DeskResponse(
        @Schema(description = "Identificador unico", example = "42")
        @JsonProperty("id") Long id,

        @Schema(description = "Numero del puesto (1-65)", example = "12")
        @JsonProperty("number") Integer number,

        @Schema(description = "Etiqueta derivada del numero", example = "D-12")
        @JsonProperty("label") String label,

        @Schema(description = "Categoria del puesto", example = "STANDARD")
        @JsonProperty("category") DeskCategory category,

        @Schema(description = "Coordenada X (porcentaje 0-100)", example = "30.5")
        @JsonProperty("coordX") BigDecimal coordX,

        @Schema(description = "Coordenada Y (porcentaje 0-100)", example = "47.0")
        @JsonProperty("coordY") BigDecimal coordY,

        @Schema(description = "Estado activo del puesto", example = "true")
        @JsonProperty("active") boolean active,

        @Schema(description = "Instante de alta (ISO-8601)")
        @JsonProperty("createdAt") Instant createdAt) {

    /**
     * Mapea la entidad de persistencia a su DTO de salida.
     *
     * @param desk entidad origen
     * @return el DTO equivalente
     */
    public static DeskResponse from(Desk desk) {
        return new DeskResponse(
                desk.getId(),
                desk.getNumber(),
                desk.getLabel(),
                desk.getCategory(),
                desk.getCoordX(),
                desk.getCoordY(),
                desk.isActive(),
                desk.getCreatedAt());
    }
}
