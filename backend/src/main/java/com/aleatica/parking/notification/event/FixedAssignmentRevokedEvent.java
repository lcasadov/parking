package com.aleatica.parking.notification.event;

/**
 * Evento de dominio: un administrador ha revocado la(s) asignacion(es) fija(s) de un
 * empleado. Lo publica el caso de uso de revocacion y lo consume la capability
 * {@code notifications} {@code AFTER_COMMIT} para notificar por email al empleado
 * afectado.
 *
 * <p>Transporta solo el identificador del empleado afectado (no una entidad JPA): la
 * capability {@code notifications} resuelve su email en el momento del envio.</p>
 *
 * @param employeeId identificador del empleado cuya asignacion fija fue revocada
 */
public record FixedAssignmentRevokedEvent(Long employeeId) {
}
