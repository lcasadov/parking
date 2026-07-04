package com.aleatica.parking.notification.application;

import com.aleatica.parking.auth.domain.ClockPort;
import com.aleatica.parking.notification.EmailOutbox;
import com.aleatica.parking.notification.EmailOutboxRepository;
import com.aleatica.parking.notification.EmailOutboxStatus;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Entrega resiliente de emails: intenta el envio inmediato y, si el SMTP falla, registra
 * el fallo (sin datos personales) y encola el email para reintento, sin propagar nunca la
 * excepcion al flujo de negocio ya confirmado (spec §"Resiliencia ante fallo SMTP").
 *
 * <p>El job programado invoca {@link #retryPending()} para reprocesar las entradas
 * {@code PENDING}: marca {@code SENT} las que tienen exito y conserva {@code PENDING} (o
 * pasa a {@code FAILED} al agotar la politica) las que vuelven a fallar. Solo relee
 * {@code PENDING}, por lo que un email ya enviado nunca se reenvia (idempotencia).</p>
 */
@Service
public class NotificationDeliveryService {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationDeliveryService.class);

    /** Aviso de fallo SMTP: no incluye destinatario ni asunto (minimizacion RGPD, OWASP A09). */
    private static final String MSG_SEND_FAILED =
            "Envio SMTP fallido; email encolado para reintento (outbox)";
    private static final String MSG_RETRY_FAILED =
            "Reintento SMTP fallido; email id={} sigue pendiente (intentos={})";

    private final EmailSenderPort emailSenderPort;
    private final EmailOutboxRepository outboxRepository;
    private final PendingEmailStore pendingEmailStore;
    private final ClockPort clock;
    private final int maxAttempts;

    /**
     * @param emailSenderPort   adaptador de envio SMTP
     * @param outboxRepository  almacen de reintento (lectura/actualizacion en el reintento)
     * @param pendingEmailStore encolado transaccional-nuevo del email fallido
     * @param clock             reloj inyectable para marcas de tiempo
     * @param maxAttempts       maximo de intentos antes de descartar (politica de reintentos)
     */
    public NotificationDeliveryService(
            EmailSenderPort emailSenderPort,
            EmailOutboxRepository outboxRepository,
            PendingEmailStore pendingEmailStore,
            ClockPort clock,
            @Value("${parking.notifications.max-attempts:5}") int maxAttempts) {
        this.emailSenderPort = emailSenderPort;
        this.outboxRepository = outboxRepository;
        this.pendingEmailStore = pendingEmailStore;
        this.clock = clock;
        this.maxAttempts = maxAttempts;
    }

    /**
     * Intenta enviar el mensaje de inmediato; si el SMTP falla, lo encola como
     * {@code PENDING} para reintento. Nunca relanza la excepcion de envio: la operacion
     * funcional que origino el email ya esta confirmada.
     *
     * @param message mensaje a enviar
     */
    public void sendOrQueue(EmailMessage message) {
        try {
            emailSenderPort.send(message);
        } catch (EmailDeliveryException ex) {
            LOG.warn(MSG_SEND_FAILED, ex);
            pendingEmailStore.queue(message, ex.getMessage(), clock.now());
        }
    }

    /**
     * Reprocesa todas las entradas {@code PENDING} del almacen: reenvia cada una, marcando
     * {@code SENT} las que tienen exito y registrando el fallo (conservando {@code PENDING}
     * o pasando a {@code FAILED} al agotar la politica) las que vuelven a fallar. Es
     * idempotente: solo lee {@code PENDING}, nunca reenvia un {@code SENT}.
     */
    @Transactional
    public void retryPending() {
        List<EmailOutbox> pending = outboxRepository.findByStatus(EmailOutboxStatus.PENDING);
        for (EmailOutbox entry : pending) {
            retryOne(entry);
        }
    }

    private void retryOne(EmailOutbox entry) {
        try {
            emailSenderPort.send(entry.toMessage());
            entry.markSent(clock.now());
        } catch (EmailDeliveryException ex) {
            entry.registerFailedRetry(clock.now(), ex.getMessage(), maxAttempts);
            LOG.warn(MSG_RETRY_FAILED, entry.getId(), entry.getAttempts());
        }
    }
}
