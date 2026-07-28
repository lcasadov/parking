package com.aleatica.parking.release.infrastructure;

import com.aleatica.parking.release.domain.ReleaseType;
import com.aleatica.parking.resource.ResourceType;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA de liberaciones (adaptador de salida de infraestructura,
 * change {@code hexagonal-persistence}).
 *
 * <p>Opera con la entidad JPA {@link ReleaseEntity}. El agregado {@code release} accede a el a
 * traves de {@link ReleasePersistenceAdapter} (que mapea a/desde el modelo de dominio); los casos
 * de uso vecinos aun no migrados ({@code availability}, {@code floor-plan}, {@code visitor}) lo
 * consultan directamente hasta su propia fase de migracion.</p>
 *
 * <p>Todas las consultas se derivan del nombre del metodo (parametros vinculados, sin
 * concatenacion), eliminando la inyeccion SQL por construccion (OWASP API / security-design §4).
 * La unicidad recurso+fecha la garantiza el indice unico de la BD ({@code UX_releases_space_date});
 * {@link #existsByResourceIdAndResourceTypeAndReleaseDate} es la primera capa (UX y mensaje claro),
 * no la red dura frente a concurrencia.</p>
 */
public interface ReleaseJpaRepository extends JpaRepository<ReleaseEntity, Long> {

    /**
     * Pagina de las liberaciones de un empleado (listado "mis liberaciones").
     *
     * @param employeeId empleado propietario
     * @param pageable   pagina y orden solicitados
     * @return pagina de liberaciones del empleado
     */
    Page<ReleaseEntity> findByEmployeeId(Long employeeId, Pageable pageable);

    /**
     * Indica si el recurso ya tiene una liberacion para la fecha (soporte de la
     * unicidad recurso+fecha: comprobacion previa antes del alta).
     *
     * @param resourceId   recurso a comprobar
     * @param resourceType tipo de recurso (PARKING en el nucleo de parking)
     * @param releaseDate  fecha liberada
     * @return {@code true} si ya existe una liberacion para ese recurso y fecha
     */
    boolean existsByResourceIdAndResourceTypeAndReleaseDate(
            Long resourceId, ResourceType resourceType, LocalDate releaseDate);

    /**
     * Liberaciones cuyo {@code release_date} cae dentro del intervalo (extremos inclusive).
     *
     * <p>Carga por rango para el calendario semanal admin ({@code availability-calendar}):
     * una sola consulta para toda la semana, evitando N+1 al pintar los siete dias.</p>
     *
     * @param start fecha inicial del intervalo (inclusive)
     * @param end   fecha final del intervalo (inclusive)
     * @return liberaciones del intervalo (posiblemente vacia)
     */
    List<ReleaseEntity> findByReleaseDateBetween(LocalDate start, LocalDate end);

    /**
     * Liberaciones de un tipo de recurso cuyo {@code release_date} cae dentro del intervalo
     * (extremos inclusive).
     *
     * <p>Variante filtrada por {@code resource_type} de {@link #findByReleaseDateBetween}:
     * la disponibilidad y el calendario cargan solo las liberaciones del tipo consultado,
     * evitando que una liberacion {@code DESK} con el mismo {@code resource_id} que una
     * plaza marque erroneamente esa plaza como liberada (los identificadores no son unicos
     * entre tablas de recurso).</p>
     *
     * @param resourceType tipo de recurso ({@code PARKING}/{@code DESK})
     * @param start        fecha inicial del intervalo (inclusive)
     * @param end          fecha final del intervalo (inclusive)
     * @return liberaciones del intervalo y tipo (posiblemente vacia)
     */
    List<ReleaseEntity> findByResourceTypeAndReleaseDateBetween(
            ResourceType resourceType, LocalDate start, LocalDate end);

    /**
     * Liberaciones de un empleado cuyo {@code release_date} cae dentro del intervalo
     * (extremos inclusive).
     *
     * <p>Carga por rango para la vista "Mi Semana" ({@code availability-calendar}):
     * restringe a las liberaciones propias del solicitante en una sola consulta.</p>
     *
     * @param employeeId empleado propietario
     * @param start      fecha inicial del intervalo (inclusive)
     * @param end        fecha final del intervalo (inclusive)
     * @return liberaciones propias del intervalo (posiblemente vacia)
     */
    List<ReleaseEntity> findByEmployeeIdAndReleaseDateBetween(
            Long employeeId, LocalDate start, LocalDate end);

    /**
     * Liberaciones de un empleado y de un tipo de recurso cuyo {@code release_date} cae
     * dentro del intervalo (extremos inclusive): variante filtrada por {@code resource_type}
     * de {@link #findByEmployeeIdAndReleaseDateBetween} para la vista "Mi Semana" por tipo.
     *
     * @param employeeId   empleado propietario
     * @param resourceType tipo de recurso ({@code PARKING}/{@code DESK})
     * @param start        fecha inicial del intervalo (inclusive)
     * @param end          fecha final del intervalo (inclusive)
     * @return liberaciones propias del intervalo y tipo (posiblemente vacia)
     */
    List<ReleaseEntity> findByEmployeeIdAndResourceTypeAndReleaseDateBetween(
            Long employeeId, ResourceType resourceType, LocalDate start, LocalDate end);

    /**
     * Pagina de las liberaciones de un tipo ejecutadas por un actor concreto, en orden de
     * actividad reciente ({@code created_at DESC}): historial de "mis liberaciones
     * administrativas" para {@code ADMIN}/{@code AGENCIA} (change
     * {@code restructure-admin-workflows}, capability {@code releases}, design §D5).
     *
     * @param releasedById empleado (ADMIN/AGENCIA) que ejecuto la liberacion
     * @param type         tipo de liberacion ({@code ADMINISTRATIVE} para este historial)
     * @param pageable     pagina y tamano solicitados
     * @return pagina de liberaciones ejecutadas por el actor
     */
    Page<ReleaseEntity> findByReleasedByIdAndTypeOrderByCreatedAtDesc(
            Long releasedById, ReleaseType type, Pageable pageable);
}
