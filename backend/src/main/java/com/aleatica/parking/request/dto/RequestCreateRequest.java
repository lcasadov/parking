package com.aleatica.parking.request.dto;

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
 * @param requestedDate fecha solicitada (obligatoria)
 */
@Schema(description = "Peticion de creacion de solicitud para una fecha")
public record RequestCreateRequest(
        @Schema(description = "Fecha solicitada (ISO-8601)", example = "2026-07-10")
        @JsonProperty("requestedDate")
        @NotNull(message = "La fecha solicitada es obligatoria")
        LocalDate requestedDate) {
}
