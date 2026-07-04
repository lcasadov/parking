package com.aleatica.parking.audit.application;

/**
 * Se lanza cuando una consulta de auditoria o de logins recibe una ventana temporal
 * invalida ({@code from} posterior a {@code to}).
 *
 * <p>La traduce el manejador global a {@code 400 Bad Request} con el detalle de la ventana
 * en {@code fields} (spec audit-retention, Req 1/2, validacion de ventana).</p>
 */
public class InvalidDateRangeException extends RuntimeException {

    /**
     * @param message mensaje legible que describe la ventana invalida
     */
    public InvalidDateRangeException(String message) {
        super(message);
    }
}
