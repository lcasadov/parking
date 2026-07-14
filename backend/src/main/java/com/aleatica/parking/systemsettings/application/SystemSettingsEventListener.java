package com.aleatica.parking.systemsettings.application;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Escucha los {@link SystemSettingsAuditEvent} y delega en {@link SystemSettingsAuditPort}
 * <strong>despues del commit</strong> de la transaccion que aplico el cambio.
 *
 * <p>{@code AFTER_COMMIT} garantiza que solo se audita lo efectivamente persistido y que un
 * fallo del canal de auditoria no revierte la operacion de negocio.</p>
 */
@Component
public class SystemSettingsEventListener {

    private final SystemSettingsAuditPort auditPort;

    /**
     * @param auditPort puerto de auditoria (adaptador de registro en {@code audit_log})
     */
    public SystemSettingsEventListener(SystemSettingsAuditPort auditPort) {
        this.auditPort = auditPort;
    }

    /**
     * Enruta el evento al puerto de auditoria tras el commit.
     *
     * @param event evento de cambio de modo global
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSystemSettingsEvent(SystemSettingsAuditEvent event) {
        auditPort.auditApprovalModeChanged(event.settings());
    }
}
