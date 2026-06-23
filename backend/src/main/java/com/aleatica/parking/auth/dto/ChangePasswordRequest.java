package com.aleatica.parking.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Solicitud de cambio de la propia contrasena.
 *
 * <p>DTO de entrada del contrato; la logica (politica de contrasena, verificacion
 * de la actual) la implementa el change funcional de {@code auth-local}.</p>
 *
 * @param currentPassword contrasena actual
 * @param newPassword     nueva contrasena propuesta
 */
@Schema(description = "Solicitud de cambio de contrasena")
public record ChangePasswordRequest(
        @Schema(description = "Contrasena actual")
        @NotBlank(message = "La contrasena actual es obligatoria")
        String currentPassword,

        @Schema(description = "Nueva contrasena")
        @NotBlank(message = "La nueva contrasena es obligatoria")
        String newPassword
) {
}
