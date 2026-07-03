package com.aleatica.parking.employee;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
