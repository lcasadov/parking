package com.aleatica.parking.visitor.application;

/**
 * Indica que la plaza no esta disponible para la fecha de la reserva de visitante:
 * plaza inactiva, con asignacion fija activa no liberada ese dia, con una solicitud
 * aprobada esa fecha o con otra reserva de visitante para la misma plaza y fecha.
 *
 * <p>El manejador global la traduce a {@code 409 Conflict} con {@code error =
 * SPACE_NOT_AVAILABLE} y el detalle en {@code parkingSpaceId}. La comprobacion previa da
 * un mensaje claro; la violacion del indice unico {@code UX_visitor_reservations_space_date}
 * bajo concurrencia produce el mismo {@code 409} (red dura). La logica temporal de
 * disponibilidad consolidara la capability {@code availability-calendar} (B7).</p>
 */
public class SpaceNotAvailableForReservationException extends RuntimeException {

    /**
     * @param message mensaje legible por humanos
     */
    public SpaceNotAvailableForReservationException(String message) {
        super(message);
    }
}
