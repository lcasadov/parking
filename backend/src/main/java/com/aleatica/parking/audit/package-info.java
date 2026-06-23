/**
 * Auditoria funcional transversal (base AOP).
 *
 * <p>Define la anotacion {@link com.aleatica.parking.audit.Auditable}, el aspecto
 * {@link com.aleatica.parking.audit.AuditAspect} y el puerto
 * {@link com.aleatica.parking.audit.AuditRecorder} con su adaptador JDBC, que
 * escriben en {@code audit_log}. La consulta y la retencion las aporta el change
 * funcional de {@code audit-retention}.</p>
 */
package com.aleatica.parking.audit;
