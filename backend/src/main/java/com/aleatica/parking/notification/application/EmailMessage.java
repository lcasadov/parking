package com.aleatica.parking.notification.application;

/**
 * Mensaje de email ya renderizado, listo para enviar por SMTP.
 *
 * <p>Es el contrato entre el renderizado (plantillas Thymeleaf) y el envio: transporta
 * el cuerpo HTML final, de modo que el reintento pueda reenviar sin re-resolver
 * destinatarios ni re-renderizar plantillas. No contiene entidades JPA.</p>
 *
 * @param to       direccion de email del destinatario
 * @param subject  asunto del correo
 * @param htmlBody cuerpo del correo en HTML (ya renderizado)
 */
public record EmailMessage(String to, String subject, String htmlBody) {
}
