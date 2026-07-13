package com.aleatica.parking.parkingspace;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Adaptador de salida de persistencia de plazas de parking (Spring Data JPA).
 *
 * <p>Las consultas usan parametros vinculados (sin concatenacion), eliminando la
 * inyeccion SQL por construccion (OWASP API / security-design §4).</p>
 */
public interface ParkingSpaceRepository extends JpaRepository<ParkingSpace, Long> {

    /**
     * Busca una plaza por su etiqueta (clave natural unica).
     *
     * @param label etiqueta a buscar
     * @return la plaza, o {@link Optional#empty()} si no existe
     */
    Optional<ParkingSpace> findByLabel(String label);

    /**
     * Indica si ya existe una plaza con la etiqueta dada.
     *
     * @param label etiqueta a comprobar
     * @return {@code true} si la etiqueta esta en uso
     */
    boolean existsByLabel(String label);

    /**
     * Indica si la etiqueta pertenece a una plaza distinta de la indicada
     * (colision al editar, excluyendo a la propia plaza).
     *
     * @param label etiqueta a comprobar
     * @param id    id de la plaza que se esta editando
     * @return {@code true} si la etiqueta la usa otra plaza
     */
    boolean existsByLabelAndIdNot(String label, Long id);

    /**
     * Indica si ya existe una plaza con el numero dado.
     *
     * @param number numero a comprobar
     * @return {@code true} si el numero esta en uso
     */
    boolean existsByNumber(Integer number);

    /**
     * Indica si el numero pertenece a una plaza distinta de la indicada
     * (colision al editar, excluyendo a la propia plaza).
     *
     * @param number numero a comprobar
     * @param id     id de la plaza que se esta editando
     * @return {@code true} si el numero lo usa otra plaza
     */
    boolean existsByNumberAndIdNot(Integer number, Long id);

    /**
     * Recupera las plazas activas ordenadas por id ascendente (para la
     * configuracion masiva del total, que ajusta el subconjunto activo).
     *
     * @return las plazas activas ordenadas por id
     */
    List<ParkingSpace> findByActiveTrueOrderByIdAsc();

    /**
     * Busqueda paginada de plazas con filtro opcional por estado y por planta.
     *
     * <p>Cuando {@code active} es {@code null} el filtro por estado no se aplica.
     * Cuando {@code minNumber} es {@code null} el filtro por planta no se aplica;
     * en otro caso se restringe al rango {@code [minNumber, maxNumber]} (la planta
     * {@code f} corresponde a {@code [f*1000, f*1000+999]}). Los parametros van
     * vinculados (sin concatenacion, OWASP A03).</p>
     *
     * @param active    filtro por estado; {@code null} para no filtrar
     * @param minNumber limite inferior del rango de planta; {@code null} para no filtrar
     * @param maxNumber limite superior del rango de planta
     * @param pageable  pagina y orden solicitados
     * @return pagina de plazas que cumplen el filtro
     */
    @Query("SELECT p FROM ParkingSpace p WHERE (:active IS NULL OR p.active = :active) "
            + "AND (:minNumber IS NULL OR p.number BETWEEN :minNumber AND :maxNumber)")
    Page<ParkingSpace> search(@Param("active") Boolean active,
            @Param("minNumber") Integer minNumber,
            @Param("maxNumber") Integer maxNumber,
            Pageable pageable);
}
