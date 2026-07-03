package com.aleatica.parking.request.application;

/**
 * Se lanza cuando la {@code requestedDate} de una solicitud cae fuera de la ventana
 * permitida (hoy..hoy+14 dias naturales, extremos inclusive).
 *
 * <p>El manejador global la traduce a {@code 400 Bad Request} con
 * {@code error = OUTSIDE_REQUEST_WINDOW} y {@code fields.requestedDate} (design
 * §Decisions, spec Req 1).</p>
 */
public class OutsideRequestWindowException extends RuntimeException {

    /**
     * @param message mensaje legible por humanos
     */
    public OutsideRequestWindowException(String message) {
        super(message);
    }
}
