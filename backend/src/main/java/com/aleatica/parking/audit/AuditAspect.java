package com.aleatica.parking.audit;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
 * {@code audit_log} a traves del {@link AuditRecorder}, enriquecida con el actor
 * autenticado (id, login), la IP y el user-agent ({@link AuditContextResolver}) y un
 * snapshot del resultado serializado como JSON en {@code details}
 * ({@link AuditDetailsSerializer}). Centralizar la auditoria en un aspecto garantiza
 * cobertura uniforme sin ensuciar el dominio (OWASP A09 / trazabilidad).</p>
 */
@Aspect
@Component
public class AuditAspect {

    private static final Logger LOG = LoggerFactory.getLogger(AuditAspect.class);

    private static final String ID_ACCESSOR = "getId";

    private final AuditRecorder auditRecorder;
    private final AuditContextResolver contextResolver;
    private final AuditDetailsSerializer detailsSerializer;

    /**
     * @param auditRecorder     puerto de registro de auditoria
     * @param contextResolver   resolutor del contexto del actor (login, IP, user-agent)
     * @param detailsSerializer serializador de los atributos enriquecidos a JSON
     */
    public AuditAspect(
            AuditRecorder auditRecorder,
            AuditContextResolver contextResolver,
            AuditDetailsSerializer detailsSerializer) {
        this.auditRecorder = auditRecorder;
        this.contextResolver = contextResolver;
        this.detailsSerializer = detailsSerializer;
    }

    /**
     * Registra la accion auditable tras la ejecucion satisfactoria del metodo.
     *
     * <p>La auditoria es <em>fail-open</em>: si el registro falla despues de que la
     * operacion de negocio ya se aplico, el error se registra en el log y se devuelve el
     * resultado de negocio. Asi una incidencia de auditoria no provoca un 500 ni induce
     * reintentos sobre una operacion ya consolidada.</p>
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
            AuditContext context = contextResolver.resolve();
            String details = detailsSerializer.serialize(context, result);
            AuditEntry entry = new AuditEntry(
                    context.actorEmployeeId(), auditable.action(), auditable.entityType(),
                    entityIdOf(result), details);
            auditRecorder.record(entry);
        } catch (RuntimeException ex) {
            // Fail-open: la operacion de negocio ya se aplico; un fallo de auditoria
            // (p. ej. DataAccessException) no debe propagarse al cliente.
            LOG.error("Fallo al registrar la auditoria de la accion '{}' sobre '{}'; "
                    + "la operacion de negocio se mantiene aplicada",
                    auditable.action(), auditable.entityType(), ex);
        }
        return result;
    }

    /**
     * Deriva el id de la entidad afectada del valor devuelto por el caso de uso, si expone un
     * accesor {@code getId()} que retorna un {@link Long}. Devuelve {@code null} cuando no se
     * puede resolver (resultado nulo, sin {@code getId}, o tipo no compatible).
     */
    private static Long entityIdOf(Object result) {
        if (result == null) {
            return null;
        }
        try {
            Method accessor = result.getClass().getMethod(ID_ACCESSOR);
            if (accessor.getReturnType() == Long.class
                    && accessor.invoke(result) instanceof Long id) {
                return id;
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            LOG.debug("El resultado de la accion auditable no expone un id resoluble: {}",
                    ex.getMessage());
        }
        return null;
    }
}
