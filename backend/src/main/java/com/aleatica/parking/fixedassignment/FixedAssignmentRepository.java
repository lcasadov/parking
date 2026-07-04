package com.aleatica.parking.fixedassignment;

import java.util.Collection;
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

    /**
     * Asignaciones fijas activas de un empleado para un dia de la semana (soporte de la
     * resolucion de plaza al liberar: init-releases).
     *
     * <p>Una liberacion opera sobre una asignacion fija activa del recurso para el dia
     * de la semana de la fecha a liberar. Si el empleado tiene mas de una asignacion
     * activa ese dia, la resolucion implicita de plaza es ambigua (409); si no tiene
     * ninguna, no hay recurso fijo que liberar (409).</p>
     *
     * @param employeeId empleado titular
     * @param dayOfWeek  dia de la semana (1=Lunes … 7=Domingo)
     * @return lista de asignaciones activas de ese empleado y dia (posiblemente vacia)
     */
    List<FixedAssignment> findByEmployeeIdAndDayOfWeekAndActiveTrue(Long employeeId, Integer dayOfWeek);

    /**
     * Indica si una plaza tiene una asignacion fija activa para un dia de la semana.
     *
     * <p>Soporte de la comprobacion de disponibilidad al aprobar una solicitud
     * (init-requests): una plaza con asignacion fija activa ese dia de la semana no
     * esta disponible para una solicitud puntual esa fecha. Logica temporal que
     * consolidara la capability {@code availability-calendar} (B7).</p>
     *
     * @param parkingSpaceId plaza a comprobar
     * @param dayOfWeek      dia de la semana (1=Lunes … 7=Domingo)
     * @return {@code true} si existe una asignacion fija activa para esa plaza y dia
     */
    boolean existsByParkingSpaceIdAndDayOfWeekAndActiveTrue(Long parkingSpaceId, Integer dayOfWeek);

    /**
     * Asignaciones fijas activas de un conjunto de plazas (carga por bloque para el
     * calendario semanal: una sola consulta para todas las plazas, evitando N+1).
     *
     * <p>Soporte de la capability {@code availability-calendar}: el ensamblado del
     * calendario cruza en memoria estas asignaciones con las liberaciones, solicitudes
     * aprobadas y reservas del rango, sin una consulta por celda.</p>
     *
     * @param parkingSpaceIds plazas a cargar; si esta vacio la consulta no devuelve filas
     * @return lista de asignaciones activas de esas plazas (posiblemente vacia)
     */
    List<FixedAssignment> findByParkingSpaceIdInAndActiveTrue(Collection<Long> parkingSpaceIds);
}
