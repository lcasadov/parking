package com.aleatica.parking.release;

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
 * Entidad de persistencia de una liberacion (tabla {@code dbo.releases}).
 *
 * <p>Marca un recurso con asignacion fija como disponible para una fecha concreta.
 * Distingue el {@code employee_id} (dueno cuyo recurso se libera) del
 * {@code released_by_id} (quien ejecuta): en {@link ReleaseType#VOLUNTARY} coinciden;
 * en {@link ReleaseType#ADMINISTRATIVE} el ejecutor es un {@code ADMIN} y
 * {@code reason} es obligatorio (data-model §3.4).</p>
 *
 * <p>Es un adaptador de salida: nunca se expone en la capa web (S4684); el
 * controlador trabaja con DTOs. Las referencias a otras tablas se guardan como
 * identificadores ({@code Long}) en lugar de {@code @ManyToOne}, evitando por diseno
 * cualquier consulta N+1 y manteniendo el agregado desacoplado.</p>
 *
 * <p>Tras el refactor a recurso generico la referencia reservable es
 * {@code resource_id} + {@code resource_type} ({@link ResourceType}); en el nucleo de
 * parking el tipo es siempre {@link ResourceType#PARKING}. El DTO de salida sigue
 * exponiendo {@code parkingSpaceId} (contrato invariable).</p>
 */
@Entity
@Table(name = "releases")
public class Release {

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
    protected Release() {
        // JPA
    }

    /**
     * Da de alta una liberacion voluntaria: el titular libera su propio recurso
     * ({@code employee_id = released_by_id}, {@code reason = null}).
     *
     * @param resourceId  recurso liberado (resuelto de la asignacion fija)
     * @param employeeId  empleado titular = ejecutor
     * @param releaseDate fecha liberada (presente o futura)
     * @param now         instante de creacion (UTC)
     * @return la liberacion nueva, aun no persistida
     */
    public static Release voluntary(
            Long resourceId, Long employeeId, LocalDate releaseDate, Instant now) {
        return build(resourceId, employeeId, releaseDate, ReleaseType.VOLUNTARY, null, employeeId, now);
    }

    /**
     * Da de alta una liberacion administrativa: un {@code ADMIN} libera el recurso de
     * un empleado, indicando el motivo obligatorio.
     *
     * @param resourceId   recurso liberado
     * @param employeeId   empleado titular cuyo recurso se libera
     * @param releaseDate  fecha liberada (presente o futura)
     * @param reason       motivo obligatorio de la liberacion administrativa
     * @param releasedById empleado (ADMIN) que ejecuta la liberacion
     * @param now          instante de creacion (UTC)
     * @return la liberacion nueva, aun no persistida
     */
    public static Release administrative(
            Long resourceId, Long employeeId, LocalDate releaseDate, String reason,
            Long releasedById, Instant now) {
        return build(resourceId, employeeId, releaseDate, ReleaseType.ADMINISTRATIVE, reason,
                releasedById, now);
    }

    private static Release build(
            Long resourceId, Long employeeId, LocalDate releaseDate, ReleaseType type,
            String reason, Long releasedById, Instant now) {
        Release release = new Release();
        release.resourceId = resourceId;
        release.resourceType = ResourceType.PARKING;
        release.employeeId = employeeId;
        release.releaseDate = releaseDate;
        release.type = type;
        release.reason = reason;
        release.releasedById = releasedById;
        release.createdAt = now;
        return release;
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
        if (!(other instanceof Release release)) {
            return false;
        }
        return id != null && id.equals(release.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
