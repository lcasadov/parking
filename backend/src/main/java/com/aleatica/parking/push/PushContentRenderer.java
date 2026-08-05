package com.aleatica.parking.push;

import com.aleatica.parking.notification.NotificationEventType;
import com.aleatica.parking.notification.application.NotificationCommand;
import com.aleatica.parking.request.dto.RequestResponse;
import com.aleatica.parking.resource.ResourceType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Construye el payload JSON de una notificacion Web Push (titulo + cuerpo cortos + deep-link)
 * a partir de una {@link NotificationCommand} (change {@code push-notifications}).
 *
 * <p>El texto es conciso y sin datos sensibles de mas (se muestra en pantalla de bloqueo); el
 * detalle vive en la app al abrir via {@code data.url}. Rutas de deep-link: eventos del empleado
 * &rarr; su semana; eventos de admin &rarr; la bandeja de pendientes.</p>
 */
@Component
public class PushContentRenderer {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM");
    private static final String URL_EMPLOYEE = "/employee/my-week";
    private static final String URL_ADMIN = "/admin/requests";

    private final ObjectMapper objectMapper;

    public PushContentRenderer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Renderiza el payload JSON del push para una orden de notificacion.
     *
     * @param command orden (tipo de evento + solicitud asociada, que puede ser {@code null})
     * @return JSON {@code {title, body, url}} listo para el Service Worker
     */
    public String render(NotificationCommand command) {
        RequestResponse request = command.request();
        String date = request != null && request.requestedDate() != null
                ? request.requestedDate().format(DAY)
                : "";
        String kind = resourceKind(request);
        Content content = switch (command.eventType()) {
            case REQUEST_APPROVED -> new Content(
                    "Reserva confirmada", "Tu " + kind + " del " + date + " esta confirmada.", URL_EMPLOYEE);
            case REQUEST_REJECTED -> new Content(
                    "Solicitud rechazada", "Tu solicitud del " + date + " ha sido rechazada.", URL_EMPLOYEE);
            case REQUEST_ADMIN_ASSIGNED -> new Content(
                    "Recurso asignado", "Un administrador te ha asignado una " + kind + " el " + date + ".",
                    URL_EMPLOYEE);
            case REQUEST_ADMIN_CANCELLED -> new Content(
                    "Reserva cancelada", "Un administrador ha cancelado tu reserva del " + date + ".",
                    URL_EMPLOYEE);
            case ASSIGNMENT_REVOKED -> new Content(
                    "Asignacion fija revocada", "Se ha revocado tu asignacion fija.", URL_EMPLOYEE);
            case REQUEST_CREATED -> new Content(
                    "Nueva solicitud", "Hay una solicitud pendiente de aprobar del " + date + ".", URL_ADMIN);
            case REQUEST_CANCELLED -> new Content(
                    "Reserva cancelada", "Se ha cancelado una reserva del " + date + " (recurso libre).",
                    URL_ADMIN);
            case WAITLIST_AVAILABLE -> new Content(
                    "Hueco disponible",
                    "Se ha liberado un recurso con lista de espera para el " + date + ".", URL_ADMIN);
        };
        return toJson(content);
    }

    private static String resourceKind(RequestResponse request) {
        if (request == null || request.resourceType() == null) {
            return "recurso";
        }
        return request.resourceType() == ResourceType.PARKING ? "plaza" : "puesto";
    }

    private String toJson(Content content) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("title", content.title());
        payload.put("body", content.body());
        payload.put("url", content.url());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {  // NOSONAR: mapa de strings, no deberia fallar
            return "{\"title\":\"Parking\",\"body\":\"" + content.title() + "\",\"url\":\"" + content.url() + "\"}";
        }
    }

    private record Content(String title, String body, String url) {
    }
}
