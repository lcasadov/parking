package com.aleatica.parking.request.application;

import com.aleatica.parking.request.dto.RequestResponse;

/**
 * Evento de dominio publicado por {@link RequestService} tras una operacion sobre una
 * solicitud, consumido {@code AFTER_COMMIT} por {@link RequestEventListener}.
 *
 * <p>Desacopla la resolucion de la solicitud (dentro de la transaccion) de la
 * notificacion (fuera de ella): un fallo de envio nunca revierte la resolucion
 * (design §Risks). Transporta un DTO, nunca la entidad JPA.</p>
 *
 * @param kind    tipo de evento
 * @param request instantanea de la solicitud afectada
 */
public record RequestNotificationEvent(Kind kind, RequestResponse request) {

    /** Tipos de evento de notificacion de una solicitud. */
    public enum Kind {
        /** Solicitud creada (destinatarios: administradores). */
        CREATED,
        /** Solicitud aprobada (destinatario: empleado). */
        APPROVED,
        /** Solicitud rechazada (destinatario: empleado). */
        REJECTED
    }
}
