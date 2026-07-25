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
 * <p>Extendido (change {@code restructure-admin-workflows}, design §D4) para llevar el estado de
 * <strong>ambos</strong> tipos de recurso por dia: los cuatro campos historicos
 * ({@code state}/{@code parkingSpaceLabel}/{@code requestStatus}/{@code requestId}) siguen
 * describiendo la plaza ({@code PARKING}), y los cuatro nuevos {@code desk*} describen el puesto
 * ({@code DESK}) con la misma semantica, en paralelo e independientes entre si (un empleado puede
 * tener plaza y puesto, uno solo, o ninguno, el mismo dia).</p>
 *
 * @param date              fecha del dia (ISO-8601)
 * @param state             estado del dia para el empleado en su plaza (PARKING)
 * @param parkingSpaceLabel etiqueta de la plaza propia ese dia; {@code null} si no aplica
 * @param requestStatus     estado de la solicitud de plaza propia ese dia; {@code null} si no aplica
 * @param requestId         id de la solicitud de plaza propia ese dia; {@code null} si el dia no
 *                          proviene de una solicitud (asignacion fija o libre). Permite a la UI
 *                          "Liberar" el recurso del dia cancelando la solicitud (change
 *                          release-occupied-resource)
 * @param deskState         estado del dia para el empleado en su puesto (DESK)
 * @param deskLabel         etiqueta del puesto propio ese dia; {@code null} si no aplica
 * @param deskRequestStatus estado de la solicitud de puesto propia ese dia; {@code null} si no aplica
 * @param deskRequestId     id de la solicitud de puesto propia ese dia; {@code null} si el dia no
 *                          proviene de una solicitud
 * @param waitlisted        {@code true} si la solicitud de plaza propia ese dia esta en lista de
 *                          espera (change {@code waitlist-requests}); {@code false} si el dia no
 *                          proviene de una solicitud en espera
 * @param deskWaitlisted    igual que {@code waitlisted}, para la solicitud de puesto propia
 */
@Schema(description = "Dia de la vista personal 'Mi Semana' (plaza y puesto)")
public record MyWeekDayResponse(
        @Schema(description = "Fecha del dia (ISO-8601)", example = "2026-07-06")
        @JsonProperty("date") LocalDate date,

        @Schema(description = "Estado del dia para el empleado en su plaza", example = "ASSIGNED")
        @JsonProperty("state") MyWeekDayState state,

        @Schema(description = "Etiqueta de la plaza propia ese dia; null si no aplica", example = "P-08")
        @JsonProperty("parkingSpaceLabel") String parkingSpaceLabel,

        @Schema(description = "Estado de la solicitud de plaza propia ese dia; null si no aplica",
                example = "APPROVED")
        @JsonProperty("requestStatus") RequestStatus requestStatus,

        @Schema(description = "Id de la solicitud de plaza propia ese dia; null si no proviene de "
                + "una solicitud", example = "42")
        @JsonProperty("requestId") Long requestId,

        @Schema(description = "Estado del dia para el empleado en su puesto", example = "FREE")
        @JsonProperty("deskState") MyWeekDayState deskState,

        @Schema(description = "Etiqueta del puesto propio ese dia; null si no aplica", example = "Puesto 12")
        @JsonProperty("deskLabel") String deskLabel,

        @Schema(description = "Estado de la solicitud de puesto propia ese dia; null si no aplica",
                example = "PENDING")
        @JsonProperty("deskRequestStatus") RequestStatus deskRequestStatus,

        @Schema(description = "Id de la solicitud de puesto propia ese dia; null si no proviene de "
                + "una solicitud", example = "43")
        @JsonProperty("deskRequestId") Long deskRequestId,

        @Schema(description = "En lista de espera la solicitud de plaza propia ese dia",
                example = "false")
        @JsonProperty("waitlisted") boolean waitlisted,

        @Schema(description = "En lista de espera la solicitud de puesto propia ese dia",
                example = "false")
        @JsonProperty("deskWaitlisted") boolean deskWaitlisted) {
}
