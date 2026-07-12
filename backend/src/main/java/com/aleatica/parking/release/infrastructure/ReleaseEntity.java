package com.aleatica.parking.release.infrastructure;

import com.aleatica.parking.release.domain.ReleaseType;
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
import java.time.LocalDate;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entidad de persistencia (adaptador de salida) de una liberacion (tabla {@code dbo.releases}).
 * Arquitectura hexagonal, change {@code hexagonal-persistence}.
 *
 * <p>Es la representacion JPA del agregado; el modelo de negocio vive en
 * {@link com.aleatica.parking.release.domain.Release} y el puente entidad&harr;dominio lo resuelve
 * {@link ReleaseMapper}. Nunca se expone en la capa web (S4684); el controlador trabaja con DTOs.
 * Las referencias a otras tablas se guardan como identificadores ({@code Long}) en lugar de
 * {@code @ManyToOne}, evitando por diseno cualquier consulta N+1 y manteniendo el agregado
 * desacoplado.</p>
 *
 * <p>Conserva las factorias de conveniencia ({@link #voluntary}, {@link #administrative}) usadas
 * por los casos de uso vecinos aun no migrados ({@code availability}, {@code floor-plan}) que hoy
 * consultan esta tabla directamente; el mapeo dominio&rarr;entidad de este agregado usa el
 * constructor de todos los campos.</p>
 *
 * <p>Tras el refactor a recurso generico la referencia reservable es {@code resource_id} +
 * {@code resource_type} ({@link ResourceType}); en el nucleo de parking el tipo es siempre
 * {@link ResourceType#PARKING}. El DTO de salida sigue exponiendo {@code parkingSpaceId}
 * (contrato invariable).</p>
 */
@Entity
@Table(name = "releases")
public class ReleaseEntity {

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

    @Column(name = "release_date", nullable = false)
    private LocalDate releaseDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 15)
    private ReleaseType type;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "released_by_id", nullable = false)
    private Long releasedById;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Constructor sin argumentos requerido por JPA. */
    protected ReleaseEntity() {
        // JPA
    }

    /**
     * Constructor de todos los campos usado por {@link ReleaseMapper} para mapear el modelo de
     * dominio a la entidad (alta con {@code id == null}, actualizacion con {@code id} presente).
     *
     * @param id           identificador ({@code null} en el alta)
     * @param resourceId   recurso liberado
     * @param resourceType tipo del recurso
     * @param employeeId   empleado titular cuyo recurso se libera
     * @param releaseDate  fecha liberada
     * @param type         tipo de liberacion
     * @param reason       motivo; {@code null} en las voluntarias
     * @param releasedById empleado que ejecuto la liberacion
     * @param createdAt    instante de creacion (UTC)
     */
    public ReleaseEntity(
            Long id, Long resourceId, ResourceType resourceType, Long employeeId,
            LocalDate releaseDate, ReleaseType type, String reason, Long releasedById,
            Instant createdAt) {
        this.id = id;
        this.resourceId = resourceId;
        this.resourceType = resourceType;
        this.employeeId = employeeId;
        this.releaseDate = releaseDate;
        this.type = type;
        this.reason = reason;
        this.releasedById = releasedById;
        this.createdAt = createdAt;
    }

    /**
     * Da de alta una entidad de liberacion voluntaria de tipo {@code PARKING}: el titular libera
     * su propio recurso ({@code employee_id = released_by_id}, {@code reason = null}).
     *
     * @param resourceId  recurso liberado (resuelto de la asignacion fija)
     * @param employeeId  empleado titular = ejecutor
     * @param releaseDate fecha liberada (presente o futura)
     * @param now         instante de creacion (UTC)
     * @return la entidad nueva, aun no persistida
     */
    public static ReleaseEntity voluntary(
            Long resourceId, Long employeeId, LocalDate releaseDate, Instant now) {
        return voluntary(resourceId, ResourceType.PARKING, employeeId, releaseDate, now);
    }

    /**
     * Da de alta una entidad de liberacion voluntaria de un recurso de un tipo concreto.
     *
     * @param resourceId   recurso liberado (resuelto de la asignacion fija)
     * @param resourceType tipo del recurso ({@code PARKING}/{@code DESK})
     * @param employeeId   empleado titular = ejecutor
     * @param releaseDate  fecha liberada (presente o futura)
     * @param now          instante de creacion (UTC)
     * @return la entidad nueva, aun no persistida
     */
    public static ReleaseEntity voluntary(
            Long resourceId, ResourceType resourceType, Long employeeId, LocalDate releaseDate,
            Instant now) {
        return new ReleaseEntity(null, resourceId, resourceType, employeeId, releaseDate,
                ReleaseType.VOLUNTARY, null, employeeId, now);
    }

    /**
     * Da de alta una entidad de liberacion administrativa de tipo {@code PARKING}.
     *
     * @param resourceId   recurso liberado
     * @param employeeId   empleado titular cuyo recurso se libera
     * @param releaseDate  fecha liberada (presente o futura)
     * @param reason       motivo obligatorio de la liberacion administrativa
     * @param releasedById empleado (ADMIN) que ejecuta la liberacion
     * @param now          instante de creacion (UTC)
     * @return la entidad nueva, aun no persistida
     */
    public static ReleaseEntity administrative(
            Long resourceId, Long employeeId, LocalDate releaseDate, String reason,
            Long releasedById, Instant now) {
        return administrative(resourceId, ResourceType.PARKING, employeeId, releaseDate, reason,
                releasedById, now);
    }

    /**
     * Da de alta una entidad de liberacion administrativa de un recurso de un tipo concreto.
     *
     * @param resourceId   recurso liberado
     * @param resourceType tipo del recurso ({@code PARKING}/{@code DESK})
     * @param employeeId   empleado titular cuyo recurso se libera
     * @param releaseDate  fecha liberada (presente o futura)
     * @param reason       motivo obligatorio de la liberacion administrativa
     * @param releasedById empleado (ADMIN) que ejecuta la liberacion
     * @param now          instante de creacion (UTC)
     * @return la entidad nueva, aun no persistida
     */
    public static ReleaseEntity administrative(
            Long resourceId, ResourceType resourceType, Long employeeId, LocalDate releaseDate,
            String reason, Long releasedById, Instant now) {
        return new ReleaseEntity(null, resourceId, resourceType, employeeId, releaseDate,
                ReleaseType.ADMINISTRATIVE, reason, releasedById, now);
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

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public ReleaseType getType() {
        return type;
    }

    public String getReason() {
        return reason;
    }

    public Long getReleasedById() {
        return releasedById;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ReleaseEntity release)) {
            return false;
        }
        return id != null && id.equals(release.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
