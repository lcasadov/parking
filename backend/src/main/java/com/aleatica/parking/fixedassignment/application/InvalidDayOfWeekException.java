package com.aleatica.parking.fixedassignment.application;

/**
 * Senala que la lista de dias de la semana contiene un valor fuera del rango 1-7
 * (o un elemento nulo) al establecer una asignacion fija.
 *
 * <p>El manejador global la traduce a {@code 400 Bad Request} con el cuerpo uniforme
 * {@code { error, message, fields, timestamp }}, indicando el campo {@code daysOfWeek}.
 * Es la verificacion de rango de la capa de aplicacion (frontera de seguridad,
 * complementaria a la validacion sintactica del DTO, OWASP A04).</p>
 */
public class InvalidDayOfWeekException extends RuntimeException {

    /**
     * @param message mensaje legible por humanos
     */
    public InvalidDayOfWeekException(String message) {
        super(message);
    }
}
