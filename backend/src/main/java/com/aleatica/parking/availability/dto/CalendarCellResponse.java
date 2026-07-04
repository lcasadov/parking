package com.aleatica.parking.availability.dto;

import com.aleatica.parking.availability.CalendarCellState;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * Celda (plaza, dia) del calendario semanal admin (schema {@code CalendarCell} de la API,
 * autoridad {@code docs/openapi.yaml}).
 *
 * <p>Es un DTO, no la entidad JPA (S4684 / OWASP API3). Cada componente viaja bajo su
 * clave contractual exacta fijada con {@link JsonProperty}. {@code employeeId},
 * {@code employeeName} y {@code requestId} son {@code null} cuando la celda esta libre.</p>
 *
 * @param date         fecha de la celda (ISO-8601)
 * @param state        estado de la celda
 * @param employeeId   titular del recurso ese dia; {@code null} si libre
 * @param employeeName nombre del titular ese dia; {@code null} si libre
 * @param requestId    solicitud aprobada que ocupa la celda; {@code null} si no aplica
 */
@Schema(description = "Celda (plaza, dia) del calendario semanal admin")
public record CalendarCellResponse(
        @Schema(description = "Fecha de la celda (ISO-8601)", example = "2026-07-06")
        @JsonProperty("date") LocalDate date,

        @Schema(description = "Estado de la celda", example = "ASSIGNED")
        @JsonProperty("state") CalendarCellState state,

        @Schema(description = "Titular del recurso ese dia; null si libre", example = "15")
        @JsonProperty("employeeId") Long employeeId,

        @Schema(description = "Nombre del titular ese dia; null si libre", example = "Ada Lovelace")
        @JsonProperty("employeeName") String employeeName,

        @Schema(description = "Solicitud aprobada que ocupa la celda; null si no aplica", example = "42")
        @JsonProperty("requestId") Long requestId) {
}
