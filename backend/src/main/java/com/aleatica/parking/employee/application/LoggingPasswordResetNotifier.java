package com.aleatica.parking.employee.application;

import com.aleatica.parking.employee.Employee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adaptador de {@link PasswordResetNotifier} de Fase 1: deja constancia del envio
 * en el log sin exponer la contrasena.
 *
 * <p>En Fase 2 la capability {@code notifications} aportara un adaptador de email
 * que sustituira a este (fuera del alcance de este change). Nunca registra la
 * contrasena en claro (OWASP A09 / logging seguro).</p>
 */
@Component
public class LoggingPasswordResetNotifier implements PasswordResetNotifier {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingPasswordResetNotifier.class);

    @Override
    public void notifyReset(Employee employee, String temporaryPassword) {
        LOG.info("Reset de contrasena notificado al empleado id={}", employee.getId());
    }
}
