package com.aleatica.parking.parkingspace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo de la configuracion masiva del total de plazas
 * (schema {@code ParkingSpaceConfigureRequest} de la API).
 *
 * <p>{@code total} es el numero total de plazas activas deseado ({@code >= 0}). El
 * caso de uso ajusta el parque a ese total preservando el historico existente
 * (design §Decisions).</p>
 *
 * @param total numero total de plazas activas deseado (obligatorio, {@code >= 0})
 */
@Schema(description = "Numero total de plazas a configurar")
public record ParkingSpaceConfigureRequest(
        @Schema(description = "Total de plazas activas deseado", example = "50")
        @NotNull @Min(0) Integer total) {
}
