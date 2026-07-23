package com.aleatica.parking.availability.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/**
 * Ocupacion semanal de un empleado para el flujo de liberacion administrativa (ADMIN/AGENCIA):
 * por cada uno de los siete dias de la semana, las reservas del empleado en plaza y puesto con
 * su origen y, cuando proceden de una solicitud aprobada, su {@code requestId}.
 *
 * <p>Es un DTO, no la entidad JPA (S4684 / OWASP API3). Es la base del navegador de semana del
 * selector de liberacion: el cliente marca las reservas a liberar y libera cada una por su
 * mecanismo (asignacion fija -&gt; liberacion administrativa; solicitud aprobada -&gt;
 * {@code admin-cancel} con el {@code requestId}). Cada componente viaja bajo su clave contractual
 * exacta fijada con {@link JsonProperty}.</p>
 *
 * @param employeeId   identificador del empleado consultado
 * @param employeeName nombre completo del empleado consultado
 * @param weekStart    lunes de la semana consultada (ISO-8601)
 * @param days         los siete dias de la semana con las reservas del empleado en cada uno
 */
@Schema(description = "Ocupacion semanal de un empleado (plaza y puesto) para liberacion")
public record EmployeeWeekOccupancyResponse(
        @Schema(description = "Identificador del empleado consultado", example = "15")
        @JsonProperty("employeeId") Long employeeId,

        @Schema(description = "Nombre completo del empleado consultado", example = "Ada Lovelace")
        @JsonProperty("employeeName") String employeeName,

        @Schema(description = "Lunes de la semana consultada (ISO-8601)", example = "2026-07-06")
        @JsonProperty("weekStart") LocalDate weekStart,

        @Schema(description = "Los siete dias de la semana con las reservas del empleado")
        @JsonProperty("days") List<EmployeeWeekDayResponse> days) {
}
