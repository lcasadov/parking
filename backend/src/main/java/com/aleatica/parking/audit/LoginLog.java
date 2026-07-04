package com.aleatica.parking.audit;

import com.aleatica.parking.auth.domain.LoginPhase;
import com.aleatica.parking.auth.domain.LoginResult;
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
 * Entidad de persistencia de un intento de autenticacion (tabla {@code dbo.login_log}).
 *
 * <p>La rellena el filtro/servicio de autenticacion (via {@code LoginLogRecorder}); esta
 * entidad es el modelo de <em>lectura</em> que consume la consulta de la capability
 * {@code audit-retention}. Es un adaptador de salida: no se expone en la web (S4684); la
 * consulta devuelve DTOs. Se mantiene separada de {@code audit_log} para no contaminar la
 * auditoria funcional con el ruido de autenticacion (security-design §8).</p>
 */
@Entity
@Table(name = "login_log")
public class LoginLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login_attempted", nullable = false, length = 100)
    private String loginAttempted;

    @Column(name = "employee_id")
    private Long employeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 20)
    private LoginResult result;

    @Enumerated(EnumType.STRING)
    @Column(name = "phase", nullable = false, length = 10)
    private LoginPhase phase;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    /** Constructor sin argumentos requerido por JPA. */
    protected LoginLog() {
        // JPA
    }

    /**
     * @return identificador unico
     */
    public Long getId() {
        return id;
    }

    /**
     * @return login tal cual lo intento el usuario
     */
    public String getLoginAttempted() {
        return loginAttempted;
    }

    /**
     * @return id del empleado resuelto; {@code null} si el login no existia
     */
    public Long getEmployeeId() {
        return employeeId;
    }

    /**
     * @return resultado del intento
     */
    public LoginResult getResult() {
        return result;
    }

    /**
     * @return fase de autenticacion
     */
    public LoginPhase getPhase() {
        return phase;
    }

    /**
     * @return direccion IP de origen; {@code null} si no se registro
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * @return user-agent del cliente; {@code null} si no se registro
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * @return instante del intento (UTC)
     */
    public Instant getOccurredAt() {
        return occurredAt;
    }
}
