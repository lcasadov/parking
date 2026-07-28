package com.aleatica.parking.request.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Vista previa del recurso (plaza o puesto) que se auto-asignaria al PROPIO empleado para una
 * fecha, aplicando la MISMA logica que la auto-asignacion real —incluida la preferencia por el
 * recurso fijo propio (Feature A)— sin crear la solicitud (change
 * {@code reservas-employee-admin-reassign}, Feature de feedback de auto-asignacion).
 *
 * <p>Es un DTO, no la entidad JPA (S4684 / OWASP API3). Lo consume el asistente de reserva del
 * empleado ANTES de enviar {@code POST /requests}, para mostrar "se te asignara Plaza 3005".
 * Cuando no hay ningun recurso libre esa fecha, {@code available} viaja en {@code false} y
 * {@code resourceLabel} en {@code null} (200, no error): es informacion de negocio.</p>
 *
 * @param available     {@code true} si existe un recurso que se auto-asignaria
 * @param resourceLabel etiqueta humana del recurso sugerido (p. ej. "Plaza 3005" / "Puesto 12");
 *                      {@code null} si no hay disponibilidad
 */
@Schema(description = "Recurso que se auto-asignaria al empleado para una fecha (preview)")
public record SuggestedResourceResponse(
        @Schema(description = "Indica si hay un recurso libre que se auto-asignaria", example = "true")
        @JsonProperty("available") boolean available,

        @Schema(description = "Etiqueta humana del recurso sugerido; null si no hay disponibilidad",
                example = "Plaza 3005")
        @JsonProperty("resourceLabel") String resourceLabel) {

    private static final SuggestedResourceResponse UNAVAILABLE =
            new SuggestedResourceResponse(false, null);

    /**
     * Vista previa disponible con la etiqueta del recurso sugerido.
     *
     * @param resourceLabel etiqueta humana del recurso
     * @return la vista previa con {@code available = true}
     */
    public static SuggestedResourceResponse available(String resourceLabel) {
        return new SuggestedResourceResponse(true, resourceLabel);
    }

    /**
     * Vista previa cuando no hay ningun recurso libre esa fecha ({@code available = false}).
     *
     * @return la vista previa sin disponibilidad
     */
    public static SuggestedResourceResponse unavailable() {
        return UNAVAILABLE;
    }
}
