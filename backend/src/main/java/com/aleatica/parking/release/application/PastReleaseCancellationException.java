package com.aleatica.parking.release.application;

/**
 * Se lanza al intentar cancelar una liberacion cuya fecha ya es pasada (solo se pueden
 * anular liberaciones presentes o futuras).
 *
 * <p>El manejador global la traduce a {@code 409 Conflict} con
 * {@code error = RELEASE_NOT_CANCELLABLE} (spec Req 2).</p>
 */
public class PastReleaseCancellationException extends RuntimeException {

    /**
     * @param message mensaje legible por humanos
     */
    public PastReleaseCancellationException(String message) {
        super(message);
    }
}
