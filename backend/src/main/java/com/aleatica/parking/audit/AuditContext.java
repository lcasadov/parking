package com.aleatica.parking.audit;

/**
 * Contexto del actor de una accion auditable, resuelto en el momento del registro.
 *
 * <p>Transporta los atributos enriquecidos que no forman parte de la firma del caso de
 * uso: el actor (id y login), la IP de origen y el user-agent. Todos sus campos pueden ser
 * {@code null} cuando la accion la origina el propio sistema (sin peticion HTTP ni
 * autenticacion), en cuyo caso el actor de la entrada queda vacio.</p>
 *
 * @param actorEmployeeId id del empleado autenticado; {@code null} en acciones del sistema
 * @param actorLogin      login del empleado autenticado; {@code null} en acciones del sistema
 * @param ip              direccion IP de origen; {@code null} si no hay peticion HTTP
 * @param userAgent       user-agent del cliente; {@code null} si no hay peticion HTTP
 */
public record AuditContext(Long actorEmployeeId, String actorLogin, String ip, String userAgent) {

    private static final AuditContext SYSTEM = new AuditContext(null, null, null, null);

    /**
     * @return el contexto de una accion del sistema (todos los campos nulos)
     */
    public static AuditContext system() {
        return SYSTEM;
    }
}
