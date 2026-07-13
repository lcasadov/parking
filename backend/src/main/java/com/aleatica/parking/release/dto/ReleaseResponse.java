package com.aleatica.parking.release.dto;

import com.aleatica.parking.release.domain.Release;
import com.aleatica.parking.release.domain.ReleaseType;
import com.aleatica.parking.resource.ResourceType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Representacion de salida de una liberacion (schema {@code Release} de la API,
 * autoridad {@code docs/openapi.yaml}).
 *
 * <p>Es un DTO, no la entidad JPA (S4684 / OWASP API3). Cada componente viaja bajo
 * su clave contractual exacta fijada con {@link JsonProperty}, de modo que Jackson
 * no derive otra clave del record (blindaje del bug #21 con {@code isCorporate}). El
 * enum {@code type} se serializa por su nombre ({@code VOLUNTARY}, {@code ADMINISTRATIVE}).</p>
 *
 * @param id             identificador
 * @param parkingSpaceId recurso liberado
 * @param employeeId     empleado titular cuyo recurso se libera
 * @param releaseDate    fecha liberada (ISO-8601 date)
 * @param type           tipo de liberacion ({@code VOLUNTARY}/{@code ADMINISTRATIVE})
 * @param reason         motivo; {@code null} en las voluntarias
 * @param releasedById   empleado que ejecuto la liberacion (titular o {@code ADMIN})
 * @param createdAt      instante de creacion (UTC)
 */
@Schema(description = "Datos de una liberacion de recurso")
public record ReleaseResponse(
        @Schema(description = "Identificador unico", example = "42")
        @JsonProperty("id") Long id,

        @Schema(description = "Recurso liberado", example = "8")
        @JsonProperty("parkingSpaceId") Long parkingSpaceId,

        @Schema(description = "Empleado titular cuyo recurso se libera", example = "15")
        @JsonProperty("employeeId") Long employeeId,

        @Schema(description = "Fecha liberada (ISO-8601)", example = "2026-07-10")
        @JsonProperty("releaseDate") LocalDate releaseDate,

        @Schema(description = "Tipo de liberacion", example = "VOLUNTARY")
        @JsonProperty("type") ReleaseType type,

        @Schema(description = "Motivo; null en las voluntarias", example = "Ausencia justificada")
        @JsonProperty("reason") String reason,

        @Schema(description = "Empleado que ejecuto la liberacion", example = "15")
        @JsonProperty("releasedById") Long releasedById,

        @Schema(description = "Instante de creacion (ISO-8601)")
        @JsonProperty("createdAt") Instant createdAt,

        @Schema(description = "Tipo de recurso liberado", example = "PARKING")
        @JsonProperty("resourceType") ResourceType resourceType) {

    /**
     * Mapea la entidad de persistencia a su DTO de salida.
     *
     * @param release entidad origen
     * @return el DTO equivalente
     */
    public static ReleaseResponse from(Release release) {
        return new ReleaseResponse(
                release.getId(),
                release.getResourceId(),
                release.getEmployeeId(),
                release.getReleaseDate(),
                release.getType(),
                release.getReason(),
                release.getReleasedById(),
                release.getCreatedAt(),
                release.getResourceType());
    }
}
