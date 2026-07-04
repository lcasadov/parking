package com.aleatica.parking.notification.event;

import com.aleatica.parking.request.dto.RequestResponse;

/**
 * Evento de dominio: se ha rechazado una solicitud puntual. Lo publica el caso de uso de
 * rechazo y lo consume la capability {@code notifications} {@code AFTER_COMMIT} para
 * notificar por email al empleado solicitante, incluyendo el motivo del rechazo
 * ({@code rejectionReason}, viaja dentro del DTO).
 *
 * <p>Transporta un DTO ({@link RequestResponse}), nunca la entidad JPA (S4684 / OWASP
 * API3).</p>
 *
 * @param request instantanea de la solicitud rechazada (con {@code rejectionReason})
 */
public record RequestRejectedEvent(RequestResponse request) {
}
