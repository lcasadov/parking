package com.aleatica.parking.notification.application;

import com.aleatica.parking.auth.domain.ClockPort;
import com.aleatica.parking.notification.EmailOutbox;
import com.aleatica.parking.notification.EmailOutboxRepository;
import com.aleatica.parking.notification.EmailOutboxStatus;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Entrega resiliente de notificaciones: renderiza la orden con la plantilla vigente, intenta
 * el envio inmediato y, si el SMTP falla, registra el fallo (sin datos personales) y encola
 * la <strong>orden del evento</strong> (no el HTML) para reintento, sin propagar nunca la
 * excepcion al flujo de negocio ya confirmado (spec §"Resiliencia ante fallo SMTP").
 *
 * <p>El job programado invoca {@link #retryPending()} para reprocesar las entradas
 * {@code PENDING}: reconstruye la orden desde el outbox, la <strong>re-renderiza con la
 * plantilla actual</strong> y la reenvia; marca {@code SENT} las que tienen exito y conserva
 * {@code PENDING} (o pasa a {@code FAILED} al agotar la politica) las que vuelven a fallar.
 * Solo relee {@code PENDING}, por lo que un email ya enviado nunca se reenvia (idempotencia).
 * Como el reintento re-renderiza, un cambio de plantilla nunca provoca el reenvio de
 * contenido obsoleto.</p>
 */
@Service
public class NotificationDeliveryService {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationDeliveryService.class);

    /** Aviso de fallo SMTP: no incluye destinatario ni asunto (minimizacion RGPD, OWASP A09). */
    private static final String MSG_SEND_FAILED =
            "Envio SMTP fallido; notificacion encolada para reintento (outbox)";
    private static final String MSG_RETRY_FAILED =
            "Reintento SMTP fallido; email id={} sigue pendiente (intentos={})";
    private static final String MSG_RETRY_NO_RECIPIENT =
            "Reintento descartado; destinatario no resoluble para email id={} (marcado FAILED)";
    private static final String ERR_RECIPIENT_UNRESOLVABLE =
            "Destinatario no resoluble al reintentar";

    private final EmailSenderPort emailSenderPort;
    private final EmailOutboxRepository outboxRepository;
    private final PendingEmailStore pendingEmailStore;
    private final NotificationRenderer notificationRenderer;
    private final NotificationOutboxMapper outboxMapper;
    private final ClockPort clock;
    private final int maxAttempts;

    /**
     * @param emailSenderPort      adaptador de envio SMTP
     * @param outboxRepository     almacen de reintento (lectura/actualizacion en el reintento)
     * @param pendingEmailStore    encolado transaccional-nuevo de la notificacion fallida
     * @param notificationRenderer renderizador compartido (envio inmediato y reintento)
     * @param outboxMapper         conversor orden &harr; fila del outbox
     * @param clock                reloj inyectable para marcas de tiempo
     * @param maxAttempts          maximo de intentos antes de descartar (politica de reintentos)
     */
    public NotificationDeliveryService(
            EmailSenderPort emailSenderPort,
            EmailOutboxRepository outboxRepository,
            PendingEmailStore pendingEmailStore,
            NotificationRenderer notificationRenderer,
            NotificationOutboxMapper outboxMapper,
            ClockPort clock,
            @Value("${parking.notifications.max-attempts:5}") int maxAttempts) {
        this.emailSenderPort = emailSenderPort;
        this.outboxRepository = outboxRepository;
        this.pendingEmailStore = pendingEmailStore;
        this.notificationRenderer = notificationRenderer;
        this.outboxMapper = outboxMapper;
        this.clock = clock;
        this.maxAttempts = maxAttempts;
    }

    /**
     * Renderiza la orden y la envia de inmediato; si el destinatario no existe no hace nada
     * (no encola), y si el SMTP falla encola la orden como {@code PENDING} para reintento.
     * Nunca relanza la excepcion de envio: la operacion funcional que origino la notificacion
     * ya esta confirmada.
     *
     * @param command orden de notificacion a entregar
     */
    public void dispatch(NotificationCommand command) {
        notificationRenderer.render(command).ifPresent(message -> sendOrQueue(command, message));
    }

    private void sendOrQueue(NotificationCommand command, EmailMessage message) {
        try {
            emailSenderPort.send(message);
        } catch (EmailDeliveryException ex) {
            LOG.warn(MSG_SEND_FAILED, ex);
            pendingEmailStore.queue(outboxMapper.toPendingOutbox(command, ex.getMessage(), clock.now()));
        }
    }

    /**
     * Reprocesa todas las entradas {@code PENDING} del almacen: reconstruye y re-renderiza
     * cada orden, marcando {@code SENT} las que tienen exito y registrando el fallo
     * (conservando {@code PENDING} o pasando a {@code FAILED} al agotar la politica) las que
     * vuelven a fallar. Es idempotente: solo lee {@code PENDING}, nunca reenvia un
     * {@code SENT}.
     */
    @Transactional
    public void retryPending() {
        List<EmailOutbox> pending = outboxRepository.findByStatus(EmailOutboxStatus.PENDING);
        for (EmailOutbox entry : pending) {
            retryOne(entry);
        }
    }

    private void retryOne(EmailOutbox entry) {
        Optional<EmailMessage> message = notificationRenderer.render(outboxMapper.toCommand(entry));
        if (message.isEmpty()) {
            entry.markFailed(clock.now(), ERR_RECIPIENT_UNRESOLVABLE);
            LOG.warn(MSG_RETRY_NO_RECIPIENT, entry.getId());
            return;
        }
        try {
            emailSenderPort.send(message.get());
            entry.markSent(clock.now());
        } catch (EmailDeliveryException ex) {
            entry.registerFailedRetry(clock.now(), ex.getMessage(), maxAttempts);
            LOG.warn(MSG_RETRY_FAILED, entry.getId(), entry.getAttempts());
        }
    }
}
