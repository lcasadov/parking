package com.aleatica.parking.notification.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job programado que reintenta los emails pendientes del almacen de reintento.
 *
 * <p>Se ejecuta con periodo fijo (configurable via {@code parking.notifications.retry-interval-ms});
 * delega en {@link NotificationDeliveryService#retryPending()}, que es idempotente (solo
 * reprocesa las entradas {@code PENDING}). En los tests el metodo se invoca directamente
 * para no depender de temporizadores (S2925: sin {@code Thread.sleep}).</p>
 */
@Component
public class EmailRetryJob {

    private final NotificationDeliveryService deliveryService;

    /**
     * @param deliveryService servicio de entrega/reintento resiliente
     */
    public EmailRetryJob(NotificationDeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    /**
     * Dispara el reintento de los emails pendientes. El periodo se controla por
     * configuracion; el primer disparo ocurre tras el retardo inicial.
     */
    @Scheduled(fixedDelayString = "${parking.notifications.retry-interval-ms:60000}")
    public void retryPendingEmails() {
        deliveryService.retryPending();
    }
}
