package com.aleatica.parking.export.application;

/**
 * Se lanza cuando un usuario supera el limite de exportaciones por ventana temporal
 * (5 por minuto y usuario, {@code docs/security-design.md} §7).
 *
 * <p>El {@code GlobalExceptionHandler} la traduce a {@code 429 Too Many Requests} con el cuerpo
 * uniforme {@code ApiError}. Frena la exfiltracion masiva de datos personales (spec Req 4).</p>
 */
public class ExportRateLimitExceededException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message mensaje legible por humanos (sin detalle sensible)
     */
    public ExportRateLimitExceededException(String message) {
        super(message);
    }
}
