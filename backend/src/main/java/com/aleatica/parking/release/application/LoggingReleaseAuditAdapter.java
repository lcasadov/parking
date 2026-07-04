package com.aleatica.parking.release.application;

import com.aleatica.parking.audit.AuditDetailsSerializer;
import com.aleatica.parking.audit.AuditEntry;
import com.aleatica.parking.audit.AuditRecorder;
import com.aleatica.parking.release.dto.ReleaseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adaptador de {@link ReleaseAuditPort} que consolida la auditoria de liberaciones en
 * {@code audit_log} (capability {@code audit-retention}).
 *
 * <p>Persiste cada evento de liberacion como una entrada de auditoria enriquecida (actor,
 * entidad y snapshot JSON) via {@link AuditRecorder}, y mantiene ademas la traza en el log
 * de aplicacion sin datos personales (OWASP A09 / logging seguro). Se invoca
 * {@code AFTER_COMMIT} desde {@link ReleaseEventListener} y el registro corre en su propia
 * transaccion: un fallo de auditoria nunca revierte la liberacion (best-effort).</p>
 */
@Component
public class LoggingReleaseAuditAdapter implements ReleaseAuditPort {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingReleaseAuditAdapter.class);

    private static final String ENTITY_TYPE = "Release";
    private static final String ACTION_VOLUNTARY = "VOLUNTARY_RELEASE";
    private static final String ACTION_ADMINISTRATIVE = "ADMINISTRATIVE_RELEASE";
    private static final String ACTION_CANCELLED = "CANCEL_RELEASE";

    private final AuditRecorder auditRecorder;
    private final AuditDetailsSerializer detailsSerializer;

    /**
     * @param auditRecorder     puerto de registro en {@code audit_log}
     * @param detailsSerializer serializador del snapshot a JSON para {@code details}
     */
    public LoggingReleaseAuditAdapter(
            AuditRecorder auditRecorder, AuditDetailsSerializer detailsSerializer) {
        this.auditRecorder = auditRecorder;
        this.detailsSerializer = detailsSerializer;
    }

    @Override
    public void auditVoluntaryRelease(ReleaseResponse release) {
        LOG.info("Liberacion voluntaria id={} recurso={} fecha={} por empleado={}",
                release.id(), release.parkingSpaceId(), release.releaseDate(), release.releasedById());
        persist(ACTION_VOLUNTARY, release, release.releasedById());
    }

    @Override
    public void auditAdministrativeRelease(ReleaseResponse release) {
        LOG.info("Liberacion administrativa id={} recurso={} fecha={} titular={} por admin={}",
                release.id(), release.parkingSpaceId(), release.releaseDate(),
                release.employeeId(), release.releasedById());
        persist(ACTION_ADMINISTRATIVE, release, release.releasedById());
    }

    @Override
    public void auditCancellation(ReleaseResponse release) {
        LOG.info("Cancelacion de liberacion id={} recurso={} fecha={} por empleado={}",
                release.id(), release.parkingSpaceId(), release.releaseDate(), release.employeeId());
        persist(ACTION_CANCELLED, release, release.employeeId());
    }

    private void persist(String action, ReleaseResponse release, Long actorId) {
        try {
            AuditEntry entry = new AuditEntry(
                    actorId, action, ENTITY_TYPE, release.id(),
                    detailsSerializer.serialize(release));
            auditRecorder.record(entry);
        } catch (RuntimeException ex) {
            LOG.error("Fallo al persistir la auditoria de la accion '{}' sobre '{}' id={}; "
                    + "la operacion de negocio se mantiene aplicada",
                    action, ENTITY_TYPE, release.id(), ex);
        }
    }
}
