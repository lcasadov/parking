package com.aleatica.parking.notification.event;

import com.aleatica.parking.request.dto.RequestResponse;

/**
 * Evento de dominio: se ha aprobado una solicitud puntual. Lo publica el caso de uso de
 * aprobacion y lo consume la capability {@code notifications} {@code AFTER_COMMIT} para
 * notificar por email al empleado solicitante, incluyendo la nota del administrador
 * ({@code approvalNote}, viaja dentro del DTO).
 *
 * <p>Transporta un DTO ({@link RequestResponse}), nunca la entidad JPA (S4684 / OWASP
 * API3).</p>
 *
 * @param request instantanea de la solicitud aprobada (con {@code approvalNote})
 */
public record RequestApprovedEvent(RequestResponse request) {
}
