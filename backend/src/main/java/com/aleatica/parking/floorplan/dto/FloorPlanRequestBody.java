package com.aleatica.parking.floorplan.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Cuerpo de entrada de {@code POST requestDeskFromFloorPlan} (schema
 * {@code FloorPlanRequestBody} de la API): la fecha para la que se solicita el puesto
 * pinchado en el plano.
 *
 * <p>La validacion sintactica ({@code @NotNull}) es la primera capa (UX); la ventana
 * temporal hoy..hoy+14 la verifica el caso de uso con {@code ClockPort} (frontera de
 * seguridad, OWASP A04). La clave contractual {@code date} se fija con
 * {@link JsonProperty}.</p>
 *
 * @param date fecha solicitada (obligatoria)
 */
@Schema(description = "Peticion de solicitud de un puesto desde el plano para una fecha")
public record FloorPlanRequestBody(
        @Schema(description = "Fecha solicitada (ISO-8601)", example = "2026-07-10")
        @JsonProperty("date")
        @NotNull(message = "La fecha solicitada es obligatoria")
        LocalDate date) {
}
