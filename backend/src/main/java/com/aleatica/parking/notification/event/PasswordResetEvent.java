package com.aleatica.parking.notification.event;

/**
 * Evento de dominio: se ha reseteado la contrasena de un empleado y debe comunicarsele
 * la contrasena temporal por email. 🔵 <strong>Solo Fase 2</strong>: en Fase 1 la
 * contrasena temporal se muestra al administrador en pantalla y no se envia email (ver
 * {@code openspec/changes/init-notifications/specs/notifications/spec.md} §Casos limite).
 *
 * <p>Marcador de Fase 2: el evento y su plantilla ({@code password-reset.html}) existen y
 * son renderizables, pero el cableado del publicador (en {@code EmployeeService}) queda
 * diferido a la Fase 2 y permanece minimo. Transporta primitivos, nunca una entidad JPA.</p>
 *
 * @param employeeId        identificador del empleado destinatario
 * @param email             direccion de email del empleado
 * @param temporaryPassword contrasena temporal en claro (efimera, no se persiste en el outbox salvo reintento)
 */
public record PasswordResetEvent(Long employeeId, String email, String temporaryPassword) {
}
