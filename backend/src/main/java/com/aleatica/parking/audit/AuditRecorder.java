package com.aleatica.parking.audit;

/**
 * Puerto de salida para registrar acciones funcionales en {@code audit_log}.
 *
 * <p>Abstrae la persistencia para que el {@link AuditAspect} y los adaptadores de
 * auditoria de cada modulo (liberaciones, visitantes) no dependan de un adaptador
 * concreto. La implementacion persiste la entrada enriquecida (actor, entidad y
 * {@code details} JSON) en su propia transaccion, de modo que un fallo de auditoria
 * nunca revierta la operacion de negocio (registro best-effort).</p>
 */
public interface AuditRecorder {

    /**
     * Registra una accion auditable en {@code audit_log}.
     *
     * @param entry datos de la entrada de auditoria (actor, accion, entidad, detalle)
     */
    void record(AuditEntry entry);
}
