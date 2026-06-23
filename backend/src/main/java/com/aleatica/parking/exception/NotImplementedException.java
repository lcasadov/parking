package com.aleatica.parking.exception;

/**
 * Senala una funcionalidad declarada pero aun no implementada.
 *
 * <p>La usan los endpoints placeholder de Fase 1 ({@code /api/v1/auth/*}), que
 * existen en el contrato pero cuya logica la aporta el change funcional de
 * {@code auth-local}. El manejador global la traduce a {@code 501 Not Implemented}.</p>
 */
public class NotImplementedException extends RuntimeException {

    /**
     * @param message descripcion de la funcionalidad pendiente
     */
    public NotImplementedException(String message) {
        super(message);
    }
}
