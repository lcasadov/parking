package com.aleatica.parking.fixedassignment.domain;

import com.aleatica.parking.resource.ResourceType;
import java.time.Instant;
import java.util.Objects;

/**
 * Modelo de dominio de una asignacion fija (arquitectura hexagonal, change
 * {@code hexagonal-persistence}).
 *
 * <p>Es el nucleo de negocio del agregado {@code fixedassignment}: <strong>libre de
 * framework</strong> (sin JPA, sin Spring, sin Hibernate). Vinculo indefinido entre un
 * empleado y un recurso (plaza o puesto) para un dia de la semana ({@code dayOfWeek} 1-7),
 * vigente hasta que el {@code ADMIN} lo revoca. La revocacion es <em>logica</em>
 * ({@link #revoke(Long, Instant)}: {@code active=false} + {@code revokedAt}/
 * {@code revokedById}), nunca fisica, para preservar el historico (auditoria). La
 * persistencia la resuelve un adaptador de infraestructura que mapea este modelo a/desde una
 * entidad JPA ({@code FixedAssignmentEntity}) a traves de {@code FixedAssignmentMapper}.</p>
 *
 * <p>La referencia reservable es {@code resourceId} + {@code resourceType}
 * ({@link ResourceType}); en el nucleo de parking el tipo es siempre
 * {@link ResourceType#PARKING} y {@code resourceId} apunta a la {@code ParkingSpace}. Las
 * referencias a otras tablas se guardan como identificadores ({@code Long}), manteniendo el
 * agregado desacoplado.</p>
 */
public class FixedAssignment {

    private Long id;
    private Long resourceId;
    private ResourceType resourceType;
    private Long employeeId;
    private Integer dayOfWeek;
    private boolean active = true;
    private Long createdById;
    private Instant createdAt;
    private Long revokedById;
    private Instant revokedAt;

    /** Constructor privado; las instancias se obtienen por las factorias estaticas. */
    private FixedAssignment() {
        // factorias
    }

    /**
     * Da de alta una nueva asignacion fija activa de tipo {@code PARKING} (plaza).
     *
     * <p>Nace con {@code active = true}; {@code createdAt} lo fija el reloj inyectable
     * ({@code ClockPort}) para tests deterministas.</p>
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
     * <p>Nace con {@code active = true}; {@code createdAt} lo fija el reloj inyectable
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
     * Reconstituye una asignacion fija a partir de su estado persistido (uso exclusivo del
     * mapper de infraestructura {@code FixedAssignmentMapper}; no aplica reglas de negocio).
     *
     * @param id           identificador
     * @param resourceId   recurso asignado
     * @param resourceType tipo del recurso
     * @param employeeId   empleado titular
     * @param dayOfWeek    dia de la semana (1-7)
     * @param active       si la asignacion esta vigente
     * @param createdById  empleado (ADMIN) que la creo
     * @param createdAt    instante de alta (UTC)
     * @param revokedById  empleado (ADMIN) que la revoco; {@code null} si vigente
     * @param revokedAt    instante de revocacion (UTC); {@code null} si vigente
     * @return la asignacion reconstituida
     */
    public static FixedAssignment restore(
            Long id, Long resourceId, ResourceType resourceType, Long employeeId, Integer dayOfWeek,
            boolean active, Long createdById, Instant createdAt, Long revokedById,
            Instant revokedAt) {
        FixedAssignment assignment = new FixedAssignment();
        assignment.id = id;
        assignment.resourceId = resourceId;
        assignment.resourceType = resourceType;
        assignment.employeeId = employeeId;
        assignment.dayOfWeek = dayOfWeek;
        assignment.active = active;
        assignment.createdById = createdById;
        assignment.createdAt = createdAt;
        assignment.revokedById = revokedById;
        assignment.revokedAt = revokedAt;
        return assignment;
    }

    /**
     * Revoca logicamente la asignacion: {@code active=false}, con marca de tiempo y autor de
     * la revocacion. No borra la fila (historico intacto).
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
