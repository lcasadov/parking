package com.aleatica.parking.auth.application;

/**
 * La {@code currentPassword} aportada en el cambio de contrasena no coincide
 * con la actual -> {@code 400} con detalle en el campo {@code currentPassword}.
 */
public class InvalidCurrentPasswordException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Crea la excepcion con un mensaje fijo para el campo {@code currentPassword}. */
    public InvalidCurrentPasswordException() {
        super("La contrasena actual no es correcta");
    }
}
