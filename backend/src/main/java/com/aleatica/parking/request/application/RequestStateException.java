package com.aleatica.parking.request.application;

/**
 * Se lanza cuando se intenta una transicion de estado no permitida sobre una
 * solicitud (cancelar/aprobar/rechazar una solicitud que ya no esta en
 * {@code PENDING}).
 *
 * <p>El manejador global la traduce a {@code 409 Conflict} (spec Req 3/4/5 y casos
 * limite: no hay transiciones desde estados terminales).</p>
 */
public class RequestStateException extends RuntimeException {

    /**
     * @param message mensaje legible por humanos
     */
    public RequestStateException(String message) {
        super(message);
    }
}
