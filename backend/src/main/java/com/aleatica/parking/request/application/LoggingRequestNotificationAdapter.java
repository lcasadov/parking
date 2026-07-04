package com.aleatica.parking.request.application;

import com.aleatica.parking.request.dto.RequestResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adaptador de {@link RequestNotificationPort} de Fase 1: deja constancia del envio en
 * el log sin exponer datos personales.
 *
 * <p>En Fase 2 la capability {@code notifications} (B9) aportara un adaptador de email
 * que sustituira a este (fuera del alcance de este change). Registra solo
 * identificadores, nunca datos personales (OWASP A09 / logging seguro).</p>
 */
@Component
public class LoggingRequestNotificationAdapter implements RequestNotificationPort {

    private static final Logger LOG =
            LoggerFactory.getLogger(LoggingRequestNotificationAdapter.class);

    @Override
    public void notifyCreated(RequestResponse request) {
        LOG.info("Nueva solicitud id={} notificada a administradores", request.id());
    }

    @Override
    public void notifyApproved(RequestResponse request) {
        LOG.info("Solicitud id={} aprobada notificada al empleado id={}",
                request.id(), request.employeeId());
    }

    @Override
    public void notifyRejected(RequestResponse request) {
        LOG.info("Solicitud id={} rechazada notificada al empleado id={}",
                request.id(), request.employeeId());
    }
}
