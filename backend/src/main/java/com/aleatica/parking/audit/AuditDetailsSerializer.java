package com.aleatica.parking.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Serializa los atributos enriquecidos de una accion auditable a JSON para la columna
 * {@code audit_log.details}.
 *
 * <p>Compone el contexto del actor ({@code actorLogin}, {@code ip}, {@code userAgent}) con
 * un snapshot opcional de la entidad afectada bajo la clave {@code snapshot}. La
 * serializacion es best-effort: si Jackson no puede serializar el snapshot, se registra el
 * fallo y se devuelve el JSON con solo el contexto, de modo que la auditoria nunca tumba la
 * operacion de negocio (design §Decisions).</p>
 */
@Component
public class AuditDetailsSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(AuditDetailsSerializer.class);

    private static final String KEY_ACTOR_LOGIN = "actorLogin";
    private static final String KEY_IP = "ip";
    private static final String KEY_USER_AGENT = "userAgent";
    private static final String KEY_SNAPSHOT = "snapshot";

    private final ObjectMapper objectMapper;

    /**
     * @param objectMapper serializador JSON compartido de la aplicacion
     */
    public AuditDetailsSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Serializa el contexto del actor y un snapshot opcional a un objeto JSON.
     *
     * @param context  contexto del actor (login, IP, user-agent)
     * @param snapshot instantanea de la entidad afectada (DTO); puede ser {@code null}
     * @return el JSON con los atributos enriquecidos
     */
    public String serialize(AuditContext context, Object snapshot) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put(KEY_ACTOR_LOGIN, context.actorLogin());
        details.put(KEY_IP, context.ip());
        details.put(KEY_USER_AGENT, context.userAgent());
        if (snapshot != null) {
            details.put(KEY_SNAPSHOT, snapshot);
        }
        return writeAsString(details);
    }

    /**
     * Serializa un unico objeto (snapshot) a JSON, sin contexto de actor.
     *
     * @param snapshot instantanea a serializar; puede ser {@code null}
     * @return el JSON del objeto, o {@code null} si {@code snapshot} es {@code null}
     */
    public String serialize(Object snapshot) {
        return snapshot == null ? null : writeAsString(snapshot);
    }

    private String writeAsString(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            LOG.warn("No se pudo serializar el detalle de auditoria a JSON: {}", ex.getMessage());
            return null;
        }
    }
}
