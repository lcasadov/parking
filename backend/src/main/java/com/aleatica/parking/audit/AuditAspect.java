package com.aleatica.parking.audit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Aspecto que audita los metodos anotados con {@link Auditable}.
 *
 * <p>Ejecuta el metodo de negocio y, si termina con exito, registra la accion en
 * {@code audit_log} a traves del {@link AuditRecorder}. Centralizar la auditoria
 * en un aspecto garantiza cobertura uniforme sin ensuciar el dominio (OWASP /
 * trazabilidad). En este change de bootstrap ningun caso de uso esta anotado.</p>
 */
@Aspect
@Component
public class AuditAspect {

    private static final Logger LOG = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditRecorder auditRecorder;

    /**
     * @param auditRecorder puerto de registro de auditoria
     */
    public AuditAspect(AuditRecorder auditRecorder) {
        this.auditRecorder = auditRecorder;
    }

    /**
     * Registra la accion auditable tras la ejecucion satisfactoria del metodo.
     *
     * <p>La auditoria es <em>fail-open</em>: si {@link AuditRecorder#record} falla
     * despues de que la operacion de negocio ya se aplico, el error se registra en
     * el log y se devuelve el resultado de negocio. Asi una incidencia de auditoria
     * no provoca un 500 ni induce reintentos sobre una operacion ya consolidada.</p>
     *
     * @param joinPoint punto de union interceptado
     * @param auditable metadatos de la anotacion {@link Auditable}
     * @return el valor devuelto por el metodo de negocio
     * @throws Throwable si el metodo de negocio lanza una excepcion (no se audita en ese caso)
     */
    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        Object result = joinPoint.proceed();
        try {
            auditRecorder.record(auditable.action(), auditable.entityType(), null);
        } catch (RuntimeException ex) {
            // Fail-open: la operacion de negocio ya se aplico; un fallo de auditoria
            // (p. ej. DataAccessException) no debe propagarse al cliente.
            LOG.error("Fallo al registrar la auditoria de la accion '{}' sobre '{}'; "
                    + "la operacion de negocio se mantiene aplicada",
                    auditable.action(), auditable.entityType(), ex);
        }
        return result;
    }
}
