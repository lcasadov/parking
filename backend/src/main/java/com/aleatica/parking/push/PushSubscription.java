package com.aleatica.parking.push;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Suscripcion Web Push de un navegador/dispositivo de un empleado (tabla
 * {@code dbo.push_subscription}, change {@code push-notifications}).
 *
 * <p>Guarda el {@code endpoint} del push service y las claves {@code p256dh}/{@code auth}
 * del {@code PushManager} necesarias para cifrar el payload (VAPID). Un empleado puede tener
 * varias (multi-dispositivo); unicidad por {@code endpoint} (re-suscribir = upsert). Entidad
 * JPA directa (patron del proyecto para agregados simples); nunca se expone en la capa web.</p>
 */
@Entity
@Table(name = "push_subscription")
public class PushSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "endpoint", nullable = false, length = 1024)
    private String endpoint;

    @Column(name = "p256dh", nullable = false, length = 255)
    private String p256dh;

    @Column(name = "auth", nullable = false, length = 255)
    private String auth;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    /** Constructor sin argumentos requerido por JPA. */
    protected PushSubscription() {
        // JPA
    }

    /**
     * Crea una suscripcion nueva (aun no persistida) para un empleado.
     *
     * @param employeeId empleado propietario
     * @param endpoint   endpoint del push service (URL, unico)
     * @param p256dh     clave publica del cliente (base64url)
     * @param auth       secreto de autenticacion del cliente (base64url)
     * @param userAgent  descripcion del dispositivo/navegador (opcional)
     * @return la suscripcion nueva
     */
    public static PushSubscription of(
            Long employeeId, String endpoint, String p256dh, String auth, String userAgent) {
        PushSubscription subscription = new PushSubscription();
        subscription.employeeId = employeeId;
        subscription.endpoint = endpoint;
        subscription.p256dh = p256dh;
        subscription.auth = auth;
        subscription.userAgent = userAgent;
        return subscription;
    }

    /** Actualiza las claves/dispositivo (upsert al re-suscribir el mismo endpoint). */
    public void updateKeys(String p256dh, String auth, String userAgent) {
        this.p256dh = p256dh;
        this.auth = auth;
        this.userAgent = userAgent;
    }

    public Long getId() {
        return id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getP256dh() {
        return p256dh;
    }

    public String getAuth() {
        return auth;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PushSubscription that)) {
            return false;
        }
        return endpoint != null && endpoint.equals(that.endpoint);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(endpoint);
    }
}
