package com.aleatica.parking.notification.application;

/**
 * Adjunto binario de un email ya renderizado, listo para transportarse dentro de un
 * {@link EmailMessage} y anadirse como parte MIME por el adaptador SMTP.
 *
 * <p>Es autocontenido (nombre, tipo MIME y contenido en memoria) para que el reintento
 * pueda reenviar el correo sin volver a cargar el recurso de origen. No contiene entidades
 * JPA. El contenido se conserva por referencia; el productor no debe mutarlo tras crearlo.</p>
 *
 * @param filename    nombre del fichero adjunto (p. ej. {@code floor-plan.png})
 * @param contentType tipo MIME del adjunto (p. ej. {@code image/png})
 * @param content     contenido binario del adjunto
 */
public record EmailAttachment(String filename, String contentType, byte[] content) {
}
