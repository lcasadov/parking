package com.aleatica.parking.employee;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Adaptador de salida de persistencia de empleados (Spring Data JPA).
 *
 * <p>Las consultas usan parametros vinculados (sin concatenacion), eliminando la
 * inyeccion SQL por construccion (OWASP API / security-design §4).</p>
 */
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    /**
     * Busca un empleado por su login (clave natural unica).
     *
     * @param login login a buscar
     * @return el empleado, o {@link Optional#empty()} si no existe
     */
    Optional<Employee> findByLogin(String login);

    /**
     * Indica si ya existe un empleado con el login dado.
     *
     * @param login login a comprobar
     * @return {@code true} si el login esta en uso
     */
    boolean existsByLogin(String login);

    /**
     * Indica si ya existe un empleado con el email dado.
     *
     * @param email email a comprobar
     * @return {@code true} si el email esta en uso
     */
    boolean existsByEmail(String email);

    /**
     * Indica si el email pertenece a un empleado distinto del indicado
     * (colision al editar, excluyendo al propio empleado).
     *
     * @param email email a comprobar
     * @param id    id del empleado que se esta editando
     * @return {@code true} si el email lo usa otro empleado
     */
    boolean existsByEmailAndIdNot(String email, Long id);

    /**
     * Devuelve los empleados con un rol dado que estan activos (baja logica {@code active
     * = true}). Lo usa la capability {@code notifications} para resolver los destinatarios
     * del email de "nueva solicitud" (todos los {@code ADMIN} activos), excluyendo por
     * construccion a los administradores inactivos.
     *
     * @param role rol a filtrar
     * @return empleados activos con ese rol (posiblemente vacia)
     */
    List<Employee> findByRoleAndActiveTrue(Role role);

    /**
     * Busqueda paginada de empleados por texto libre y estado.
     *
     * <p>El texto {@code q} se compara (case-insensitive) contra nombre,
     * apellidos, login y email. Cuando {@code q} o {@code active} son
     * {@code null}, ese filtro no se aplica. Todos los parametros van vinculados.</p>
     *
     * @param q        texto de busqueda libre; {@code null} para no filtrar
     * @param active   filtro por estado de baja logica; {@code null} para no filtrar
     * @param pageable pagina y orden solicitados
     * @return pagina de empleados que cumplen los filtros
     */
    @Query("""
            SELECT e FROM Employee e
            WHERE (:q IS NULL
                   OR LOWER(e.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(e.lastName)  LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(e.login)     LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(e.email)     LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:active IS NULL OR e.active = :active)
            """)
    Page<Employee> search(
            @Param("q") String q, @Param("active") Boolean active, Pageable pageable);
}
