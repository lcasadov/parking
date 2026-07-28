package com.aleatica.parking.request.application;

/**
 * Se lanza al intentar reenviar el aviso de una solicitud (change {@code request-resend-notice})
 * que no esta en estado {@link com.aleatica.parking.request.domain.RequestStatus#PENDING}: solo
 * tiene sentido reavisar al admin de una solicitud aun sin resolver.
 *
 * <p>El manejador global la traduce a {@code 409 Conflict} con
 * {@code error = REQUEST_NOT_PENDING}.</p>
 */
public class RequestNotPendingException extends RuntimeException {

    /**
     * @param message mensaje legible por humanos
     */
    public RequestNotPendingException(String message) {
        super(message);
    }
}
