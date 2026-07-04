package com.aleatica.parking.visitor.application;

import com.aleatica.parking.visitor.dto.VisitorResponse;

/**
 * Evento de dominio publicado por {@link VisitorService} tras crear o editar una ficha
 * de visitante, consumido {@code AFTER_COMMIT} por {@link VisitorEventListener}.
 *
 * <p>Desacopla la operacion de negocio (dentro de la transaccion) de la auditoria (fuera
 * de ella): un fallo de registro nunca revierte la operacion. Transporta un DTO, nunca la
 * entidad JPA.</p>
 *
 * @param kind    tipo de evento auditable
 * @param visitor instantanea de la ficha afectada
 */
public record VisitorAuditEvent(Kind kind, VisitorResponse visitor) {

    /** Tipos de evento auditable de una ficha de visitante. */
    public enum Kind {
        /** Ficha de visitante creada. */
        CREATED,
        /** Ficha de visitante editada. */
        UPDATED
    }
}
