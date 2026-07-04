package com.aleatica.parking.request.application;

/**
 * Se lanza al aprobar una solicitud cuando la plaza elegida no esta disponible para
 * la fecha (asignacion fija activa ese dia de la semana, u otra solicitud
 * {@code APPROVED} para esa plaza y fecha).
 *
 * <p>El manejador global la traduce a {@code 409 Conflict} con
 * {@code error = SPACE_NOT_AVAILABLE} (spec Req 4). La colision por concurrencia entre
 * administradores la detecta ademas el indice unico filtrado
 * {@code UX_requests_space_date_approved}, traducido al mismo estado. Logica de
 * disponibilidad temporal que consolidara {@code availability-calendar} (B7).</p>
 */
public class SpaceUnavailableException extends RuntimeException {

    /**
     * @param message mensaje legible por humanos
     */
    public SpaceUnavailableException(String message) {
        super(message);
    }
}
