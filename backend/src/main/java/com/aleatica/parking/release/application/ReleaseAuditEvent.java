package com.aleatica.parking.release.application;

import com.aleatica.parking.release.dto.ReleaseResponse;

/**
 * Evento de dominio publicado por {@link ReleaseService} tras una operacion sobre una
 * liberacion, consumido {@code AFTER_COMMIT} por {@link ReleaseEventListener}.
 *
 * <p>Desacopla la operacion de negocio (dentro de la transaccion) de la auditoria
 * (fuera de ella): un fallo de registro nunca revierte la liberacion. Transporta un
 * DTO, nunca la entidad JPA.</p>
 *
 * @param kind    tipo de evento auditable
 * @param release instantanea de la liberacion afectada
 */
public record ReleaseAuditEvent(Kind kind, ReleaseResponse release) {

    /** Tipos de evento auditable de una liberacion. */
    public enum Kind {
        /** Liberacion voluntaria creada (accion del titular). */
        VOLUNTARY_RELEASED,
        /** Liberacion administrativa creada (accion del ADMIN). */
        ADMINISTRATIVE_RELEASED,
        /** Liberacion futura propia cancelada. */
        CANCELLED
    }
}
