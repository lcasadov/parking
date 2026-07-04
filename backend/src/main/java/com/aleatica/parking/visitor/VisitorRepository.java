package com.aleatica.parking.visitor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Adaptador de salida de persistencia de fichas de visitante (Spring Data JPA).
 *
 * <p>Las consultas usan parametros vinculados (sin concatenacion), eliminando la
 * inyeccion SQL por construccion (OWASP API / security-design §4). La unicidad de
 * {@code nationalId} la garantiza el indice unico de la BD ({@code UX_visitors_national_id});
 * {@link #existsByNationalId} y {@link #existsByNationalIdAndIdNot} son la primera capa
 * (UX y mensaje claro), no la red dura frente a concurrencia.</p>
 */
public interface VisitorRepository extends JpaRepository<Visitor, Long> {

    /**
     * Indica si ya existe una ficha con el {@code nationalId} dado (soporte del alta).
     *
     * @param nationalId documento de identidad a comprobar
     * @return {@code true} si el {@code nationalId} esta en uso
     */
    boolean existsByNationalId(String nationalId);

    /**
     * Indica si el {@code nationalId} pertenece a una ficha distinta de la indicada
     * (colision al editar, excluyendo a la propia ficha).
     *
     * @param nationalId documento de identidad a comprobar
     * @param id         id de la ficha que se esta editando
     * @return {@code true} si el {@code nationalId} lo usa otra ficha
     */
    boolean existsByNationalIdAndIdNot(String nationalId, Long id);

    /**
     * Busqueda paginada de fichas por texto libre. El texto {@code q} se compara
     * (case-insensitive) contra {@code nationalId}, {@code firstName}, {@code lastName}
     * y {@code licensePlate}; cuando {@code q} es {@code null} el filtro no se aplica.
     * Cubre el reuso de ficha por cualquiera de esos campos (spec §Casos limite).
     *
     * @param q        texto de busqueda libre; {@code null} para no filtrar
     * @param pageable pagina y orden solicitados
     * @return pagina de fichas que cumplen el filtro
     */
    @Query("""
            SELECT v FROM Visitor v
            WHERE (:q IS NULL
                   OR LOWER(v.nationalId)   LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(v.firstName)    LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(v.lastName)     LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(v.licensePlate) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<Visitor> search(@Param("q") String q, Pageable pageable);
}
