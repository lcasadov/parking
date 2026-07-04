package com.aleatica.parking.availability.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/**
 * Vista personal de la semana del solicitante (schema {@code MyWeekResponse} de la API,
 * autoridad {@code docs/openapi.yaml}).
 *
 * <p>Es un DTO, no la entidad JPA (S4684 / OWASP API3). Cada componente viaja bajo su
 * clave contractual exacta fijada con {@link JsonProperty}. Por diseno no incluye ningun
 * campo de identidad (ni propia ni ajena): solo el estado diario de los recursos propios
 * (privacidad, {@code docs/security-design.md}).</p>
 *
 * @param weekStart lunes (normalizado) de la semana mostrada
 * @param days      un dia por fecha de la semana con su estado
 */
@Schema(description = "Vista personal 'Mi Semana'")
public record MyWeekResponse(
        @Schema(description = "Lunes normalizado de la semana (ISO-8601)", example = "2026-07-06")
        @JsonProperty("weekStart") LocalDate weekStart,

        @Schema(description = "Los dias de la semana con su estado")
        @JsonProperty("days") List<MyWeekDayResponse> days) {
}
