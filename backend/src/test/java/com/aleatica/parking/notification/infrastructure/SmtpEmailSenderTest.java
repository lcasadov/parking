package com.aleatica.parking.notification.infrastructure;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.aleatica.parking.notification.application.EmailAttachment;
import com.aleatica.parking.notification.application.EmailDeliveryException;
import com.aleatica.parking.notification.application.EmailMessage;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Tests unitarios de {@link SmtpEmailSender}: envio correcto por {@link JavaMailSender} y
 * traduccion de un fallo del proveedor a {@link EmailDeliveryException}. El
 * {@code JavaMailSender} esta mockeado (no hay SMTP real).
 */
@ExtendWith(MockitoExtension.class)
class SmtpEmailSenderTest {

    private static final String FROM = "no-reply@parking.aleatica.com";
    private static final EmailMessage MESSAGE =
            new EmailMessage("emp@aleatica.com", "asunto", "<p>cuerpo</p>");

    @Mock
    private JavaMailSender mailSender;

    private SmtpEmailSender sender() {
        return new SmtpEmailSender(mailSender, FROM);
    }

    @Test
    void shouldSendMimeMessage_whenMessageValid() {
        // Arrange
        MimeMessage mime = new MimeMessage((jakarta.mail.Session) null);
        given(mailSender.createMimeMessage()).willReturn(mime);

        // Act
        sender().send(MESSAGE);

        // Assert
        verify(mailSender).send(mime);
    }

    @Test
    void shouldSendMultipartMimeMessage_whenMessageHasAttachment() {
        // Arrange: mensaje con un adjunto (plano de la planta) -> envio multipart
        MimeMessage mime = new MimeMessage((jakarta.mail.Session) null);
        given(mailSender.createMimeMessage()).willReturn(mime);
        EmailMessage withAttachment = MESSAGE.withAttachments(List.of(
                new EmailAttachment("floor-plan.png", "image/png", new byte[] {1, 2, 3})));

        // Act
        sender().send(withAttachment);

        // Assert: se recorre el bucle addAttachment y se envia el MimeMessage
        verify(mailSender).send(mime);
    }

    @Test
    void shouldThrowDeliveryException_whenMailSenderFails() {
        // Arrange
        MimeMessage mime = new MimeMessage((jakarta.mail.Session) null);
        given(mailSender.createMimeMessage()).willReturn(mime);
        org.mockito.BDDMockito.willThrow(new MailSendException("smtp down"))
                .given(mailSender).send(mime);

        // Act / Assert
        assertThatThrownBy(() -> sender().send(MESSAGE))
                .isInstanceOf(EmailDeliveryException.class);
    }
}
