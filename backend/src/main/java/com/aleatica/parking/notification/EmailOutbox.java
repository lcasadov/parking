package com.aleatica.parking.notification;

import com.aleatica.parking.notification.application.EmailMessage;
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
 * Entidad de persistencia de un email pendiente de reintento (tabla
 * {@code dbo.email_outbox}).
 *
 * <p>Se crea SOLO cuando el envio inmediato (tras el commit del evento origen) falla.
 * Guarda el mensaje ya renderizado ({@code recipient}, {@code subject}, {@code body_html})
 * para que el job de reintento reenvie sin re-resolver destinatarios ni re-renderizar
 * plantillas. Es un adaptador de salida: nunca se expone en la capa web (S4684).</p>
 *
 * <p>La maquina de estados es {@code PENDING -> SENT | FAILED}: el job de reintento solo
 * relee {@code PENDING}, por lo que un {@code SENT} no se reenvia jamas (idempotencia), y
 * al agotar la politica de maximo de reintentos la fila pasa a {@code FAILED} y deja de
 * reintentarse (evita el bucle infinito ante un destinatario sin email valido).</p>
 */
@Entity
@Table(name = "email_outbox")
public class EmailOutbox {

    /** Longitud maxima persistible del detalle de error (columna {@code last_error}). */
    private static final int MAX_ERROR_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient", nullable = false, length = 255)
    private String recipient;

    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    @Column(name = "body_html", nullable = false)
    private String bodyHtml;

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
     * Crea una entrada {@code PENDING} para un email cuyo envio inmediato ha fallado
     * (primer intento ya consumido: {@code attempts = 1}).
     *
     * @param message mensaje renderizado que no pudo enviarse
     * @param error   detalle del fallo (se trunca a 500 caracteres)
     * @param now     instante actual (UTC), del reloj inyectable
     * @return la entrada pendiente de reintento, aun no persistida
     */
    public static EmailOutbox pending(EmailMessage message, String error, Instant now) {
        EmailOutbox outbox = new EmailOutbox();
        outbox.recipient = message.to();
        outbox.subject = message.subject();
        outbox.bodyHtml = message.htmlBody();
        outbox.status = EmailOutboxStatus.PENDING;
        outbox.attempts = 1;
        outbox.lastError = truncateError(error);
        outbox.createdAt = now;
        outbox.lastAttemptAt = now;
        return outbox;
    }

    /**
     * Reconstruye el mensaje renderizado para reenviarlo en un reintento.
     *
     * @return el mensaje equivalente a esta entrada
     */
    public EmailMessage toMessage() {
        return new EmailMessage(recipient, subject, bodyHtml);
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

    private static String truncateError(String error) {
        if (error == null) {
            return null;
        }
        return error.length() > MAX_ERROR_LENGTH ? error.substring(0, MAX_ERROR_LENGTH) : error;
    }

    public Long getId() {
        return id;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getSubject() {
        return subject;
    }

    public String getBodyHtml() {
        return bodyHtml;
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
