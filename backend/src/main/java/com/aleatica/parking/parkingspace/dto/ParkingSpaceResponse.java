package com.aleatica.parking.parkingspace.dto;

import com.aleatica.parking.parkingspace.ParkingSpace;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Representacion de salida de una plaza (schema {@code ParkingSpace} de la API).
 *
 * <p>Es un DTO, no la entidad JPA (S4684 / OWASP API3). Expone el {@code number}
 * (entero identificador) y la {@code floor} (planta) DERIVADA del numero
 * ({@code number / 1000}, solo lectura, design §Decision 1), junto al {@code label}
 * derivado. El flag {@code active} viaja bajo la clave contractual {@code active}
 * (autoridad {@code docs/openapi.yaml}); se fija con {@link JsonProperty} para que
 * Jackson no derive otra clave del componente record (regresion del bug #21 con
 * {@code isCorporate}).</p>
 *
 * @param id        identificador
 * @param number    numero unico de la plaza
 * @param label     etiqueta derivada del numero
 * @param floor     planta derivada ({@code number / 1000}), solo lectura
 * @param active    si la plaza esta activa (entra en disponibilidad)
 * @param createdAt instante de alta (UTC)
 */
@Schema(description = "Datos de una plaza de parking")
public record ParkingSpaceResponse(
        @Schema(description = "Identificador unico", example = "42") Long id,
        @Schema(description = "Numero unico de la plaza", example = "1007") Integer number,
        @Schema(description = "Etiqueta derivada del numero", example = "1007") String label,
        @Schema(description = "Planta derivada (number/1000), solo lectura", example = "1")
        Integer floor,
        @Schema(description = "Estado activo de la plaza", example = "true")
        @JsonProperty("active") boolean active,
        @Schema(description = "Instante de alta (ISO-8601)") Instant createdAt) {

    /**
     * Mapea la entidad de persistencia a su DTO de salida, derivando la planta.
     *
     * @param space entidad origen
     * @return el DTO equivalente
     */
    public static ParkingSpaceResponse from(ParkingSpace space) {
        return new ParkingSpaceResponse(
                space.getId(),
                space.getNumber(),
                space.getLabel(),
                space.floor(),
                space.isActive(),
                space.getCreatedAt());
    }
}
