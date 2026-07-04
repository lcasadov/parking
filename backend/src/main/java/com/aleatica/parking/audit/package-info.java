/**
 * Auditoria funcional transversal y retencion de datos historicos (capability
 * {@code audit-retention}).
 *
 * <p>Define la anotacion {@link com.aleatica.parking.audit.Auditable}, el aspecto
 * {@link com.aleatica.parking.audit.AuditAspect} y el puerto
 * {@link com.aleatica.parking.audit.AuditRecorder} con su adaptador JPA
 * ({@link com.aleatica.parking.audit.JpaAuditRecorder}), que escriben en {@code audit_log} de
 * forma best-effort. Aporta ademas las entidades de lectura
 * {@link com.aleatica.parking.audit.AuditLog} y {@link com.aleatica.parking.audit.LoginLog}
 * con sus repositorios, la consulta paginada solo {@code ADMIN}
 * ({@link com.aleatica.parking.audit.AuditController}) y la purga por lotes a 2 anos
 * ({@link com.aleatica.parking.audit.RetentionPurgeJob}).</p>
 */
package com.aleatica.parking.audit;
