package com.aleatica.parking.visitor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos de alta/edicion de un vehiculo de visitante (change {@code visitor-vehicles}).
 *
 * <p>Solo {@code licensePlate} (matricula) es obligatoria; marca, modelo y color son opcionales.
 * La validacion sintactica ({@code @NotBlank} + {@code @Size}) es la primera capa (UX y 400); la
 * unicidad de la matricula por visitante se garantiza en la capa de aplicacion/BD (409). Un unico
 * record sirve para el alta y la edicion (misma forma), evitando duplicacion (S1192).</p>
 *
 * @param licensePlate matricula (obligatoria, se normaliza a mayusculas/trim al persistir)
 * @param brand        marca (opcional)
 * @param model        modelo (opcional)
 * @param color        color (opcional)
 */
@Schema(description = "Datos de un vehiculo de visitante (solo la matricula es obligatoria)")
public record VisitorVehicleRequest(
        @Schema(description = "Matricula del vehiculo", example = "1234ABC")
        @NotBlank(message = "La matricula es obligatoria")
        @Size(max = 15, message = "La matricula no puede superar 15 caracteres")
        String licensePlate,

        @Schema(description = "Marca del vehiculo", example = "Seat")
        @Size(max = 60, message = "La marca no puede superar 60 caracteres")
        String brand,

        @Schema(description = "Modelo del vehiculo", example = "Leon")
        @Size(max = 60, message = "El modelo no puede superar 60 caracteres")
        String model,

        @Schema(description = "Color del vehiculo", example = "Gris")
        @Size(max = 30, message = "El color no puede superar 30 caracteres")
        String color) {
}
