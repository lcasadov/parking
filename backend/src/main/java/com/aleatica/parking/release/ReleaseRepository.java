package com.aleatica.parking.release;

import com.aleatica.parking.resource.ResourceType;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Adaptador de salida de persistencia de liberaciones (Spring Data JPA).
 *
 * <p>Todas las consultas se derivan del nombre del metodo (parametros vinculados,
 * sin concatenacion), eliminando la inyeccion SQL por construccion (OWASP API /
 * security-design §4). La unicidad recurso+fecha la garantiza el indice unico de la BD
 * ({@code UX_releases_space_date}); {@link #existsByResourceIdAndResourceTypeAndReleaseDate}
 * es la primera capa (UX y mensaje claro), no la red dura frente a concurrencia.</p>
 */
public interface ReleaseRepository extends JpaRepository<Release, Long> {

    /**
     * Pagina de las liberaciones de un empleado (listado "mis liberaciones").
     *
     * @param employeeId empleado propietario
     * @param pageable   pagina y orden solicitados
     * @return pagina de liberaciones del empleado
     */
    Page<Release> findByEmployeeId(Long employeeId, Pageable pageable);

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
    List<Release> findByReleaseDateBetween(LocalDate start, LocalDate end);

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
    List<Release> findByEmployeeIdAndReleaseDateBetween(Long employeeId, LocalDate start, LocalDate end);
}
