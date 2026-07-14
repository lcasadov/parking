package com.aleatica.parking.notification.event;

import com.aleatica.parking.request.dto.RequestResponse;

/**
 * Evento de dominio: un empleado ha cancelado una solicitud que estaba {@code APPROVED}, por lo
 * que el recurso reservado (plaza/puesto) ha quedado liberado. Lo publica el caso de uso de
 * cancelacion <strong>solo</strong> cuando el estado previo era {@code APPROVED} y lo consume la
 * capability {@code notifications} {@code AFTER_COMMIT} para avisar por email a los
 * administradores activos (recurso liberado). La cancelacion de una {@code PENDING} no publica
 * este evento (no libera recurso).
 *
 * <p>Transporta un DTO ({@link RequestResponse}), nunca la entidad JPA (S4684 / OWASP
 * API3).</p>
 *
 * @param request instantanea de la solicitud cancelada (estado previo {@code APPROVED})
 */
public record RequestCancelledEvent(RequestResponse request) {
}
