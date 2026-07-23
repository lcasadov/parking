package com.aleatica.parking.availability.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/**
 * Reservas de un empleado para un dia concreto de la semana en la vista de liberacion por
 * empleado: las ocupaciones del empleado ese dia en plaza y/o puesto (posiblemente vacia).
 *
 * <p>Es un DTO, no la entidad JPA (S4684 / OWASP API3). Cada reserva reutiliza
 * {@link OccupancyItemResponse} (tipo de recurso, recurso, origen y {@code requestId} cuando
 * proviene de una solicitud aprobada). Un dia sin reservas del empleado devuelve la lista
 * vacia, no se omite, para que el navegador de semana muestre los siete dias.</p>
 *
 * @param date         fecha del dia (ISO-8601)
 * @param reservations reservas del empleado ese dia (plaza y/o puesto), posiblemente vacia
 */
@Schema(description = "Reservas de un empleado para un dia (plaza y/o puesto)")
public record EmployeeWeekDayResponse(
        @Schema(description = "Fecha del dia (ISO-8601)", example = "2026-07-10")
        @JsonProperty("date") LocalDate date,

        @Schema(description = "Reservas del empleado ese dia")
        @JsonProperty("reservations") List<OccupancyItemResponse> reservations) {
}
