package com.aleatica.parking.fixedassignment;

import com.aleatica.parking.resource.ResourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entidad de persistencia de una asignacion fija (tabla {@code dbo.fixed_assignments}).
 *
 * <p>Vinculo indefinido entre un empleado y una plaza de parking para un dia de la
 * semana ({@code day_of_week} 1-7), vigente hasta que el {@code ADMIN} lo revoca. La
 * revocacion es <em>logica</em> ({@code active=false} + {@code revoked_at}/
 * {@code revoked_by_id}), nunca fisica, para preservar el historico (auditoria).</p>
 *
 * <p>Es un adaptador de salida: nunca se expone en la capa web (S4684); el
 * controlador trabaja con DTOs. Las referencias a otras tablas se guardan como
 * identificadores ({@code Long}) en lugar de {@code @ManyToOne}, evitando por diseno
 * cualquier consulta N+1 y manteniendo el agregado desacoplado.</p>
 *
 * <p>Tras el refactor a recurso generico la referencia reservable es
 * {@code resource_id} + {@code resource_type} ({@link ResourceType}); en el nucleo de
 * parking el tipo es siempre {@link ResourceType#PARKING} y {@code resource_id} apunta
 * a la {@code ParkingSpace}. El DTO de salida sigue exponiendo {@code parkingSpaceId}
 * (contrato invariable).</p>
 */
@Entity
@Table(name = "fixed_assignments")
public class FixedAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 10)
    private ResourceType resourceType;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_by_id", nullable = false)
    private Long createdById;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "revoked_by_id")
    private Long revokedById;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "revoked_at")
    private Instant revokedAt;

    /** Constructor sin argumentos requerido por JPA. */
    protected FixedAssignment() {
        // JPA
    }

    /**
     * Da de alta una nueva asignacion fija activa.
     *
     * <p>Nace con {@code active = true}; {@code created_at} lo fija el reloj
     * inyectable ({@code ClockPort}) para tests deterministas.</p>
     *
     * @param resourceId  recurso asignado (plaza en el nucleo de parking)
     * @param employeeId  empleado titular
     * @param dayOfWeek   dia de la semana (1-7)
     * @param createdById empleado (ADMIN) que crea la asignacion
     * @param now         instante de creacion (UTC)
     * @return la asignacion nueva, aun no persistida
     */
    public static FixedAssignment create(
            Long resourceId, Long employeeId, Integer dayOfWeek, Long createdById, Instant now) {
        return create(resourceId, ResourceType.PARKING, employeeId, dayOfWeek, createdById, now);
    }

    /**
     * Da de alta una nueva asignacion fija activa para un tipo de recurso concreto
     * ({@code PARKING} plaza / {@code DESK} puesto).
     *
     * <p>Nace con {@code active = true}; {@code created_at} lo fija el reloj inyectable
     * ({@code ClockPort}) para tests deterministas.</p>
     *
     * @param resourceId   recurso asignado (plaza o puesto segun {@code resourceType})
     * @param resourceType tipo del recurso ({@code PARKING}/{@code DESK})
     * @param employeeId   empleado titular
     * @param dayOfWeek    dia de la semana (1-7)
     * @param createdById  empleado (ADMIN) que crea la asignacion
     * @param now          instante de creacion (UTC)
     * @return la asignacion nueva, aun no persistida
     */
    public static FixedAssignment create(
            Long resourceId, ResourceType resourceType, Long employeeId, Integer dayOfWeek,
            Long createdById, Instant now) {
        FixedAssignment assignment = new FixedAssignment();
        assignment.resourceId = resourceId;
        assignment.resourceType = resourceType;
        assignment.employeeId = employeeId;
        assignment.dayOfWeek = dayOfWeek;
        assignment.active = true;
        assignment.createdById = createdById;
        assignment.createdAt = now;
        return assignment;
    }

    /**
     * Revoca logicamente la asignacion: {@code active=false}, con marca de tiempo y
     * autor de la revocacion. No borra la fila (historico intacto).
     *
     * @param revokedById empleado (ADMIN) que revoca
     * @param now         instante de revocacion (UTC)
     */
    public void revoke(Long revokedById, Instant now) {
        this.active = false;
        this.revokedById = revokedById;
        this.revokedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public Integer getDayOfWeek() {
        return dayOfWeek;
    }

    public boolean isActive() {
        return active;
    }

    public Long getCreatedById() {
        return createdById;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Long getRevokedById() {
        return revokedById;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FixedAssignment assignment)) {
            return false;
        }
        return id != null && id.equals(assignment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
