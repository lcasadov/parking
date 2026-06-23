package com.aleatica.parking.audit;

/**
 * Puerto de salida para registrar acciones funcionales en {@code audit_log}.
 *
 * <p>Abstrae la persistencia para que el {@link AuditAspect} no dependa de un
 * adaptador concreto. La implementacion enriquecida (actor, IP, user-agent,
 * payload antes/despues como JSON en {@code details}) la aporta el change
 * funcional de {@code audit-retention}.</p>
 */
public interface AuditRecorder {

    /**
     * Registra una accion auditable.
     *
     * @param action     accion funcional realizada
     * @param entityType tipo de entidad afectada
     * @param details    detalle adicional (puede ser {@code null})
     */
    void record(String action, String entityType, String details);
}
