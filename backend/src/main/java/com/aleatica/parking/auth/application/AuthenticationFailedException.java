package com.aleatica.parking.auth.application;

/**
 * Fallo de autenticacion local (Fase 1) -> {@code 401}.
 *
 * <p>Lleva siempre un mensaje <strong>generico</strong>: no revela si el login
 * existe, si la contrasena es incorrecta o si la cuenta esta inactiva, para no
 * habilitar la enumeracion de usuarios (security-design §2, OWASP API2). El
 * detalle real (resultado fino) viaja solo a {@code login_log}.</p>
 */
public class AuthenticationFailedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Mensaje generico unico para cualquier causa de fallo de login. */
    public static final String GENERIC_MESSAGE = "Credenciales invalidas o cuenta no disponible";

    /** Crea la excepcion con el mensaje generico. */
    public AuthenticationFailedException() {
        super(GENERIC_MESSAGE);
    }
}
