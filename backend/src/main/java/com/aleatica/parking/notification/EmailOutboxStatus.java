package com.aleatica.parking.notification;

/**
 * Estado de una entrada del almacen de reintento de emails ({@code dbo.email_outbox}).
 *
 * <p>El job de reintento SOLO procesa {@link #PENDING}; {@link #SENT} y {@link #FAILED}
 * son terminales, de modo que un email enviado nunca se reenvia (idempotencia) y uno
 * que agota la politica de reintentos deja de intentarse.</p>
 */
public enum EmailOutboxStatus {

    /** Pendiente de reintento tras un fallo SMTP. */
    PENDING,

    /** Enviado con exito (terminal). */
    SENT,

    /** Agotada la politica de maximo de reintentos (terminal). */
    FAILED
}
