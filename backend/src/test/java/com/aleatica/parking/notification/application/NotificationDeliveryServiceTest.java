package com.aleatica.parking.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.aleatica.parking.auth.domain.ClockPort;
import com.aleatica.parking.notification.EmailOutbox;
import com.aleatica.parking.notification.EmailOutboxRepository;
import com.aleatica.parking.notification.EmailOutboxStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests unitarios de {@link NotificationDeliveryService}: entrega resiliente (envio
 * inmediato / encolado ante fallo SMTP) y reintento del job (exito -> SENT, fallo ->
 * PENDING/FAILED, idempotencia). Con {@link EmailSenderPort} y repositorio mockeados.
 */
@ExtendWith(MockitoExtension.class)
class NotificationDeliveryServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-04T10:00:00Z");
    private static final int MAX_ATTEMPTS = 3;
    private static final EmailMessage MESSAGE = new EmailMessage("emp@aleatica.com", "asunto", "<p>cuerpo</p>");

    @Mock
    private EmailSenderPort emailSenderPort;

    @Mock
    private EmailOutboxRepository outboxRepository;

    @Mock
    private PendingEmailStore pendingEmailStore;

    private final ClockPort clock = () -> NOW;

    private NotificationDeliveryService service() {
        return new NotificationDeliveryService(
                emailSenderPort, outboxRepository, pendingEmailStore, clock, MAX_ATTEMPTS);
    }

    @Test
    void shouldNotPersistOutbox_whenImmediateSendSucceeds() {
        // Act
        service().sendOrQueue(MESSAGE);

        // Assert: envio directo, sin encolar
        verify(emailSenderPort).send(MESSAGE);
        verify(pendingEmailStore, never()).queue(any(), anyString(), any());
    }

    @Test
    void shouldQueuePendingEmail_whenSmtpFailsOnImmediateSend() {
        // Arrange
        willThrow(new EmailDeliveryException("smtp down", new RuntimeException()))
                .given(emailSenderPort).send(MESSAGE);

        // Act: no debe relanzar (la operacion origen ya esta confirmada)
        service().sendOrQueue(MESSAGE);

        // Assert: se encola el mensaje renderizado para reintento (transaccion nueva)
        verify(pendingEmailStore).queue(eq(MESSAGE), eq("smtp down"), eq(NOW));
    }

    @Test
    void shouldMarkSent_whenRetrySucceeds() {
        // Arrange
        EmailOutbox pending = EmailOutbox.pending(MESSAGE, "smtp down", NOW);
        given(outboxRepository.findByStatus(EmailOutboxStatus.PENDING)).willReturn(List.of(pending));

        // Act
        service().retryPending();

        // Assert
        assertThat(pending.getStatus()).isEqualTo(EmailOutboxStatus.SENT);
        assertThat(pending.getSentAt()).isEqualTo(NOW);
    }

    @Test
    void shouldKeepPending_whenRetryFailsBelowMaxAttempts() {
        // Arrange (attempts=1 tras el fallo inicial; el reintento lo lleva a 2 < 3)
        EmailOutbox pending = EmailOutbox.pending(MESSAGE, "smtp down", NOW);
        given(outboxRepository.findByStatus(EmailOutboxStatus.PENDING)).willReturn(List.of(pending));
        willThrow(new EmailDeliveryException("still down", new RuntimeException()))
                .given(emailSenderPort).send(any());

        // Act
        service().retryPending();

        // Assert
        assertThat(pending.getStatus()).isEqualTo(EmailOutboxStatus.PENDING);
        assertThat(pending.getAttempts()).isEqualTo(2);
    }

    @Test
    void shouldMarkFailed_whenRetryExhaustsMaxAttempts() {
        // Arrange: fila ya en su ultimo intento posible (attempts=1 -> 2 -> 3 = MAX)
        EmailOutbox pending = EmailOutbox.pending(MESSAGE, "smtp down", NOW);
        pending.registerFailedRetry(NOW, "again", MAX_ATTEMPTS); // attempts=2, sigue PENDING
        given(outboxRepository.findByStatus(EmailOutboxStatus.PENDING)).willReturn(List.of(pending));
        willThrow(new EmailDeliveryException("still down", new RuntimeException()))
                .given(emailSenderPort).send(any());

        // Act
        service().retryPending();

        // Assert: al alcanzar MAX_ATTEMPTS pasa a FAILED (no se reintenta mas)
        assertThat(pending.getAttempts()).isEqualTo(MAX_ATTEMPTS);
        assertThat(pending.getStatus()).isEqualTo(EmailOutboxStatus.FAILED);
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
        // Arrange: garantiza que sendOrQueue nunca propaga (contrato de resiliencia)
        EmailSenderPort failing = mock(EmailSenderPort.class);
        willThrow(new EmailDeliveryException("down", new RuntimeException())).given(failing).send(any());
        NotificationDeliveryService svc = new NotificationDeliveryService(
                failing, outboxRepository, pendingEmailStore, clock, MAX_ATTEMPTS);

        // Act / Assert: no lanza
        svc.sendOrQueue(MESSAGE);
        verify(pendingEmailStore).queue(any(), anyString(), any());
    }
}
