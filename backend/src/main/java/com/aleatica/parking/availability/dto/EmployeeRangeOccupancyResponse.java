package com.aleatica.parking.availability.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/**
 * Ocupacion de un empleado en un RANGO de fechas para la liberacion administrativa por rango
 * (change {@code admin-release-by-range}, caso "vacaciones"): por cada dia del rango {@code [from, to]}
 * (ambos inclusive), las reservas del empleado en plaza y puesto con su origen y, cuando proceden de
 * una solicitud aprobada, su {@code requestId}.
 *
 * <p>Misma forma y reglas de precedencia que {@link EmployeeWeekOccupancyResponse} (solicitud
 * {@code APPROVED} &gt; asignacion fija vigente no liberada), pero sobre un rango arbitrario en vez de
 * una semana fija. Es un DTO, no la entidad JPA (S4684 / OWASP API3). Permite que el cliente obtenga
 * de UNA sola consulta todas las reservas del empleado en el rango (conteo fiable) y las libere en
 * lote por su mecanismo (asignacion fija -&gt; liberacion administrativa; solicitud aprobada -&gt;
 * {@code admin-cancel}). Cada componente viaja bajo su clave contractual exacta con {@link JsonProperty}.</p>
 *
 * @param employeeId   identificador del empleado consultado
 * @param employeeName nombre completo del empleado consultado
 * @param from         primer dia del rango consultado (ISO-8601, inclusive)
 * @param to           ultimo dia del rango consultado (ISO-8601, inclusive)
 * @param days         los dias del rango con las reservas del empleado en cada uno
 */
@Schema(description = "Ocupacion de un empleado en un rango de fechas (plaza y puesto) para liberacion")
public record EmployeeRangeOccupancyResponse(
        @Schema(description = "Identificador del empleado consultado", example = "15")
        @JsonProperty("employeeId") Long employeeId,

        @Schema(description = "Nombre completo del empleado consultado", example = "Ada Lovelace")
        @JsonProperty("employeeName") String employeeName,

        @Schema(description = "Primer dia del rango consultado (ISO-8601, inclusive)", example = "2026-08-03")
        @JsonProperty("from") LocalDate from,

        @Schema(description = "Ultimo dia del rango consultado (ISO-8601, inclusive)", example = "2026-08-14")
        @JsonProperty("to") LocalDate to,

        @Schema(description = "Los dias del rango con las reservas del empleado en cada uno")
        @JsonProperty("days") List<EmployeeWeekDayResponse> days) {
}
