/**
 * Modulo de liberaciones: la entidad de persistencia
 * {@link com.aleatica.parking.release.Release}, su repositorio JPA, los casos de uso
 * ({@link com.aleatica.parking.release.application.ReleaseService}) y el adaptador web
 * ({@link com.aleatica.parking.release.ReleaseController}).
 *
 * <p>Una liberacion marca un recurso con asignacion fija como disponible para una fecha
 * concreta: {@code VOLUNTARY} (por el titular, {@code reason} nulo) o
 * {@code ADMINISTRATIVE} (por el {@code ADMIN}, {@code reason} obligatorio). La ventana
 * temporal ({@code release_date >= hoy}) vive en el caso de uso con {@code ClockPort}; la
 * unicidad recurso+fecha la garantiza el indice unico de la BD ({@code UX_releases_space_date},
 * 409, incluida la concurrencia). El listado y la cancelacion propios aplican verificacion
 * de pertenencia (BOLA), no solo RBAC. La cancelacion es un borrado fisico de la fila
 * futura. La auditoria se dispara {@code AFTER_COMMIT} via un puerto de log (consolidacion
 * de {@code audit-retention}, B10). Alimenta {@code availability-calendar} (B7).</p>
 */
package com.aleatica.parking.release;
