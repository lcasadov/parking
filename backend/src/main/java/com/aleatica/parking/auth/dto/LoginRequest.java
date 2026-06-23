package com.aleatica.parking.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Credenciales de inicio de sesion de Fase 1 (login local).
 *
 * <p>DTO de entrada del contrato; la verificacion real la implementa el change
 * funcional de {@code auth-local}. La validacion sintactica ({@code @NotBlank})
 * ya esta activa para garantizar el formato del cuerpo.</p>
 *
 * @param login    identificador del empleado
 * @param password contrasena en claro (se verifica contra el hash BCrypt)
 */
@Schema(description = "Credenciales de inicio de sesion (Fase 1)")
public record LoginRequest(
        @Schema(description = "Login del empleado", example = "jperez")
        @NotBlank(message = "El login es obligatorio")
        String login,

        @Schema(description = "Contrasena en claro")
        @NotBlank(message = "La contrasena es obligatoria")
        String password
) {
}
