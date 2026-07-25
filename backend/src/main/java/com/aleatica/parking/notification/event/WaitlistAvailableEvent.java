package com.aleatica.parking.notification.event;

import com.aleatica.parking.request.dto.RequestResponse;

/**
 * Evento de dominio: se ha liberado un recurso (plaza/puesto) para una fecha con solicitudes en
 * <strong>lista de espera</strong>, estando el sistema en modo {@code MANUAL} (change
 * {@code waitlist-requests}). A diferencia del modo {@code AUTOMATIC} (que auto-asigna y
 * reutiliza {@link RequestApprovedEvent}), en {@code MANUAL} el sistema no decide: se publica
 * este evento para avisar a los administradores activos, que resuelven desde la bandeja de
 * pendientes existente.
 *
 * <p>Transporta la solicitud {@code PENDING} en cabeza de la cola (mayor categoria del empleado
 * y, a igualdad, la mas antigua) como referencia del dia y tipo de recurso liberado; el
 * administrador ve el detalle completo de la cola en la bandeja de pendientes. Transporta un DTO
 * ({@link RequestResponse}), nunca la entidad JPA (S4684 / OWASP API3). Lo publica
 * {@code RequestService#promoteWaitlist} y lo consume la capability {@code notifications}
 * {@code AFTER_COMMIT}.</p>
 *
 * @param topWaitlistedRequest solicitud en cabeza de la lista de espera de ese dia/tipo
 */
public record WaitlistAvailableEvent(RequestResponse topWaitlistedRequest) {
}
