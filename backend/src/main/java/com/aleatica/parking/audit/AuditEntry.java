package com.aleatica.parking.audit;

/**
 * Comando de registro de una accion funcional en {@code audit_log}.
 *
 * <p>Agrupa los atributos de una entrada de auditoria (S107: evita listas largas de
 * parametros). El {@code actorEmployeeId} es {@code null} para acciones originadas por
 * el propio sistema (p. ej. la purga programada); {@code entityId} es {@code null} cuando
 * la accion no se refiere a una entidad concreta. {@code details} es un JSON opcional con
 * los atributos enriquecidos (login del actor, IP, user-agent, snapshot antes/despues).</p>
 *
 * @param actorEmployeeId id del empleado que ejecuta la accion; {@code null} si es del sistema
 * @param action          accion funcional realizada (p. ej. {@code "APPROVE_REQUEST"})
 * @param entityType      tipo de entidad afectada (p. ej. {@code "Request"})
 * @param entityId        id de la entidad afectada; {@code null} si no aplica
 * @param details         detalle JSON enriquecido; {@code null} si no aplica
 */
public record AuditEntry(
        Long actorEmployeeId,
        String action,
        String entityType,
        Long entityId,
        String details) {

    /**
     * Crea una entrada minima (sin actor, entidad ni detalle) para una accion del sistema.
     *
     * @param action     accion funcional realizada
     * @param entityType tipo de entidad afectada
     * @return la entrada con {@code actorEmployeeId}, {@code entityId} y {@code details} nulos
     */
    public static AuditEntry system(String action, String entityType) {
        return new AuditEntry(null, action, entityType, null, null);
    }
}
