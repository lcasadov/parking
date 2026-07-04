package com.aleatica.parking.floorplan.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/**
 * Plano de puestos para una fecha (schema {@code FloorPlanResponse} de la API, autoridad
 * {@code docs/openapi.yaml}).
 *
 * <p>Es un DTO, no la entidad JPA (S4684 / OWASP API3). Devuelve, en una unica proyeccion
 * (sin N+1), la posicion y el estado de cada puesto activo para la fecha consultada, listo
 * para pintar el plano. Cada componente viaja bajo su clave contractual exacta fijada con
 * {@link JsonProperty}.</p>
 *
 * @param date  fecha consultada (ISO-8601)
 * @param desks puestos del plano con su posicion y estado (posiblemente vacia)
 */
@Schema(description = "Plano de puestos con estado por fecha")
public record FloorPlanResponse(
        @Schema(description = "Fecha consultada (ISO-8601)", example = "2026-07-10")
        @JsonProperty("date") LocalDate date,

        @Schema(description = "Puestos del plano con posicion y estado")
        @JsonProperty("desks") List<FloorPlanDeskResponse> desks) {
}
