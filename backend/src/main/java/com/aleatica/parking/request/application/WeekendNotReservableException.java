package com.aleatica.parking.request.application;

/**
 * Se lanza cuando se intenta crear una solicitud (POST /requests) para un sabado o domingo estando
 * deshabilitadas las reservas de fin de semana (ajuste global {@code weekendReservable = false},
 * change {@code reservas-employee-admin-reassign}).
 *
 * <p>El manejador global la traduce a {@code 400 Bad Request} con
 * {@code error = WEEKEND_NOT_RESERVABLE} y {@code fields.requestedDate}, en paralelo a
 * {@link OutsideRequestWindowException} (ambas son reglas de validacion de la fecha solicitada).</p>
 */
public class WeekendNotReservableException extends RuntimeException {

    /**
     * @param message mensaje legible por humanos
     */
    public WeekendNotReservableException(String message) {
        super(message);
    }
}
