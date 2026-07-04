package com.aleatica.parking.floorplan.dto;

import com.aleatica.parking.desk.DeskCategory;
import com.aleatica.parking.floorplan.FloorPlanDeskState;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * Proyeccion de un puesto en el plano para una fecha (schema {@code FloorPlanDesk} de la
 * API, autoridad {@code docs/openapi.yaml}).
 *
 * <p>Es un DTO, no la entidad JPA (S4684 / OWASP API3). Cada componente viaja bajo su clave
 * contractual exacta fijada con {@link JsonProperty}. Incluye la posicion relativa
 * ({@code coordX}/{@code coordY}, porcentaje 0-100 del plano) para dibujar el marcador y el
 * estado del puesto para la fecha, relativo al solicitante. La {@code category}
 * ({@code STANDARD}/{@code EXECUTIVE}) es una distincion visual y viaja como flag; no altera
 * las reglas de disponibilidad.</p>
 *
 * @param deskId     identificador del puesto
 * @param deskNumber numero del puesto (1-65)
 * @param category   categoria del puesto (distincion visual)
 * @param coordX     coordenada X (porcentaje 0-100); {@code null} si aun sin posicionar
 * @param coordY     coordenada Y (porcentaje 0-100); {@code null} si aun sin posicionar
 * @param state      estado del puesto para la fecha, relativo al solicitante
 */
@Schema(description = "Puesto proyectado en el plano para una fecha")
public record FloorPlanDeskResponse(
        @Schema(description = "Identificador del puesto", example = "42")
        @JsonProperty("deskId") Long deskId,

        @Schema(description = "Numero del puesto (1-65)", example = "12")
        @JsonProperty("deskNumber") Integer deskNumber,

        @Schema(description = "Categoria del puesto (distincion visual)", example = "EXECUTIVE")
        @JsonProperty("category") DeskCategory category,

        @Schema(description = "Coordenada X (porcentaje 0-100); null si sin posicionar", example = "30.5")
        @JsonProperty("coordX") BigDecimal coordX,

        @Schema(description = "Coordenada Y (porcentaje 0-100); null si sin posicionar", example = "47.0")
        @JsonProperty("coordY") BigDecimal coordY,

        @Schema(description = "Estado del puesto para la fecha, relativo al solicitante",
                example = "FREE")
        @JsonProperty("state") FloorPlanDeskState state) {
}
