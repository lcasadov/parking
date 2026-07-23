package com.aleatica.parking.release.domain;

import com.aleatica.parking.resource.ResourceType;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Puerto de salida de persistencia del agregado {@code release} (arquitectura hexagonal,
 * change {@code hexagonal-persistence}).
 *
 * <p>Declara <strong>solo lo que necesita {@code ReleaseService}</strong> (design §D3) y opera
 * exclusivamente con el modelo de dominio {@link Release}: nunca expone la entidad JPA ni tipos
 * de Spring Data Repository. La paginacion se mantiene con {@link Page}/{@link Pageable} de
 * {@code org.springframework.data.domain} (value types estables, design §D4), la unica
 * dependencia de Spring admitida en el dominio.</p>
 *
 * <p>Los metodos derivados de Spring Data viven en la interfaz {@code ReleaseJpaRepository} dentro
 * del adaptador de infraestructura, que traduce entidad&harr;dominio; este puerto es agnostico de
 * la tecnologia de persistencia.</p>
 */
public interface ReleaseRepositoryPort {

    /**
     * Busca una liberacion por su identificador.
     *
     * @param id identificador de la liberacion
     * @return la liberacion de dominio, o vacio si no existe
     */
    Optional<Release> findById(Long id);

    /**
     * Persiste una liberacion y fuerza el volcado inmediato a la BD (flush), de modo que la
     * violacion del indice unico recurso+fecha ({@code UX_releases_space_date}) aflore como
     * {@code DataIntegrityViolationException} dentro del caso de uso y se traduzca a 409
     * (design §D5: la frontera de concurrencia no cambia).
     *
     * @param release liberacion de dominio a persistir
     * @return la liberacion persistida (con identificador asignado en el alta)
     */
    Release saveAndFlush(Release release);

    /**
     * Elimina fisicamente una liberacion (la cancelacion es un borrado de la fila futura:
     * {@code releases} es historico purgable).
     *
     * @param release liberacion de dominio a eliminar
     */
    void delete(Release release);

    /**
     * Indica si el recurso ya tiene una liberacion para la fecha (soporte de la unicidad
     * recurso+fecha: comprobacion previa antes del alta).
     *
     * @param resourceId   recurso a comprobar
     * @param resourceType tipo de recurso (PARKING en el nucleo de parking)
     * @param releaseDate  fecha liberada
     * @return {@code true} si ya existe una liberacion para ese recurso y fecha
     */
    boolean existsByResourceIdAndResourceTypeAndReleaseDate(
            Long resourceId, ResourceType resourceType, LocalDate releaseDate);

    /**
     * Pagina de las liberaciones de un empleado (listado "mis liberaciones").
     *
     * @param employeeId empleado propietario
     * @param pageable   pagina y orden solicitados
     * @return pagina de liberaciones de dominio del empleado
     */
    Page<Release> findByEmployeeId(Long employeeId, Pageable pageable);

    /**
     * Pagina de las liberaciones de un tipo ejecutadas por un actor concreto, en orden de
     * actividad reciente: historial de "mis liberaciones administrativas" para
     * {@code ADMIN}/{@code AGENCIA} (change {@code restructure-admin-workflows}, capability
     * {@code releases}).
     *
     * @param releasedById empleado (ADMIN/AGENCIA) que ejecuto la liberacion
     * @param type         tipo de liberacion ({@code ADMINISTRATIVE} para este historial)
     * @param pageable     pagina y orden solicitados
     * @return pagina de liberaciones de dominio ejecutadas por el actor
     */
    Page<Release> findByReleasedByIdAndType(Long releasedById, ReleaseType type, Pageable pageable);
}
