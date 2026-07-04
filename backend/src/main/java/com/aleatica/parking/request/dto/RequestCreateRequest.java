package com.aleatica.parking.request.dto;

import com.aleatica.parking.resource.ResourceType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Cuerpo de entrada de {@code POST createRequest} (schema {@code RequestCreateRequest}
 * de la API).
 *
 * <p>La validacion sintactica ({@code @NotNull}) es la primera capa (UX); la ventana
 * temporal hoy..hoy+14 la verifica el caso de uso con {@code ClockPort} (frontera de
 * seguridad, OWASP A04). La clave contractual {@code requestedDate} se fija con
 * {@link JsonProperty}.</p>
 *
 * <p>{@code resourceType} es opcional y por defecto {@code PARKING} (compatibilidad con
 * el contrato del nucleo): con {@code DESK} el empleado solicita un puesto para la fecha.
 * La unicidad {@code PENDING} es por empleado/tipo/fecha, de modo que un empleado puede
 * tener una solicitud de plaza y otra de puesto pendientes la misma fecha.</p>
 *
 * @param requestedDate fecha solicitada (obligatoria)
 * @param resourceType  tipo de recurso solicitado; {@code null} = {@code PARKING} por defecto
 */
@Schema(description = "Peticion de creacion de solicitud para una fecha")
public record RequestCreateRequest(
        @Schema(description = "Fecha solicitada (ISO-8601)", example = "2026-07-10")
        @JsonProperty("requestedDate")
        @NotNull(message = "La fecha solicitada es obligatoria")
        LocalDate requestedDate,

        @Schema(description = "Tipo de recurso solicitado; por defecto PARKING", example = "DESK",
                defaultValue = "PARKING")
        @JsonProperty("resourceType")
        ResourceType resourceType) {

    /**
     * @return el {@code resourceType} indicado, o {@code PARKING} si se omite
     */
    public ResourceType resourceTypeOrDefault() {
        return resourceType == null ? ResourceType.PARKING : resourceType;
    }
}
