package com.aleatica.parking.notification.application;

import java.util.List;

/**
 * Mensaje de email ya renderizado, listo para enviar por SMTP.
 *
 * <p>Es el contrato entre el renderizado (plantillas Thymeleaf) y el envio: transporta
 * el cuerpo HTML final y sus adjuntos, de modo que el reintento pueda reenviar sin
 * re-resolver destinatarios ni re-renderizar plantillas. No contiene entidades JPA.</p>
 *
 * <p>La lista de {@code attachments} nunca es {@code null} (se normaliza a lista vacia
 * inmutable): los correos sin adjunto usan el constructor de tres argumentos y se envian
 * exactamente igual que antes de habilitar los adjuntos (retrocompatibilidad).</p>
 *
 * @param to          direccion de email del destinatario
 * @param subject     asunto del correo
 * @param htmlBody    cuerpo del correo en HTML (ya renderizado)
 * @param attachments adjuntos binarios (posiblemente vacio, nunca {@code null})
 */
public record EmailMessage(String to, String subject, String htmlBody, List<EmailAttachment> attachments) {

    /**
     * Normaliza {@code attachments} a una copia inmutable (o lista vacia si es {@code null}).
     */
    public EmailMessage {
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
    }

    /**
     * Construye un mensaje sin adjuntos (comportamiento previo a los adjuntos MIME).
     *
     * @param to       direccion del destinatario
     * @param subject  asunto
     * @param htmlBody cuerpo HTML renderizado
     */
    public EmailMessage(String to, String subject, String htmlBody) {
        this(to, subject, htmlBody, List.of());
    }

    /**
     * Devuelve una copia de este mensaje con los adjuntos indicados, conservando
     * destinatario, asunto y cuerpo (el record es inmutable).
     *
     * @param attachments adjuntos a incorporar
     * @return un nuevo mensaje con los adjuntos dados
     */
    public EmailMessage withAttachments(List<EmailAttachment> attachments) {
        return new EmailMessage(to, subject, htmlBody, attachments);
    }
}
