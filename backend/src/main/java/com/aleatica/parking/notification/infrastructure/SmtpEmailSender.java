package com.aleatica.parking.notification.infrastructure;

import com.aleatica.parking.notification.application.EmailAttachment;
import com.aleatica.parking.notification.application.EmailDeliveryException;
import com.aleatica.parking.notification.application.EmailMessage;
import com.aleatica.parking.notification.application.EmailSenderPort;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Adaptador SMTP de {@link EmailSenderPort}: envia el email HTML mediante
 * {@link JavaMailSender} (Ethereal en LOCAL/DES/PRE, SMTP corporativo en PRO; ver
 * {@code docs/PROJECT.md} §12 y {@code application-*.yml}).
 *
 * <p>Cualquier fallo del proveedor de correo se traduce a {@link EmailDeliveryException}
 * para que el servicio de entrega lo capture, lo registre y encole el email para
 * reintento, sin propagar el error al flujo de negocio.</p>
 */
@Component
public class SmtpEmailSender implements EmailSenderPort {

    private static final String CHARSET_UTF8 = "UTF-8";
    private static final String MSG_DELIVERY_FAILED = "Fallo al enviar email por SMTP";

    private final JavaMailSender mailSender;
    private final String fromAddress;

    /**
     * @param mailSender  remitente JavaMail autoconfigurado por Spring Boot
     * @param fromAddress direccion "from" de los correos ({@code parking.notifications.from})
     */
    public SmtpEmailSender(
            JavaMailSender mailSender,
            @Value("${parking.notifications.from:no-reply@parking.aleatica.com}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void send(EmailMessage message) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            boolean multipart = !message.attachments().isEmpty();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, multipart, CHARSET_UTF8);
            helper.setFrom(fromAddress);
            helper.setTo(message.to());
            helper.setSubject(message.subject());
            helper.setText(message.htmlBody(), true);
            for (EmailAttachment attachment : message.attachments()) {
                helper.addAttachment(attachment.filename(),
                        new ByteArrayResource(attachment.content()), attachment.contentType());
            }
            mailSender.send(mimeMessage);
        } catch (MessagingException | MailException ex) {
            throw new EmailDeliveryException(MSG_DELIVERY_FAILED, ex);
        }
    }
}
