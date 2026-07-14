package com.aleatica.parking.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.aleatica.parking.auth.domain.ClockPort;
import com.aleatica.parking.notification.EmailOutbox;
import com.aleatica.parking.notification.EmailOutboxRepository;
import com.aleatica.parking.notification.EmailOutboxStatus;
import com.aleatica.parking.notification.NotificationEventType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests unitarios de {@link NotificationDeliveryService}: entrega resiliente (render + envio
 * inmediato / encolado de la ORDEN ante fallo SMTP) y reintento del job (re-render + envio;
 * exito -> SENT, fallo -> PENDING/FAILED, destinatario irresoluble -> FAILED, idempotencia).
 * Con {@link EmailSenderPort}, {@link NotificationRenderer}, {@link NotificationOutboxMapper}
 * y repositorio mockeados.
 */
@ExtendWith(MockitoExtension.class)
class NotificationDeliveryServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-04T10:00:00Z");
    private static final int MAX_ATTEMPTS = 3;
    private static final Long RECIPIENT_ID = 15L;
    private static final NotificationCommand COMMAND =
            new NotificationCommand(NotificationEventType.ASSIGNMENT_REVOKED, RECIPIENT_ID, null);
    private static final EmailMessage MESSAGE =
            new EmailMessage("emp@aleatica.com", "asunto", "<p>cuerpo</p>");

    @Mock
    private EmailSenderPort emailSenderPort;

    @Mock
    private EmailOutboxRepository outboxRepository;

    @Mock
    private PendingEmailStore pendingEmailStore;

    @Mock
    private NotificationRenderer notificationRenderer;

    @Mock
    private NotificationOutboxMapper outboxMapper;

    private final ClockPort clock = () -> NOW;

    private NotificationDeliveryService service() {
        return new NotificationDeliveryService(
                emailSenderPort, outboxRepository, pendingEmailStore,
                notificationRenderer, outboxMapper, clock, MAX_ATTEMPTS);
    }

    private static EmailOutbox pendingEntry() {
        return EmailOutbox.pending(
                NotificationEventType.ASSIGNMENT_REVOKED, RECIPIENT_ID, null, "smtp down", NOW);
    }

    @Test
    void shouldSendImmediately_whenRenderSucceeds() {
        // Arrange
        given(notificationRenderer.render(COMMAND)).willReturn(Optional.of(MESSAGE));

        // Act
        service().dispatch(COMMAND);

        // Assert: envio directo, sin encolar
        verify(emailSenderPort).send(MESSAGE);
        verify(pendingEmailStore, never()).queue(any());
    }

    @Test
    void shouldNotSendNorQueue_whenRecipientNotResolved() {
        // Arrange: el destinatario ya no existe -> el renderer no produce mensaje
        given(notificationRenderer.render(COMMAND)).willReturn(Optional.empty());

        // Act
        service().dispatch(COMMAND);

        // Assert: no se envia ni se encola nada
        verify(emailSenderPort, never()).send(any());
        verify(pendingEmailStore, never()).queue(any());
    }

    @Test
    void shouldQueueCommand_whenSmtpFailsOnImmediateSend() {
        // Arrange
        EmailOutbox outbox = pendingEntry();
        given(notificationRenderer.render(COMMAND)).willReturn(Optional.of(MESSAGE));
        willThrow(new EmailDeliveryException("smtp down", new RuntimeException()))
                .given(emailSenderPort).send(MESSAGE);
        given(outboxMapper.toPendingOutbox(COMMAND, "smtp down", NOW)).willReturn(outbox);

        // Act: no debe relanzar (la operacion origen ya esta confirmada)
        service().dispatch(COMMAND);

        // Assert: se encola la ORDEN del evento (no el HTML) para reintento
        verify(pendingEmailStore).queue(outbox);
    }

    @Test
    void shouldMarkSent_whenRetrySucceeds() {
        // Arrange
        EmailOutbox pending = pendingEntry();
        given(outboxRepository.findByStatus(EmailOutboxStatus.PENDING)).willReturn(List.of(pending));
        given(outboxMapper.toCommand(pending)).willReturn(COMMAND);
        given(notificationRenderer.render(COMMAND)).willReturn(Optional.of(MESSAGE));

        // Act
        service().retryPending();

        // Assert
        assertThat(pending.getStatus()).isEqualTo(EmailOutboxStatus.SENT);
        assertThat(pending.getSentAt()).isEqualTo(NOW);
        verify(emailSenderPort).send(MESSAGE);
    }

    @Test
    void shouldKeepPending_whenRetryFailsBelowMaxAttempts() {
        // Arrange (attempts=1 tras el fallo inicial; el reintento lo lleva a 2 < 3)
        EmailOutbox pending = pendingEntry();
        given(outboxRepository.findByStatus(EmailOutboxStatus.PENDING)).willReturn(List.of(pending));
        given(outboxMapper.toCommand(pending)).willReturn(COMMAND);
        given(notificationRenderer.render(COMMAND)).willReturn(Optional.of(MESSAGE));
        willThrow(new EmailDeliveryException("still down", new RuntimeException()))
                .given(emailSenderPort).send(MESSAGE);

        // Act
        service().retryPending();

        // Assert
        assertThat(pending.getStatus()).isEqualTo(EmailOutboxStatus.PENDING);
        assertThat(pending.getAttempts()).isEqualTo(2);
    }

    @Test
    void shouldMarkFailed_whenRetryExhaustsMaxAttempts() {
        // Arrange: fila ya en su ultimo intento posible (attempts=1 -> 2 -> 3 = MAX)
        EmailOutbox pending = pendingEntry();
        pending.registerFailedRetry(NOW, "again", MAX_ATTEMPTS); // attempts=2, sigue PENDING
        given(outboxRepository.findByStatus(EmailOutboxStatus.PENDING)).willReturn(List.of(pending));
        given(outboxMapper.toCommand(pending)).willReturn(COMMAND);
        given(notificationRenderer.render(COMMAND)).willReturn(Optional.of(MESSAGE));
        willThrow(new EmailDeliveryException("still down", new RuntimeException()))
                .given(emailSenderPort).send(MESSAGE);

        // Act
        service().retryPending();

        // Assert: al alcanzar MAX_ATTEMPTS pasa a FAILED (no se reintenta mas)
        assertThat(pending.getAttempts()).isEqualTo(MAX_ATTEMPTS);
        assertThat(pending.getStatus()).isEqualTo(EmailOutboxStatus.FAILED);
    }

    @Test
    void shouldMarkFailed_whenRetryRecipientIsUnresolvable() {
        // Arrange: el destinatario fue eliminado -> el render no produce mensaje
        EmailOutbox pending = pendingEntry();
        given(outboxRepository.findByStatus(EmailOutboxStatus.PENDING)).willReturn(List.of(pending));
        given(outboxMapper.toCommand(pending)).willReturn(COMMAND);
        given(notificationRenderer.render(COMMAND)).willReturn(Optional.empty());

        // Act
        service().retryPending();

        // Assert: fila irrecuperable -> FAILED, sin intentar el envio
        assertThat(pending.getStatus()).isEqualTo(EmailOutboxStatus.FAILED);
        verify(emailSenderPort, never()).send(any());
    }

    @Test
    void shouldNotResendAlreadySent_whenRetryJobRunsAgain() {
        // Arrange: no hay filas PENDING (las SENT/FAILED no se consultan -> idempotencia)
        given(outboxRepository.findByStatus(EmailOutboxStatus.PENDING)).willReturn(List.of());

        // Act
        service().retryPending();

        // Assert: no se reenvia nada
        verify(emailSenderPort, never()).send(any());
    }

    @Test
    void shouldNotThrow_whenSenderPortIsUnavailable() {
        // Arrange: garantiza que dispatch nunca propaga (contrato de resiliencia)
        given(notificationRenderer.render(COMMAND)).willReturn(Optional.of(MESSAGE));
        willThrow(new EmailDeliveryException("down", new RuntimeException()))
                .given(emailSenderPort).send(MESSAGE);
        given(outboxMapper.toPendingOutbox(COMMAND, "down", NOW)).willReturn(pendingEntry());

        // Act / Assert: no lanza
        service().dispatch(COMMAND);
        verify(pendingEmailStore).queue(any());
    }
}
