package com.aleatica.parking.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entidad de persistencia de una entrada del rastro de auditoria funcional
 * (tabla {@code dbo.audit_log}).
 *
 * <p>La rellena automaticamente el {@link AuditAspect} (casos de uso anotados
 * {@link Auditable}) y los adaptadores de auditoria de cada modulo, nunca desde la web.
 * Es un adaptador de salida: no se expone en la capa web (S4684); la consulta devuelve
 * DTOs. Los atributos enriquecidos (login del actor, IP, user-agent, snapshot
 * antes/despues) viajan serializados como JSON en {@code details}, manteniendo el
 * esquema estable (design §Decisions).</p>
 */
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_employee_id")
    private Long actorEmployeeId;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "details")
    private String details;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    /** Constructor sin argumentos requerido por JPA. */
    protected AuditLog() {
        // JPA
    }

    private AuditLog(AuditEntry entry, Instant occurredAt) {
        this.actorEmployeeId = entry.actorEmployeeId();
        this.action = entry.action();
        this.entityType = entry.entityType();
        this.entityId = entry.entityId();
        this.details = entry.details();
        this.occurredAt = occurredAt;
    }

    /**
     * Construye una entrada de auditoria a partir del comando y el instante de registro.
     *
     * @param entry      datos de la accion auditable
     * @param occurredAt instante UTC en que se registra (reloj inyectable)
     * @return la entrada aun no persistida
     */
    public static AuditLog of(AuditEntry entry, Instant occurredAt) {
        return new AuditLog(entry, occurredAt);
    }

    /**
     * @return identificador unico
     */
    public Long getId() {
        return id;
    }

    /**
     * @return id del empleado actor; {@code null} en acciones del sistema
     */
    public Long getActorEmployeeId() {
        return actorEmployeeId;
    }

    /**
     * @return accion funcional registrada
     */
    public String getAction() {
        return action;
    }

    /**
     * @return tipo de entidad afectada
     */
    public String getEntityType() {
        return entityType;
    }

    /**
     * @return id de la entidad afectada; {@code null} si no aplica
     */
    public Long getEntityId() {
        return entityId;
    }

    /**
     * @return detalle JSON enriquecido; {@code null} si no aplica
     */
    public String getDetails() {
        return details;
    }

    /**
     * @return instante UTC de la accion
     */
    public Instant getOccurredAt() {
        return occurredAt;
    }
}
