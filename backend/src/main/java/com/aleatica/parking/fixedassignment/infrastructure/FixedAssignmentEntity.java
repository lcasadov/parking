package com.aleatica.parking.fixedassignment.infrastructure;

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
 * Entidad de persistencia (adaptador de salida) de una asignacion fija (tabla
 * {@code dbo.fixed_assignments}). Arquitectura hexagonal, change {@code hexagonal-persistence}.
 *
 * <p>Es la representacion JPA del agregado; el modelo de negocio vive en
 * {@link com.aleatica.parking.fixedassignment.domain.FixedAssignment} y el puente
 * entidad&harr;dominio lo resuelve {@link FixedAssignmentMapper}. Nunca se expone en la capa
 * web (S4684); el controlador trabaja con DTOs. Las referencias a otras tablas se guardan como
 * identificadores ({@code Long}) en lugar de {@code @ManyToOne}, evitando por diseno cualquier
 * consulta N+1 y manteniendo el agregado desacoplado.</p>
 *
 * <p>Conserva las factorias de conveniencia ({@link #create}) usadas por los casos de uso
 * vecinos aun no migrados ({@code availability}, {@code floor-plan}, {@code visitor},
 * {@code release}) que hoy consultan esta tabla directamente; el mapeo dominio&rarr;entidad de
 * este agregado usa el constructor de todos los campos.</p>
 *
 * <p>Tras el refactor a recurso generico la referencia reservable es {@code resource_id} +
 * {@code resource_type} ({@link ResourceType}); en el nucleo de parking el tipo es siempre
 * {@link ResourceType#PARKING}. El DTO de salida sigue exponiendo {@code parkingSpaceId}
 * (contrato invariable).</p>
 */
@Entity
@Table(name = "fixed_assignments")
public class FixedAssignmentEntity {

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
    protected FixedAssignmentEntity() {
        // JPA
    }

    /**
     * Constructor de todos los campos usado por {@link FixedAssignmentMapper} para mapear el
     * modelo de dominio a la entidad (alta con {@code id == null}, actualizacion con {@code id}
     * presente).
     *
     * @param id           identificador ({@code null} en el alta)
     * @param resourceId   recurso asignado
     * @param resourceType tipo del recurso
     * @param employeeId   empleado titular
     * @param dayOfWeek    dia de la semana (1-7)
     * @param active       si la asignacion esta vigente
     * @param createdById  empleado (ADMIN) que la creo
     * @param createdAt    instante de alta (UTC)
     * @param revokedById  empleado (ADMIN) que la revoco; {@code null} si vigente
     * @param revokedAt    instante de revocacion (UTC); {@code null} si vigente
     */
    public FixedAssignmentEntity(
            Long id, Long resourceId, ResourceType resourceType, Long employeeId, Integer dayOfWeek,
            boolean active, Long createdById, Instant createdAt, Long revokedById,
            Instant revokedAt) {
        this.id = id;
        this.resourceId = resourceId;
        this.resourceType = resourceType;
        this.employeeId = employeeId;
        this.dayOfWeek = dayOfWeek;
        this.active = active;
        this.createdById = createdById;
        this.createdAt = createdAt;
        this.revokedById = revokedById;
        this.revokedAt = revokedAt;
    }

    /**
     * Da de alta una entidad de asignacion fija activa de tipo {@code PARKING} (plaza).
     *
     * @param resourceId  recurso asignado (plaza en el nucleo de parking)
     * @param employeeId  empleado titular
     * @param dayOfWeek   dia de la semana (1-7)
     * @param createdById empleado (ADMIN) que crea la asignacion
     * @param now         instante de creacion (UTC)
     * @return la entidad nueva, aun no persistida
     */
    public static FixedAssignmentEntity create(
            Long resourceId, Long employeeId, Integer dayOfWeek, Long createdById, Instant now) {
        return create(resourceId, ResourceType.PARKING, employeeId, dayOfWeek, createdById, now);
    }

    /**
     * Da de alta una entidad de asignacion fija activa para un tipo de recurso concreto
     * ({@code PARKING} plaza / {@code DESK} puesto).
     *
     * @param resourceId   recurso asignado (plaza o puesto segun {@code resourceType})
     * @param resourceType tipo del recurso ({@code PARKING}/{@code DESK})
     * @param employeeId   empleado titular
     * @param dayOfWeek    dia de la semana (1-7)
     * @param createdById  empleado (ADMIN) que crea la asignacion
     * @param now          instante de creacion (UTC)
     * @return la entidad nueva, aun no persistida
     */
    public static FixedAssignmentEntity create(
            Long resourceId, ResourceType resourceType, Long employeeId, Integer dayOfWeek,
            Long createdById, Instant now) {
        return new FixedAssignmentEntity(
                null, resourceId, resourceType, employeeId, dayOfWeek, true, createdById, now,
                null, null);
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
        if (!(other instanceof FixedAssignmentEntity assignment)) {
            return false;
        }
        return id != null && id.equals(assignment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
