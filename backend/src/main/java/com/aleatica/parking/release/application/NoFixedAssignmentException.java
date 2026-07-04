package com.aleatica.parking.release.application;

/**
 * Se lanza cuando no existe una asignacion fija activa del recurso para el dia de la
 * semana de la fecha a liberar (no hay recurso fijo que liberar esa fecha), o cuando la
 * resolucion implicita de la plaza es ambigua (varias asignaciones ese dia).
 *
 * <p>El manejador global la traduce a {@code 409 Conflict} con
 * {@code error = NO_FIXED_ASSIGNMENT} (spec Req 1 y casos limite).</p>
 */
public class NoFixedAssignmentException extends RuntimeException {

    /**
     * @param message mensaje legible por humanos
     */
    public NoFixedAssignmentException(String message) {
        super(message);
    }
}
