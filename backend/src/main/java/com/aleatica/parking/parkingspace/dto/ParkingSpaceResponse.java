package com.aleatica.parking.parkingspace.dto;

import com.aleatica.parking.parkingspace.ParkingSpace;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Representacion de salida de una plaza (schema {@code ParkingSpace} de la API).
 *
 * <p>Es un DTO, no la entidad JPA (S4684 / OWASP API3). El flag {@code active}
 * viaja bajo la clave contractual {@code active} (autoridad {@code docs/openapi.yaml});
 * se fija con {@link JsonProperty} para que Jackson no derive otra clave del
 * componente record (regresion del patron del bug #21 con {@code isCorporate}).</p>
 *
 * @param id        identificador
 * @param label     etiqueta unica
 * @param active    si la plaza esta activa (entra en disponibilidad)
 * @param createdAt instante de alta (UTC)
 */
@Schema(description = "Datos de una plaza de parking")
public record ParkingSpaceResponse(
        @Schema(description = "Identificador unico", example = "42") Long id,
        @Schema(description = "Etiqueta unica de la plaza", example = "P-08") String label,
        @Schema(description = "Estado activo de la plaza", example = "true")
        @JsonProperty("active") boolean active,
        @Schema(description = "Instante de alta (ISO-8601)") Instant createdAt) {

    /**
     * Mapea la entidad de persistencia a su DTO de salida.
     *
     * @param space entidad origen
     * @return el DTO equivalente
     */
    public static ParkingSpaceResponse from(ParkingSpace space) {
        return new ParkingSpaceResponse(
                space.getId(),
                space.getLabel(),
                space.isActive(),
                space.getCreatedAt());
    }
}
