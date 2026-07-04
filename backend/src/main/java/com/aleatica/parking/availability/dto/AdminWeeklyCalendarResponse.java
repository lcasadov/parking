package com.aleatica.parking.availability.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/**
 * Calendario semanal completo del {@code ADMIN} (schema {@code AdminWeeklyCalendarResponse}
 * de la API, autoridad {@code docs/openapi.yaml}).
 *
 * <p>Es un DTO, no la entidad JPA (S4684 / OWASP API3). Cada componente viaja bajo su
 * clave contractual exacta fijada con {@link JsonProperty}. {@code weekStart} es el lunes
 * normalizado de la semana solicitada.</p>
 *
 * @param weekStart lunes (normalizado) de la semana mostrada
 * @param days      las siete fechas de la semana (lunes..domingo)
 * @param rows      una fila por plaza con sus celdas por dia
 */
@Schema(description = "Calendario semanal completo del administrador")
public record AdminWeeklyCalendarResponse(
        @Schema(description = "Lunes normalizado de la semana (ISO-8601)", example = "2026-07-06")
        @JsonProperty("weekStart") LocalDate weekStart,

        @Schema(description = "Las siete fechas de la semana")
        @JsonProperty("days") List<LocalDate> days,

        @Schema(description = "Filas del calendario (una por plaza)")
        @JsonProperty("rows") List<CalendarRowResponse> rows) {
}
