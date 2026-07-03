package com.aleatica.parking.auth.application;

import com.aleatica.parking.employee.Role;

/**
 * Identidad del usuario autenticado que el dominio devuelve al adaptador web.
 *
 * <p>No es una entidad JPA: nunca expone {@code passwordHash},
 * {@code failedLoginAttempts} ni {@code lockedUntil} (security-design §1 /
 * OWASP API3). El controlador lo mapea al DTO {@code CurrentUser}.</p>
 *
 * @param employeeId         id del empleado
 * @param login              login del empleado
 * @param firstName          nombre
 * @param lastName           apellidos
 * @param role               rol funcional
 * @param passwordMustChange si debe cambiar la contrasena antes de operar
 */
public record AuthenticatedUser(
        Long employeeId,
        String login,
        String firstName,
        String lastName,
        Role role,
        boolean passwordMustChange) {
}
