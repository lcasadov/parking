package com.aleatica.parking.release.application;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Escucha los {@link ReleaseAuditEvent} y delega en {@link ReleaseAuditPort}
 * <strong>despues del commit</strong> de la transaccion que aplico la operacion.
 *
 * <p>{@code AFTER_COMMIT} garantiza que solo se audita lo efectivamente persistido y que
 * un fallo del canal de auditoria no revierte la operacion de negocio (consolidacion
 * B10 de {@code audit-retention}).</p>
 */
@Component
public class ReleaseEventListener {

    private final ReleaseAuditPort auditPort;

    /**
     * @param auditPort puerto de auditoria (adaptador de log en Fase 1)
     */
    public ReleaseEventListener(ReleaseAuditPort auditPort) {
        this.auditPort = auditPort;
    }

    /**
     * Enruta el evento al metodo del puerto segun su tipo, tras el commit.
     *
     * @param event evento de auditoria de la liberacion
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReleaseEvent(ReleaseAuditEvent event) {
        switch (event.kind()) {
            case VOLUNTARY_RELEASED -> auditPort.auditVoluntaryRelease(event.release());
            case ADMINISTRATIVE_RELEASED -> auditPort.auditAdministrativeRelease(event.release());
            case CANCELLED -> auditPort.auditCancellation(event.release());
        }
    }
}
