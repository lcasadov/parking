package com.aleatica.parking.notification.event;

import com.aleatica.parking.request.dto.RequestResponse;

/**
 * Evento de dominio: un {@code ADMIN} ha asignado puntualmente un recurso a un empleado para
 * una fecha concreta (change {@code restructure-admin-workflows}, capability
 * {@code admin-punctual-assignment}). Lo publica {@code RequestService.adminAssign} y lo
 * consume la capability {@code notifications} {@code AFTER_COMMIT} para notificar al empleado
 * destino con una plantilla propia, distinta de la de "solicitud aprobada" (el empleado no
 * inicio la peticion).
 *
 * <p>Transporta un DTO ({@link RequestResponse}), nunca la entidad JPA (S4684 / OWASP
 * API3).</p>
 *
 * @param request instantanea de la asignacion puntual, ya {@code APPROVED}
 */
public record RequestAdminAssignedEvent(RequestResponse request) {
}
