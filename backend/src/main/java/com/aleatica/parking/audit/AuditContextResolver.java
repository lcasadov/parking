package com.aleatica.parking.audit;

/**
 * Puerto de salida que resuelve el {@link AuditContext} de la accion en curso.
 *
 * <p>Aisla al {@link AuditAspect} de la infraestructura web/seguridad: el aspecto pide el
 * contexto sin conocer de donde salen el actor autenticado, la IP o el user-agent. En una
 * accion del sistema (sin peticion ni autenticacion) devuelve {@link AuditContext#system()}.</p>
 */
public interface AuditContextResolver {

    /**
     * Resuelve el contexto del actor de la accion en curso.
     *
     * @return el contexto actual; {@link AuditContext#system()} si no hay actor identificable
     */
    AuditContext resolve();
}
