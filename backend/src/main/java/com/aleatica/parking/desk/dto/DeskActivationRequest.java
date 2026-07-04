package com.aleatica.parking.desk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo de {@code PATCH setDeskActivation} (schema {@code DeskActivationRequest} de la
 * API): activa o desactiva un puesto.
 *
 * <p>Un puesto inactivo no aparece como disponible en el calculo de disponibilidad, sin
 * borrar la fila ni sus asignaciones (quedan inertes). La clave contractual {@code active}
 * se fija con {@link JsonProperty} para que Jackson no derive otra clave del record.</p>
 *
 * @param active nuevo estado activo del puesto (obligatorio)
 */
@Schema(description = "Nuevo estado de activacion de un puesto")
public record DeskActivationRequest(
        @Schema(description = "Estado activo del puesto", example = "false")
        @JsonProperty("active")
        @NotNull(message = "El estado activo es obligatorio")
        Boolean active) {
}
