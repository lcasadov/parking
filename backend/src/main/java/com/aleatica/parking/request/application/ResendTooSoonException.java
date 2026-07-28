package com.aleatica.parking.request.application;

/**
 * Se lanza al intentar reenviar el aviso de una solicitud (change {@code request-resend-notice})
 * antes de que haya transcurrido el periodo minimo (24h) desde su creacion o desde el ultimo
 * reenvio, lo que ocurra mas tarde.
 *
 * <p>El manejador global la traduce a {@code 409 Conflict} con
 * {@code error = RESEND_TOO_SOON}.</p>
 */
public class ResendTooSoonException extends RuntimeException {

    /**
     * @param message mensaje legible por humanos
     */
    public ResendTooSoonException(String message) {
        super(message);
    }
}
