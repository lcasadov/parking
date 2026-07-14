package com.aleatica.parking.notification.application;

import com.aleatica.parking.notification.EmailOutbox;
import com.aleatica.parking.request.dto.RequestResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Convierte entre una {@link NotificationCommand} (modelo de la orden de notificacion) y su
 * fila persistente {@link EmailOutbox}, serializando/deserializando la instantanea de la
 * solicitud ({@link RequestResponse}) a/desde JSON con el {@link ObjectMapper} de la
 * aplicacion.
 *
 * <p>Aisla la dependencia de Jackson fuera de la entidad JPA: {@link EmailOutbox} solo
 * guarda el JSON como {@code String}. El payload NO contiene secretos (es el mismo DTO que
 * la API ya expone), por lo que persistirlo es seguro; PASSWORD_RESET queda excluido del
 * outbox por politica (ver {@code NotificationEventType}).</p>
 */
@Component
public class NotificationOutboxMapper {

    private static final String MSG_SERIALIZE_FAILED =
            "No se pudo serializar el payload de la notificacion para el outbox";
    private static final String MSG_DESERIALIZE_FAILED =
            "No se pudo deserializar el payload de la notificacion del outbox";

    private final ObjectMapper objectMapper;

    /**
     * @param objectMapper mapeador JSON de la aplicacion (con soporte de fechas JSR-310)
     */
    public NotificationOutboxMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Construye una fila {@code PENDING} del outbox a partir de una orden de notificacion
     * cuyo envio inmediato fallo.
     *
     * @param command orden de notificacion a encolar
     * @param error   detalle del fallo SMTP
     * @param now     instante actual (UTC)
     * @return la entrada pendiente, aun no persistida
     */
    public EmailOutbox toPendingOutbox(NotificationCommand command, String error, Instant now) {
        return EmailOutbox.pending(
                command.eventType(),
                command.recipientEmployeeId(),
                serialize(command.request()),
                error,
                now);
    }

    /**
     * Reconstruye la orden de notificacion desde una fila del outbox para re-renderizarla
     * en un reintento.
     *
     * @param entry fila del almacen de reintento
     * @return la orden de notificacion equivalente
     */
    public NotificationCommand toCommand(EmailOutbox entry) {
        return new NotificationCommand(
                entry.getEventType(),
                entry.getRecipientEmployeeId(),
                deserialize(entry.getRequestPayload()));
    }

    private String serialize(RequestResponse request) {
        if (request == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(MSG_SERIALIZE_FAILED, ex);
        }
    }

    private RequestResponse deserialize(String payload) {
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.readValue(payload, RequestResponse.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(MSG_DESERIALIZE_FAILED, ex);
        }
    }
}
