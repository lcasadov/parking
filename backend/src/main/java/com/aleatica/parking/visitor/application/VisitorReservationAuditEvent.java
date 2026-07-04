package com.aleatica.parking.visitor.application;

import com.aleatica.parking.visitor.dto.VisitorReservationResponse;

/**
 * Evento de dominio publicado por {@link VisitorReservationService} tras crear o anular
 * una reserva de visitante, consumido {@code AFTER_COMMIT} por
 * {@link VisitorEventListener}.
 *
 * <p>Desacopla la operacion de negocio (dentro de la transaccion) de la auditoria (fuera
 * de ella): un fallo de registro nunca revierte la operacion. Transporta un DTO, nunca la
 * entidad JPA.</p>
 *
 * @param kind        tipo de evento auditable
 * @param reservation instantanea de la reserva afectada
 */
public record VisitorReservationAuditEvent(Kind kind, VisitorReservationResponse reservation) {

    /** Tipos de evento auditable de una reserva de visitante. */
    public enum Kind {
        /** Reserva de visitante creada. */
        CREATED,
        /** Reserva de visitante futura anulada. */
        CANCELLED
    }
}
