package com.aleatica.parking.request.application;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Escucha los {@link RequestNotificationEvent} y delega en {@link RequestNotificationPort}
 * <strong>despues del commit</strong> de la transaccion que resolvio la solicitud.
 *
 * <p>{@code AFTER_COMMIT} garantiza que solo se notifica lo efectivamente persistido y
 * que un fallo del canal de notificacion no revierte la resolucion (design §Risks).</p>
 */
@Component
public class RequestEventListener {

    private final RequestNotificationPort notificationPort;

    /**
     * @param notificationPort puerto de notificacion (adaptador de log en Fase 1)
     */
    public RequestEventListener(RequestNotificationPort notificationPort) {
        this.notificationPort = notificationPort;
    }

    /**
     * Enruta el evento al metodo del puerto segun su tipo, tras el commit.
     *
     * @param event evento de notificacion de la solicitud
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRequestEvent(RequestNotificationEvent event) {
        switch (event.kind()) {
            case CREATED -> notificationPort.notifyCreated(event.request());
            case APPROVED -> notificationPort.notifyApproved(event.request());
            case REJECTED -> notificationPort.notifyRejected(event.request());
        }
    }
}
