package com.aleatica.parking.notification.application;

import com.aleatica.parking.notification.NotificationEventType;
import com.aleatica.parking.request.dto.RequestResponse;

/**
 * Orden de notificacion independiente del renderizado: describe <em>que</em> notificar
 * (tipo de evento), <em>a quien</em> (id del empleado destinatario) y con <em>que datos</em>
 * (instantanea de la solicitud), sin acoplar el destinatario/recurso ni el HTML final.
 *
 * <p>Es el contrato compartido por el flujo inmediato ({@link NotificationDispatcher} lo
 * construye) y por el reintento ({@code NotificationDeliveryService} lo reconstruye desde el
 * outbox): ambos lo pasan a {@link NotificationRenderer}, que resuelve el destinatario y el
 * recurso y re-renderiza la plantilla vigente. Al persistirse (via
 * {@link NotificationOutboxMapper}) el reintento re-renderiza siempre con la plantilla
 * actual, evitando reenviar contenido obsoleto.</p>
 *
 * <p>Transporta un DTO ({@link RequestResponse}), nunca una entidad JPA (S4684 / OWASP
 * API3). El {@code request} es {@code null} para eventos que no derivan de una solicitud
 * (p. ej. {@link NotificationEventType#ASSIGNMENT_REVOKED}).</p>
 *
 * @param eventType           tipo de evento a notificar
 * @param recipientEmployeeId identificador del empleado destinatario (siempre presente)
 * @param request             instantanea de la solicitud; {@code null} si el evento no la usa
 */
public record NotificationCommand(
        NotificationEventType eventType, Long recipientEmployeeId, RequestResponse request) {
}
