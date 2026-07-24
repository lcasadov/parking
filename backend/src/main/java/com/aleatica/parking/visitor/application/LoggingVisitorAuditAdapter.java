package com.aleatica.parking.visitor.application;

import com.aleatica.parking.audit.AuditDetailsSerializer;
import com.aleatica.parking.audit.AuditEntry;
import com.aleatica.parking.audit.AuditRecorder;
import com.aleatica.parking.visitor.dto.VisitorReservationResponse;
import com.aleatica.parking.visitor.dto.VisitorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adaptador de {@link VisitorAuditPort} que consolida la auditoria de visitantes y sus
 * reservas en {@code audit_log} (capability {@code audit-retention}).
 *
 * <p>Persiste cada accion del {@code ADMIN} como una entrada de auditoria enriquecida
 * (actor, entidad y snapshot JSON) via {@link AuditRecorder}, y mantiene ademas la traza en
 * el log de aplicacion con solo identificadores, nunca datos personales del visitante (OWASP
 * A09 / logging seguro). Se invoca {@code AFTER_COMMIT} desde {@link VisitorEventListener} y
 * el registro corre en su propia transaccion: un fallo de auditoria nunca revierte la
 * operacion de negocio (best-effort).</p>
 */
@Component
public class LoggingVisitorAuditAdapter implements VisitorAuditPort {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingVisitorAuditAdapter.class);

    private static final String ENTITY_VISITOR = "Visitor";
    private static final String ENTITY_RESERVATION = "VisitorReservation";
    private static final String ACTION_VISITOR_CREATED = "CREATE_VISITOR";
    private static final String ACTION_VISITOR_UPDATED = "UPDATE_VISITOR";
    private static final String ACTION_RESERVATION_CREATED = "CREATE_VISITOR_RESERVATION";
    private static final String ACTION_RESERVATION_CANCELLED = "CANCEL_VISITOR_RESERVATION";

    private final AuditRecorder auditRecorder;
    private final AuditDetailsSerializer detailsSerializer;

    /**
     * @param auditRecorder     puerto de registro en {@code audit_log}
     * @param detailsSerializer serializador del snapshot a JSON para {@code details}
     */
    public LoggingVisitorAuditAdapter(
            AuditRecorder auditRecorder, AuditDetailsSerializer detailsSerializer) {
        this.auditRecorder = auditRecorder;
        this.detailsSerializer = detailsSerializer;
    }

    @Override
    public void auditVisitorCreated(VisitorResponse visitor) {
        LOG.info("Ficha de visitante creada id={} por admin={}", visitor.id(), visitor.createdById());
        persist(ACTION_VISITOR_CREATED, ENTITY_VISITOR, visitor.id(), visitor.createdById(), visitor);
    }

    @Override
    public void auditVisitorUpdated(VisitorResponse visitor) {
        LOG.info("Ficha de visitante editada id={} por admin={}", visitor.id(), visitor.createdById());
        persist(ACTION_VISITOR_UPDATED, ENTITY_VISITOR, visitor.id(), visitor.createdById(), visitor);
    }

    @Override
    public void auditReservationCreated(VisitorReservationResponse reservation) {
        LOG.info("Reserva de visitante creada id={} visitante={} tipo={} recurso={} fecha={} por admin={}",
                reservation.id(), reservation.visitorId(), reservation.resourceType(),
                reservation.resourceId(), reservation.reservationDate(), reservation.createdById());
        persist(ACTION_RESERVATION_CREATED, ENTITY_RESERVATION, reservation.id(),
                reservation.createdById(), reservation);
    }

    @Override
    public void auditReservationCancelled(VisitorReservationResponse reservation) {
        LOG.info("Reserva de visitante anulada id={} visitante={} tipo={} recurso={} fecha={} por admin={}",
                reservation.id(), reservation.visitorId(), reservation.resourceType(),
                reservation.resourceId(), reservation.reservationDate(), reservation.createdById());
        persist(ACTION_RESERVATION_CANCELLED, ENTITY_RESERVATION, reservation.id(),
                reservation.createdById(), reservation);
    }

    private void persist(String action, String entityType, Long entityId, Long actorId, Object snapshot) {
        try {
            AuditEntry entry = new AuditEntry(
                    actorId, action, entityType, entityId, detailsSerializer.serialize(snapshot));
            auditRecorder.record(entry);
        } catch (RuntimeException ex) {
            LOG.error("Fallo al persistir la auditoria de la accion '{}' sobre '{}' id={}; "
                    + "la operacion de negocio se mantiene aplicada",
                    action, entityType, entityId, ex);
        }
    }
}
