package com.aleatica.parking.notification;

/**
 * Tipo de evento de notificacion persistido en el almacen de reintento
 * ({@code dbo.email_outbox}).
 *
 * <p>Identifica que plantilla/destinatario debe re-resolverse y re-renderizarse cuando el
 * job de reintento reprocesa una entrada {@code PENDING}. A diferencia del diseno anterior
 * (que congelaba el HTML ya renderizado), el outbox guarda ahora el <em>tipo de evento</em>
 * y su payload minimo, de modo que el reintento re-renderiza con la plantilla vigente y
 * nunca reenvia contenido obsoleto.</p>
 *
 * <p><strong>PASSWORD_RESET queda deliberadamente fuera</strong> de este enum: su correo
 * transporta la contrasena temporal en claro y NO debe persistirse en el outbox
 * (minimizacion RGPD, OWASP A02 "sin secretos en reposo"). Ese correo es best-effort y solo
 * se intenta el envio inmediato; si falla, no se encola.</p>
 */
public enum NotificationEventType {

    /** Nueva solicitud creada; se notifica a cada administrador activo. */
    REQUEST_CREATED,

    /** Solicitud aprobada (manual o auto-aprobacion); se notifica al empleado solicitante. */
    REQUEST_APPROVED,

    /** Solicitud rechazada; se notifica al empleado solicitante con el motivo. */
    REQUEST_REJECTED,

    /**
     * Solicitud {@code APPROVED} cancelada por el empleado (recurso liberado); se notifica a cada
     * administrador activo. La cancelacion de una {@code PENDING} no genera este evento.
     */
    REQUEST_CANCELLED,

    /** Asignacion fija revocada; se notifica al empleado afectado. */
    ASSIGNMENT_REVOKED
}
