package com.aleatica.parking.visitor.application;

/**
 * Indica el intento de anular una reserva de visitante cuya fecha es anterior a hoy.
 *
 * <p>El manejador global la traduce a {@code 400 Bad Request} con {@code error =
 * VISITOR_RESERVATION_NOT_CANCELLABLE}: el {@code ADMIN} solo puede anular reservas
 * futuras (spec Req 3).</p>
 */
public class PastVisitorReservationCancellationException extends RuntimeException {

    /**
     * @param message mensaje legible por humanos
     */
    public PastVisitorReservationCancellationException(String message) {
        super(message);
    }
}
