package com.aleatica.parking.auth.dto;

import com.aleatica.parking.auth.application.AuthenticatedUser;
import com.aleatica.parking.employee.Role;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Identidad del usuario autenticado devuelta por {@code /auth/login} y
 * {@code /auth/me} (schema {@code CurrentUser} del contrato).
 *
 * <p>{@code passwordMustChange} es obligatorio en el contrato: el frontend lo
 * usa siempre para decidir si fuerza el cambio de contrasena. Nunca expone
 * datos sensibles (hash, intentos, bloqueo).</p>
 *
 * @param employeeId         id del empleado
 * @param login              login del empleado
 * @param firstName          nombre (puede ser {@code null} si no aplica)
 * @param lastName           apellidos (puede ser {@code null} si no aplica)
 * @param role               rol funcional
 * @param passwordMustChange si debe cambiar la contrasena antes de operar
 */
@Schema(description = "Identidad del usuario autenticado")
public record CurrentUser(
        @Schema(description = "Id del empleado", example = "42")
        Long employeeId,

        @Schema(description = "Login del empleado", example = "jperez")
        String login,

        @Schema(description = "Nombre", example = "Juan")
        String firstName,

        @Schema(description = "Apellidos", example = "Perez")
        String lastName,

        @Schema(description = "Rol funcional")
        Role role,

        @Schema(description = "Si debe cambiar la contrasena antes de operar", example = "false")
        boolean passwordMustChange) {

    /**
     * Construye el DTO a partir de la identidad de dominio.
     *
     * @param user identidad autenticada del caso de uso
     * @return el DTO de respuesta del contrato
     */
    public static CurrentUser from(AuthenticatedUser user) {
        return new CurrentUser(
                user.employeeId(),
                user.login(),
                user.firstName(),
                user.lastName(),
                user.role(),
                user.passwordMustChange());
    }
}
