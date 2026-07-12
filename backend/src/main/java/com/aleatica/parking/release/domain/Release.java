package com.aleatica.parking.release.domain;

import com.aleatica.parking.resource.ResourceType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Modelo de dominio de una liberacion de recurso (arquitectura hexagonal, change
 * {@code hexagonal-persistence}).
 *
 * <p>Es el nucleo de negocio del agregado {@code release}: <strong>libre de framework</strong>
 * (sin JPA, sin Spring, sin Hibernate). Marca un recurso con asignacion fija como disponible para
 * una fecha concreta. Distingue el {@code employeeId} (dueno cuyo recurso se libera) del
 * {@code releasedById} (quien ejecuta): en {@link ReleaseType#VOLUNTARY} coinciden; en
 * {@link ReleaseType#ADMINISTRATIVE} el ejecutor es un {@code ADMIN} y {@code reason} es
 * obligatorio (data-model §3.4). La persistencia la resuelve un adaptador de infraestructura que
 * mapea este modelo a/desde una entidad JPA ({@code ReleaseEntity}) a traves de
 * {@code ReleaseMapper}.</p>
 *
 * <p>La referencia reservable es {@code resourceId} + {@code resourceType}
 * ({@link ResourceType}); en el nucleo de parking el tipo es siempre {@link ResourceType#PARKING}.
 * Las referencias a otras tablas se guardan como identificadores ({@code Long}), manteniendo el
 * agregado desacoplado.</p>
 */
public class Release {

    private Long id;
    private Long resourceId;
    private ResourceType resourceType;
    private Long employeeId;
    private LocalDate releaseDate;
    private ReleaseType type;
    private String reason;
    private Long releasedById;
    private Instant createdAt;

    /** Constructor privado; las instancias se obtienen por las factorias estaticas. */
    private Release() {
        // factorias
    }

    /**
     * Da de alta una liberacion voluntaria: el titular libera su propio recurso
     * ({@code employeeId = releasedById}, {@code reason = null}), de tipo {@code PARKING}.
     *
     * @param resourceId  recurso liberado (resuelto de la asignacion fija)
     * @param employeeId  empleado titular = ejecutor
     * @param releaseDate fecha liberada (presente o futura)
     * @param now         instante de creacion (UTC)
     * @return la liberacion nueva, aun no persistida
     */
    public static Release voluntary(
            Long resourceId, Long employeeId, LocalDate releaseDate, Instant now) {
        return voluntary(resourceId, ResourceType.PARKING, employeeId, releaseDate, now);
    }

    /**
     * Da de alta una liberacion voluntaria de un recurso de un tipo concreto
     * ({@code PARKING} plaza / {@code DESK} puesto): el titular libera su propio recurso
     * ({@code employeeId = releasedById}, {@code reason = null}).
     *
     * @param resourceId   recurso liberado (resuelto de la asignacion fija)
     * @param resourceType tipo del recurso ({@code PARKING}/{@code DESK})
     * @param employeeId   empleado titular = ejecutor
     * @param releaseDate  fecha liberada (presente o futura)
     * @param now          instante de creacion (UTC)
     * @return la liberacion nueva, aun no persistida
     */
    public static Release voluntary(
            Long resourceId, ResourceType resourceType, Long employeeId, LocalDate releaseDate,
            Instant now) {
        return build(resourceId, resourceType, employeeId, releaseDate, ReleaseType.VOLUNTARY, null,
                employeeId, now);
    }

    /**
     * Da de alta una liberacion administrativa de tipo {@code PARKING}: un {@code ADMIN} libera el
     * recurso de un empleado, indicando el motivo obligatorio.
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
        return administrative(resourceId, ResourceType.PARKING, employeeId, releaseDate, reason,
                releasedById, now);
    }

    /**
     * Da de alta una liberacion administrativa de un recurso de un tipo concreto
     * ({@code PARKING} plaza / {@code DESK} puesto): un {@code ADMIN} libera el recurso de
     * un empleado, indicando el motivo obligatorio.
     *
     * @param resourceId   recurso liberado
     * @param resourceType tipo del recurso ({@code PARKING}/{@code DESK})
     * @param employeeId   empleado titular cuyo recurso se libera
     * @param releaseDate  fecha liberada (presente o futura)
     * @param reason       motivo obligatorio de la liberacion administrativa
     * @param releasedById empleado (ADMIN) que ejecuta la liberacion
     * @param now          instante de creacion (UTC)
     * @return la liberacion nueva, aun no persistida
     */
    public static Release administrative(
            Long resourceId, ResourceType resourceType, Long employeeId, LocalDate releaseDate,
            String reason, Long releasedById, Instant now) {
        return build(resourceId, resourceType, employeeId, releaseDate, ReleaseType.ADMINISTRATIVE,
                reason, releasedById, now);
    }

    private static Release build(
            Long resourceId, ResourceType resourceType, Long employeeId, LocalDate releaseDate,
            ReleaseType type, String reason, Long releasedById, Instant now) {
        Release release = new Release();
        release.resourceId = resourceId;
        release.resourceType = resourceType;
        release.employeeId = employeeId;
        release.releaseDate = releaseDate;
        release.type = type;
        release.reason = reason;
        release.releasedById = releasedById;
        release.createdAt = now;
        return release;
    }

    /**
     * Reconstituye una liberacion a partir de su estado persistido (uso exclusivo del mapper de
     * infraestructura {@code ReleaseMapper}; no aplica reglas de negocio).
     *
     * @param id           identificador
     * @param resourceId   recurso liberado
     * @param resourceType tipo del recurso
     * @param employeeId   empleado titular cuyo recurso se libera
     * @param releaseDate  fecha liberada
     * @param type         tipo de liberacion
     * @param reason       motivo; {@code null} en las voluntarias
     * @param releasedById empleado que ejecuto la liberacion
     * @param createdAt    instante de creacion (UTC)
     * @return la liberacion reconstituida
     */
    public static Release restore(
            Long id, Long resourceId, ResourceType resourceType, Long employeeId,
            LocalDate releaseDate, ReleaseType type, String reason, Long releasedById,
            Instant createdAt) {
        Release release = new Release();
        release.id = id;
        release.resourceId = resourceId;
        release.resourceType = resourceType;
        release.employeeId = employeeId;
        release.releaseDate = releaseDate;
        release.type = type;
        release.reason = reason;
        release.releasedById = releasedById;
        release.createdAt = createdAt;
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
