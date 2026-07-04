package com.aleatica.parking.release.application;

/**
 * Se lanza al intentar liberar un recurso para una fecha anterior a hoy.
 *
 * <p>El manejador global la traduce a {@code 400 Bad Request} con
 * {@code error = RELEASE_DATE_IN_PAST} y {@code fields.releaseDate} (spec Req 1). La
 * ventana ({@code releaseDate >= hoy}) es una regla del caso de uso evaluada con
 * {@code ClockPort}, no un {@code @Valid} sintactico.</p>
 */
public class ReleaseDateInPastException extends RuntimeException {

    /**
     * @param message mensaje legible por humanos
     */
    public ReleaseDateInPastException(String message) {
        super(message);
    }
}
