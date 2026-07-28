package com.aleatica.parking.fixedassignment.infrastructure;

import com.aleatica.parking.resource.ResourceType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA de asignaciones fijas (adaptador de salida de infraestructura,
 * change {@code hexagonal-persistence}).
 *
 * <p>Opera con la entidad JPA {@link FixedAssignmentEntity}. El agregado
 * {@code fixedassignment} accede a el a traves de {@link FixedAssignmentPersistenceAdapter}
 * (que mapea a/desde el modelo de dominio); los casos de uso vecinos aun no migrados
 * ({@code availability}, {@code floor-plan}, {@code visitor}, {@code release}) lo consultan
 * directamente hasta su propia fase de migracion.</p>
 *
 * <p>Todas las consultas se derivan del nombre del metodo (parametros vinculados, sin
 * concatenacion), eliminando la inyeccion SQL por construccion (OWASP API / security-design §4).
 * El filtro {@code ActiveTrue} restringe siempre a las filas vigentes, coherente con los indices
 * unicos filtrados de la tabla.</p>
 */
public interface FixedAssignmentJpaRepository extends JpaRepository<FixedAssignmentEntity, Long> {

    /**
     * Asignaciones fijas activas de un empleado, ordenadas por dia de la semana.
     *
     * @param employeeId empleado titular
     * @return lista de asignaciones activas (posiblemente vacia)
     */
    List<FixedAssignmentEntity> findByEmployeeIdAndActiveTrueOrderByDayOfWeekAsc(Long employeeId);

    /**
     * Asignaciones fijas activas de un empleado para un tipo de recurso, ordenadas por dia
     * de la semana (base de la vista "Mi Semana" por tipo, evitando que un recurso de otro
     * tipo con el mismo {@code resource_id} contamine la vista).
     *
     * @param employeeId   empleado titular
     * @param resourceType tipo de recurso ({@code PARKING}/{@code DESK})
     * @return lista de asignaciones activas de ese empleado y tipo (posiblemente vacia)
     */
    List<FixedAssignmentEntity> findByEmployeeIdAndResourceTypeAndActiveTrueOrderByDayOfWeekAsc(
            Long employeeId, ResourceType resourceType);

    /**
     * Asignaciones fijas activas de un empleado para una plaza concreta (subconjunto
     * que gestiona el {@code PUT setEmployeeFixedAssignments}).
     *
     * @param employeeId   empleado titular
     * @param resourceId   recurso asignado
     * @param resourceType tipo de recurso (PARKING en el nucleo de parking)
     * @return lista de asignaciones activas de ese empleado y recurso
     */
    List<FixedAssignmentEntity> findByEmployeeIdAndResourceIdAndResourceTypeAndActiveTrue(
            Long employeeId, Long resourceId, ResourceType resourceType);

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
    Page<FixedAssignmentEntity> findByActiveTrue(Pageable pageable);

    /**
     * Asignaciones fijas activas de un empleado para un dia de la semana (soporte de la
     * resolucion de plaza al liberar: init-releases).
     *
     * @param employeeId empleado titular
     * @param dayOfWeek  dia de la semana (1=Lunes … 7=Domingo)
     * @return lista de asignaciones activas de ese empleado y dia (posiblemente vacia)
     */
    List<FixedAssignmentEntity> findByEmployeeIdAndDayOfWeekAndActiveTrue(
            Long employeeId, Integer dayOfWeek);

    /**
     * Asignaciones fijas activas de un empleado para un tipo de recurso y un dia de la
     * semana (resolucion del recurso a liberar acotada al tipo: un puesto no resuelve una
     * liberacion de plaza ni viceversa).
     *
     * @param employeeId   empleado titular
     * @param resourceType tipo de recurso ({@code PARKING}/{@code DESK})
     * @param dayOfWeek    dia de la semana (1=Lunes … 7=Domingo)
     * @return lista de asignaciones activas de ese empleado, tipo y dia (posiblemente vacia)
     */
    List<FixedAssignmentEntity> findByEmployeeIdAndResourceTypeAndDayOfWeekAndActiveTrue(
            Long employeeId, ResourceType resourceType, Integer dayOfWeek);

    /**
     * Indica si una plaza tiene una asignacion fija activa para un dia de la semana (soporte
     * de la comprobacion de disponibilidad al aprobar una solicitud y al reservar visitante).
     *
     * @param resourceId   recurso a comprobar
     * @param resourceType tipo de recurso (PARKING en el nucleo de parking)
     * @param dayOfWeek    dia de la semana (1=Lunes … 7=Domingo)
     * @return {@code true} si existe una asignacion fija activa para ese recurso y dia
     */
    boolean existsByResourceIdAndResourceTypeAndDayOfWeekAndActiveTrue(
            Long resourceId, ResourceType resourceType, Integer dayOfWeek);

    /**
     * Asignaciones fijas activas de un conjunto de recursos (carga por bloque para el
     * calendario/plano semanal: una sola consulta para todos los recursos, evitando N+1).
     *
     * @param resourceIds  recursos a cargar; si esta vacio la consulta no devuelve filas
     * @param resourceType tipo de recurso (PARKING/DESK)
     * @return lista de asignaciones activas de esos recursos (posiblemente vacia)
     */
    List<FixedAssignmentEntity> findByResourceIdInAndResourceTypeAndActiveTrue(
            Collection<Long> resourceIds, ResourceType resourceType);

    /**
     * Cuenta las asignaciones fijas activas de un recurso (cualquier dia de la semana). Una
     * asignacion fija activa implica ocupacion recurrente futura, por lo que bloquea la
     * desactivacion silenciosa del recurso (change {@code admin-improvements}, tarea 16).
     *
     * @param resourceId   recurso a comprobar
     * @param resourceType tipo de recurso (PARKING/DESK)
     * @return numero de asignaciones fijas activas del recurso
     */
    long countByResourceIdAndResourceTypeAndActiveTrue(Long resourceId, ResourceType resourceType);
}
