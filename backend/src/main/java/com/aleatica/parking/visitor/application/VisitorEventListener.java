package com.aleatica.parking.visitor.application;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Escucha los eventos de auditoria de visitantes y reservas y delega en
 * {@link VisitorAuditPort} <strong>despues del commit</strong> de la transaccion que
 * aplico la operacion.
 *
 * <p>{@code AFTER_COMMIT} garantiza que solo se audita lo efectivamente persistido y que
 * un fallo del canal de auditoria no revierte la operacion de negocio (consolidacion B10
 * de {@code audit-retention}).</p>
 */
@Component
public class VisitorEventListener {

    private final VisitorAuditPort auditPort;

    /**
     * @param auditPort puerto de auditoria (adaptador de log en Fase 1)
     */
    public VisitorEventListener(VisitorAuditPort auditPort) {
        this.auditPort = auditPort;
    }

    /**
     * Enruta un evento de ficha de visitante al metodo del puerto segun su tipo, tras el
     * commit.
     *
     * @param event evento de auditoria de la ficha
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVisitorEvent(VisitorAuditEvent event) {
        switch (event.kind()) {
            case CREATED -> auditPort.auditVisitorCreated(event.visitor());
            case UPDATED -> auditPort.auditVisitorUpdated(event.visitor());
        }
    }

    /**
     * Enruta un evento de reserva de visitante al metodo del puerto segun su tipo, tras el
     * commit.
     *
     * @param event evento de auditoria de la reserva
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReservationEvent(VisitorReservationAuditEvent event) {
        switch (event.kind()) {
            case CREATED -> auditPort.auditReservationCreated(event.reservation());
            case CANCELLED -> auditPort.auditReservationCancelled(event.reservation());
        }
    }
}
