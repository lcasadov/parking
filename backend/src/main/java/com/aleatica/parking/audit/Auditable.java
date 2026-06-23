package com.aleatica.parking.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un metodo de caso de uso como auditable.
 *
 * <p>El {@link AuditAspect} intercepta los metodos anotados y registra una
 * entrada en {@code audit_log} tras su ejecucion. En este change de bootstrap la
 * anotacion existe pero ningun caso de uso la utiliza todavia.</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /**
     * @return accion funcional registrada (p. ej. {@code "APPROVE_REQUEST"})
     */
    String action();

    /**
     * @return tipo de entidad afectada (p. ej. {@code "Request"})
     */
    String entityType();
}
