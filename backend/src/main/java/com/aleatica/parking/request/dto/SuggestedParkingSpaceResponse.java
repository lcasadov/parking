package com.aleatica.parking.request.dto;

import com.aleatica.parking.parkingspace.ParkingSpace;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Vista previa de la plaza que se auto-asignaria a un empleado para una fecha concreta segun su
 * categoria/planta (capability {@code admin-punctual-assignment}), sin llegar a crear la
 * asignacion.
 *
 * <p>Es un DTO, no la entidad JPA (S4684 / OWASP API3). Lo consume el resumen del asistente de
 * reserva ANTES de confirmar {@code POST /requests/admin}, para mostrar al ADMIN que plaza le
 * tocaria al empleado destino. Cuando no hay ninguna plaza libre esa fecha, {@code available}
 * viaja en {@code false} y el resto de campos en {@code null} (200, no 404/409): no es un error,
 * es informacion de negocio que el resumen debe mostrar como "sin plaza libre esa fecha".</p>
 *
 * @param available     {@code true} si existe una plaza libre que se auto-asignaria
 * @param parkingSpaceId identificador de la plaza sugerida; {@code null} si no hay disponibilidad
 * @param number        numero humano de la plaza sugerida; {@code null} si no hay disponibilidad
 * @param floor         planta de la plaza sugerida; {@code null} si no hay disponibilidad
 */
@Schema(description = "Plaza que se auto-asignaria a un empleado para una fecha (preview, sin crear "
        + "la asignacion)")
public record SuggestedParkingSpaceResponse(
        @Schema(description = "Indica si hay una plaza libre que se auto-asignaria", example = "true")
        @JsonProperty("available") boolean available,

        @Schema(description = "Identificador de la plaza sugerida; null si no hay disponibilidad",
                example = "8")
        @JsonProperty("parkingSpaceId") Long parkingSpaceId,

        @Schema(description = "Numero humano de la plaza sugerida; null si no hay disponibilidad",
                example = "3005")
        @JsonProperty("number") Integer number,

        @Schema(description = "Planta de la plaza sugerida; null si no hay disponibilidad",
                example = "3")
        @JsonProperty("floor") Integer floor) {

    private static final SuggestedParkingSpaceResponse UNAVAILABLE =
            new SuggestedParkingSpaceResponse(false, null, null, null);

    /**
     * Proyecta la plaza auto-asignada a la vista previa disponible.
     *
     * @param space plaza elegida por {@code autoAssignParkingSpace}
     * @return la vista previa con {@code available = true} y los datos de la plaza
     */
    public static SuggestedParkingSpaceResponse from(ParkingSpace space) {
        return new SuggestedParkingSpaceResponse(
                true, space.getId(), space.getNumber(), space.floor());
    }

    /**
     * Vista previa cuando no hay ninguna plaza libre esa fecha ({@code available = false}).
     *
     * @return la vista previa sin disponibilidad
     */
    public static SuggestedParkingSpaceResponse unavailable() {
        return UNAVAILABLE;
    }
}
