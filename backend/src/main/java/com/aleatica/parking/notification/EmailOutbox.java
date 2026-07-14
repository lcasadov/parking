package com.aleatica.parking.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entidad de persistencia de una notificacion pendiente de reintento (tabla
 * {@code dbo.email_outbox}).
 *
 * <p>Se crea SOLO cuando el envio inmediato (tras el commit del evento origen) falla. A
 * diferencia del diseno anterior (que congelaba el HTML ya renderizado en {@code body_html}),
 * la fila guarda ahora los <strong>datos del evento</strong>: el {@code event_type} y el
 * payload minimo ({@code recipient_employee_id} y, cuando aplica, la instantanea de la
 * solicitud serializada en {@code request_payload}). Asi el job de reintento re-resuelve el
 * destinatario y el recurso y <strong>re-renderiza con la plantilla vigente</strong>, sin
 * reenviar jamas contenido obsoleto. Es un adaptador de salida: nunca se expone en la capa
 * web (S4684).</p>
 *
 * <p>La maquina de estados es {@code PENDING -> SENT | FAILED}: el job de reintento solo
 * relee {@code PENDING}, por lo que un {@code SENT} no se reenvia jamas (idempotencia), y
 * al agotar la politica de maximo de reintentos (o si el destinatario ya no es resoluble)
 * la fila pasa a {@code FAILED} y deja de reintentarse (evita el bucle infinito).</p>
 */
@Entity
@Table(name = "email_outbox")
public class EmailOutbox {

    /** Longitud maxima persistible del detalle de error (columna {@code last_error}). */
    private static final int MAX_ERROR_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private NotificationEventType eventType;

    @Column(name = "recipient_employee_id", nullable = false)
    private Long recipientEmployeeId;

    @Column(name = "request_payload")
    private String requestPayload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EmailOutboxStatus status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "sent_at")
    private Instant sentAt;

    /** Constructor sin argumentos requerido por JPA. */
    protected EmailOutbox() {
        // JPA
    }

    /**
     * Crea una entrada {@code PENDING} para una notificacion cuyo envio inmediato ha fallado
     * (primer intento ya consumido: {@code attempts = 1}). Persiste los datos del evento (no
     * el HTML) para que el reintento re-renderice con la plantilla vigente.
     *
     * @param eventType           tipo de evento a re-renderizar en el reintento
     * @param recipientEmployeeId identificador del empleado destinatario
     * @param requestPayload      instantanea de la solicitud serializada (JSON); {@code null}
     *                            si el evento no deriva de una solicitud
     * @param error               detalle del fallo (se trunca a 500 caracteres)
     * @param now                 instante actual (UTC), del reloj inyectable
     * @return la entrada pendiente de reintento, aun no persistida
     */
    public static EmailOutbox pending(
            NotificationEventType eventType, Long recipientEmployeeId,
            String requestPayload, String error, Instant now) {
        EmailOutbox outbox = new EmailOutbox();
        outbox.eventType = eventType;
        outbox.recipientEmployeeId = recipientEmployeeId;
        outbox.requestPayload = requestPayload;
        outbox.status = EmailOutboxStatus.PENDING;
        outbox.attempts = 1;
        outbox.lastError = truncateError(error);
        outbox.createdAt = now;
        outbox.lastAttemptAt = now;
        return outbox;
    }

    /**
     * Marca la entrada como enviada con exito (estado terminal).
     *
     * @param now instante del envio (UTC)
     */
    public void markSent(Instant now) {
        this.status = EmailOutboxStatus.SENT;
        this.sentAt = now;
        this.lastAttemptAt = now;
    }

    /**
     * Registra un reintento fallido: incrementa el contador y, si alcanza el maximo
     * permitido, pasa la entrada a {@code FAILED} (deja de reintentarse); en otro caso
     * la conserva {@code PENDING}.
     *
     * @param now         instante del reintento (UTC)
     * @param error       detalle del fallo (se trunca a 500 caracteres)
     * @param maxAttempts numero maximo de intentos permitidos por la politica
     */
    public void registerFailedRetry(Instant now, String error, int maxAttempts) {
        this.attempts++;
        this.lastAttemptAt = now;
        this.lastError = truncateError(error);
        if (this.attempts >= maxAttempts) {
            this.status = EmailOutboxStatus.FAILED;
        }
    }

    /**
     * Descarta la entrada como {@code FAILED} (estado terminal) sin consumir un reintento,
     * cuando ya no puede re-renderizarse (p. ej. el empleado destinatario fue eliminado y no
     * es resoluble). Evita el reprocesado indefinido de una fila irrecuperable.
     *
     * @param now   instante del descarte (UTC)
     * @param error motivo del descarte (se trunca a 500 caracteres)
     */
    public void markFailed(Instant now, String error) {
        this.status = EmailOutboxStatus.FAILED;
        this.lastAttemptAt = now;
        this.lastError = truncateError(error);
    }

    private static String truncateError(String error) {
        if (error == null) {
            return null;
        }
        return error.length() > MAX_ERROR_LENGTH ? error.substring(0, MAX_ERROR_LENGTH) : error;
    }

    public Long getId() {
        return id;
    }

    public NotificationEventType getEventType() {
        return eventType;
    }

    public Long getRecipientEmployeeId() {
        return recipientEmployeeId;
    }

    public String getRequestPayload() {
        return requestPayload;
    }

    public EmailOutboxStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastAttemptAt() {
        return lastAttemptAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}
