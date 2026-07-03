package com.aleatica.parking.fixedassignment;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Adaptador de salida de persistencia de asignaciones fijas (Spring Data JPA).
 *
 * <p>Todas las consultas se derivan del nombre del metodo (parametros vinculados,
 * sin concatenacion), eliminando la inyeccion SQL por construccion (OWASP API /
 * security-design §4). El filtro {@code ActiveTrue} restringe siempre a las filas
 * vigentes, coherente con los indices unicos filtrados de la tabla.</p>
 */
public interface FixedAssignmentRepository extends JpaRepository<FixedAssignment, Long> {

    /**
     * Asignaciones fijas activas de un empleado, ordenadas por dia de la semana.
     *
     * @param employeeId empleado titular
     * @return lista de asignaciones activas (posiblemente vacia)
     */
    List<FixedAssignment> findByEmployeeIdAndActiveTrueOrderByDayOfWeekAsc(Long employeeId);

    /**
     * Asignaciones fijas activas de un empleado para una plaza concreta (subconjunto
     * que gestiona el {@code PUT setEmployeeFixedAssignments}).
     *
     * @param employeeId     empleado titular
     * @param parkingSpaceId plaza asignada
     * @return lista de asignaciones activas de ese empleado y plaza
     */
    List<FixedAssignment> findByEmployeeIdAndParkingSpaceIdAndActiveTrue(
            Long employeeId, Long parkingSpaceId);

    /**
     * Indica si el empleado tiene al menos una asignacion fija activa (soporte de la
     * revocacion: 404 si no hay ninguna que revocar).
     *
     * @param employeeId empleado titular
     * @return {@code true} si existe alguna asignacion activa
     */
    boolean existsByEmployeeIdAndActiveTrue(Long employeeId);

    /**
     * Pagina de todas las asignaciones fijas activas (listado del {@code ADMIN}).
     *
     * @param pageable pagina y orden solicitados
     * @return pagina de asignaciones activas
     */
    Page<FixedAssignment> findByActiveTrue(Pageable pageable);
}
