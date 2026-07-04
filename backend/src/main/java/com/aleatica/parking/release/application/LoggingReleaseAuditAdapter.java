package com.aleatica.parking.release.application;

import com.aleatica.parking.release.dto.ReleaseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adaptador de {@link ReleaseAuditPort} de Fase 1: deja constancia de la accion en el
 * log sin exponer datos personales.
 *
 * <p>En la consolidacion de {@code audit-retention} (B10) un adaptador respaldado por
 * {@code audit_log} sustituira a este (fuera del alcance de este change). Registra solo
 * identificadores, nunca datos personales (OWASP A09 / logging seguro).</p>
 */
@Component
public class LoggingReleaseAuditAdapter implements ReleaseAuditPort {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingReleaseAuditAdapter.class);

    @Override
    public void auditVoluntaryRelease(ReleaseResponse release) {
        LOG.info("Liberacion voluntaria id={} recurso={} fecha={} por empleado={}",
                release.id(), release.parkingSpaceId(), release.releaseDate(), release.releasedById());
    }

    @Override
    public void auditAdministrativeRelease(ReleaseResponse release) {
        LOG.info("Liberacion administrativa id={} recurso={} fecha={} titular={} por admin={}",
                release.id(), release.parkingSpaceId(), release.releaseDate(),
                release.employeeId(), release.releasedById());
    }

    @Override
    public void auditCancellation(ReleaseResponse release) {
        LOG.info("Cancelacion de liberacion id={} recurso={} fecha={} por empleado={}",
                release.id(), release.parkingSpaceId(), release.releaseDate(), release.employeeId());
    }
}
