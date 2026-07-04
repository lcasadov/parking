package com.aleatica.parking.release.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Cuerpo de entrada de {@code POST createRelease} (schema {@code ReleaseCreateRequest}
 * de la API).
 *
 * <p>La validacion sintactica ({@code @NotNull} sobre {@code releaseDate}) es la primera
 * capa (UX); la ventana temporal ({@code releaseDate >= hoy}) y la resolucion de la plaza
 * fija las verifica el caso de uso con {@code ClockPort} (frontera de seguridad, OWASP
 * A04). {@code parkingSpaceId} es opcional: si se omite, el servicio resuelve la plaza
 * fija del empleado para el dia de la semana de {@code releaseDate}. Las claves
 * contractuales se fijan con {@link JsonProperty}.</p>
 *
 * @param releaseDate    fecha a liberar (obligatoria, presente o futura)
 * @param parkingSpaceId recurso a liberar; {@code null} para resolver la plaza fija
 */
@Schema(description = "Peticion de liberacion voluntaria de un recurso para una fecha")
public record ReleaseCreateRequest(
        @Schema(description = "Fecha a liberar (ISO-8601)", example = "2026-07-10")
        @JsonProperty("releaseDate")
        @NotNull(message = "La fecha de liberacion es obligatoria")
        LocalDate releaseDate,

        @Schema(description = "Recurso a liberar; si se omite se resuelve la plaza fija del empleado",
                example = "8")
        @JsonProperty("parkingSpaceId")
        Long parkingSpaceId) {
}
