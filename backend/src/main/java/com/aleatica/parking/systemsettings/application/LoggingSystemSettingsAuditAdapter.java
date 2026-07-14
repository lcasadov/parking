package com.aleatica.parking.systemsettings.application;

import com.aleatica.parking.audit.AuditDetailsSerializer;
import com.aleatica.parking.audit.AuditEntry;
import com.aleatica.parking.audit.AuditRecorder;
import com.aleatica.parking.systemsettings.dto.SystemSettingsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adaptador de {@link SystemSettingsAuditPort} que consolida la auditoria del cambio de modo
 * global en {@code audit_log} (capability {@code audit-retention}).
 *
 * <p>Persiste el cambio como una entrada de auditoria enriquecida (actor, entidad y snapshot
 * JSON) via {@link AuditRecorder}, y mantiene la traza en el log de aplicacion sin datos
 * personales (OWASP A09). Se invoca {@code AFTER_COMMIT} desde
 * {@link SystemSettingsEventListener} y el registro corre en su propia transaccion: un fallo
 * de auditoria nunca revierte el cambio (best-effort).</p>
 */
@Component
public class LoggingSystemSettingsAuditAdapter implements SystemSettingsAuditPort {

    private static final Logger LOG =
            LoggerFactory.getLogger(LoggingSystemSettingsAuditAdapter.class);

    private static final String ENTITY_TYPE = "SystemSettings";
    private static final String ACTION_MODE_CHANGED = "CHANGE_APPROVAL_MODE";

    private final AuditRecorder auditRecorder;
    private final AuditDetailsSerializer detailsSerializer;

    /**
     * @param auditRecorder     puerto de registro en {@code audit_log}
     * @param detailsSerializer serializador del snapshot a JSON para {@code details}
     */
    public LoggingSystemSettingsAuditAdapter(
            AuditRecorder auditRecorder, AuditDetailsSerializer detailsSerializer) {
        this.auditRecorder = auditRecorder;
        this.detailsSerializer = detailsSerializer;
    }

    @Override
    public void auditApprovalModeChanged(SystemSettingsResponse settings) {
        LOG.info("Modo de aprobacion global cambiado a {} por admin={}",
                settings.approvalMode(), settings.updatedById());
        try {
            AuditEntry entry = new AuditEntry(
                    settings.updatedById(), ACTION_MODE_CHANGED, ENTITY_TYPE, null,
                    detailsSerializer.serialize(settings));
            auditRecorder.record(entry);
        } catch (RuntimeException ex) {
            LOG.error("Fallo al persistir la auditoria del cambio de modo global; "
                    + "la operacion de negocio se mantiene aplicada", ex);
        }
    }
}
