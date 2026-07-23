package com.aleatica.parking.availability.dto;

import com.aleatica.parking.availability.MyWeekDayState;
import com.aleatica.parking.request.domain.RequestStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * Dia de la vista "Mi Semana" (schema {@code MyWeekDay} de la API, autoridad
 * {@code docs/openapi.yaml}).
 *
 * <p>Es un DTO, no la entidad JPA (S4684 / OWASP API3). Cada componente viaja bajo su
 * clave contractual exacta fijada con {@link JsonProperty}. No contiene identidad de
 * terceros (privacidad, {@code docs/security-design.md}): solo la etiqueta del recurso
 * propio y el estado de la solicitud propia.</p>
 *
 * @param date              fecha del dia (ISO-8601)
 * @param state             estado del dia para el empleado
 * @param parkingSpaceLabel etiqueta del recurso propio ese dia; {@code null} si no aplica
 * @param requestStatus     estado de la solicitud propia ese dia; {@code null} si no aplica
 * @param requestId         id de la solicitud propia ese dia; {@code null} si el dia no proviene
 *                          de una solicitud (asignacion fija o libre). Permite a la UI "Liberar"
 *                          el recurso del dia cancelando la solicitud (change release-occupied-resource)
 */
@Schema(description = "Dia de la vista personal 'Mi Semana'")
public record MyWeekDayResponse(
        @Schema(description = "Fecha del dia (ISO-8601)", example = "2026-07-06")
        @JsonProperty("date") LocalDate date,

        @Schema(description = "Estado del dia para el empleado", example = "ASSIGNED")
        @JsonProperty("state") MyWeekDayState state,

        @Schema(description = "Etiqueta del recurso propio ese dia; null si no aplica", example = "P-08")
        @JsonProperty("parkingSpaceLabel") String parkingSpaceLabel,

        @Schema(description = "Estado de la solicitud propia ese dia; null si no aplica", example = "APPROVED")
        @JsonProperty("requestStatus") RequestStatus requestStatus,

        @Schema(description = "Id de la solicitud propia ese dia; null si no proviene de una solicitud",
                example = "42")
        @JsonProperty("requestId") Long requestId) {
}
