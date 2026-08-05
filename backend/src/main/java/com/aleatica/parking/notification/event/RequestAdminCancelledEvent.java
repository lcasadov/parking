package com.aleatica.parking.notification.event;

import com.aleatica.parking.request.dto.RequestResponse;

/**
 * Evento de dominio: un {@code ADMIN} ha cancelado una reserva {@code APPROVED} de un empleado
 * (change {@code push-notifications}, design D12). Lo publica {@code RequestService#adminCancel}
 * y lo consume la capability {@code notifications} {@code AFTER_COMMIT} para avisar al
 * <strong>empleado afectado</strong> (por email y push) de que su reserva ha sido cancelada.
 *
 * <p>Es independiente de {@link RequestCancelledEvent} (que avisa a los admins de la liberacion
 * del recurso): {@code adminCancel} publica ambos. La cancelacion que inicia el propio empleado
 * NO publica este evento (no se auto-notifica).</p>
 *
 * <p>Transporta un DTO ({@link RequestResponse}), nunca la entidad JPA (S4684 / OWASP API3).</p>
 *
 * @param request instantanea de la reserva cancelada por el admin (estado previo {@code APPROVED})
 */
public record RequestAdminCancelledEvent(RequestResponse request) {
}
