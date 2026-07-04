package com.aleatica.parking.desk;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Adaptador de salida de persistencia de puestos de oficina (Spring Data JPA).
 *
 * <p>Las consultas usan parametros vinculados (sin concatenacion), eliminando la
 * inyeccion SQL por construccion (OWASP API / security-design §4). La unicidad de
 * {@code number} la garantiza el indice unico {@code UX_desks_number} frente a
 * concurrencia; las comprobaciones de existencia son la primera capa (UX y mensaje
 * claro), no la red dura.</p>
 */
public interface DeskRepository extends JpaRepository<Desk, Long> {

    /**
     * Busca un puesto por su numero (clave natural unica).
     *
     * @param number numero a buscar
     * @return el puesto, o {@link Optional#empty()} si no existe
     */
    Optional<Desk> findByNumber(Integer number);

    /**
     * Indica si ya existe un puesto con el numero dado.
     *
     * @param number numero a comprobar
     * @return {@code true} si el numero esta en uso
     */
    boolean existsByNumber(Integer number);

    /**
     * Recupera los puestos activos ordenados por numero ascendente (base del calculo
     * de disponibilidad de puesto).
     *
     * @return los puestos activos ordenados por numero
     */
    List<Desk> findByActiveTrueOrderByNumberAsc();

    /**
     * Busqueda paginada de puestos con filtro opcional por estado.
     *
     * <p>Cuando {@code active} es {@code null} el filtro no se aplica y se devuelven
     * activos e inactivos. El parametro va vinculado.</p>
     *
     * @param active   filtro por estado; {@code null} para no filtrar
     * @param pageable pagina y orden solicitados
     * @return pagina de puestos que cumplen el filtro
     */
    @Query("SELECT d FROM Desk d WHERE (:active IS NULL OR d.active = :active)")
    Page<Desk> search(@Param("active") Boolean active, Pageable pageable);
}
