package com.aleatica.parking.visitor.application;

import com.aleatica.parking.visitor.dto.VisitorReservationResponse;
import com.aleatica.parking.visitor.dto.VisitorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adaptador de {@link VisitorAuditPort} de Fase 1: deja constancia de la accion en el log
 * sin exponer datos personales.
 *
 * <p>En la consolidacion de {@code audit-retention} (B10) un adaptador respaldado por
 * {@code audit_log} sustituira a este (fuera del alcance de este change). Registra solo
 * identificadores, nunca datos personales del visitante (OWASP A09 / logging seguro).</p>
 */
@Component
public class LoggingVisitorAuditAdapter implements VisitorAuditPort {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingVisitorAuditAdapter.class);

    @Override
    public void auditVisitorCreated(VisitorResponse visitor) {
        LOG.info("Ficha de visitante creada id={} por admin={}", visitor.id(), visitor.createdById());
    }

    @Override
    public void auditVisitorUpdated(VisitorResponse visitor) {
        LOG.info("Ficha de visitante editada id={} por admin={}", visitor.id(), visitor.createdById());
    }

    @Override
    public void auditReservationCreated(VisitorReservationResponse reservation) {
        LOG.info("Reserva de visitante creada id={} visitante={} recurso={} fecha={} por admin={}",
                reservation.id(), reservation.visitorId(), reservation.parkingSpaceId(),
                reservation.reservationDate(), reservation.createdById());
    }

    @Override
    public void auditReservationCancelled(VisitorReservationResponse reservation) {
        LOG.info("Reserva de visitante anulada id={} visitante={} recurso={} fecha={} por admin={}",
                reservation.id(), reservation.visitorId(), reservation.parkingSpaceId(),
                reservation.reservationDate(), reservation.createdById());
    }
}
