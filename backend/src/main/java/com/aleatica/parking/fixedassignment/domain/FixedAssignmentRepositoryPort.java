package com.aleatica.parking.fixedassignment.domain;

import com.aleatica.parking.resource.ResourceType;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Puerto de salida de persistencia del agregado {@code fixedassignment} (arquitectura
 * hexagonal, change {@code hexagonal-persistence}).
 *
 * <p>Declara <strong>solo lo que necesita {@code FixedAssignmentService}</strong> (design §D3)
 * y opera exclusivamente con el modelo de dominio {@link FixedAssignment}: nunca expone la
 * entidad JPA ni tipos de Spring Data Repository. La paginacion se mantiene con
 * {@link Page}/{@link Pageable} de {@code org.springframework.data.domain} (value types
 * estables, design §D4), la unica dependencia de Spring admitida en el dominio.</p>
 *
 * <p>Los metodos derivados de Spring Data y las consultas que consumen otros agregados aun no
 * migrados ({@code availability}, {@code floor-plan}, {@code visitor}, {@code release}) viven
 * en la interfaz {@code FixedAssignmentJpaRepository} dentro del adaptador de infraestructura,
 * que traduce entidad&harr;dominio; este puerto es agnostico de la tecnologia de persistencia.</p>
 */
public interface FixedAssignmentRepositoryPort {

    /**
     * Pagina de todas las asignaciones fijas activas (listado del {@code ADMIN}).
     *
     * @param pageable pagina y orden solicitados
     * @return pagina de asignaciones activas de dominio
     */
    Page<FixedAssignment> findByActiveTrue(Pageable pageable);

    /**
     * Asignaciones fijas activas de un empleado, ordenadas por dia de la semana (consulta
     * propia y base de la revocacion sin tipo).
     *
     * @param employeeId empleado titular
     * @return lista de asignaciones activas (posiblemente vacia)
     */
    List<FixedAssignment> findByEmployeeIdAndActiveTrueOrderByDayOfWeekAsc(Long employeeId);

    /**
     * Asignaciones fijas activas de un empleado para un tipo de recurso, ordenadas por dia de
     * la semana (base de la revocacion acotada por tipo).
     *
     * @param employeeId   empleado titular
     * @param resourceType tipo de recurso ({@code PARKING}/{@code DESK})
     * @return lista de asignaciones activas de ese empleado y tipo (posiblemente vacia)
     */
    List<FixedAssignment> findByEmployeeIdAndResourceTypeAndActiveTrueOrderByDayOfWeekAsc(
            Long employeeId, ResourceType resourceType);

    /**
     * Asignaciones fijas activas de un empleado para un recurso concreto (subconjunto que
     * gestiona el {@code PUT setEmployeeFixedAssignments}: el conjunto semanal por empleado y
     * recurso).
     *
     * @param employeeId   empleado titular
     * @param resourceId   recurso asignado
     * @param resourceType tipo de recurso (PARKING en el nucleo de parking)
     * @return lista de asignaciones activas de ese empleado y recurso
     */
    List<FixedAssignment> findByEmployeeIdAndResourceIdAndResourceTypeAndActiveTrue(
            Long employeeId, Long resourceId, ResourceType resourceType);

    /**
     * Persiste (alta o merge) un conjunto de asignaciones sin forzar volcado inmediato.
     *
     * @param assignments asignaciones de dominio a persistir
     * @return las asignaciones persistidas (con identificador asignado en las altas)
     */
    List<FixedAssignment> saveAll(List<FixedAssignment> assignments);

    /**
     * Persiste un conjunto de asignaciones y fuerza el volcado inmediato a la BD (flush), de
     * modo que la violacion de los indices unicos filtrados plaza/dia y empleado/dia
     * ({@code UX_fixed_assignments_space_day_active} / {@code UX_fixed_assignments_employee_day_active})
     * aflore como {@code DataIntegrityViolationException} dentro del caso de uso y se traduzca a
     * 409 (design §D5: la frontera de concurrencia no cambia). Aplicar las revocaciones antes de
     * las altas con flush intermedio evita chocar con el indice filtrado empleado/dia al
     * reasignar un dia dentro del mismo recurso.
     *
     * @param assignments asignaciones de dominio a persistir y volcar
     * @return las asignaciones persistidas (con identificador asignado en las altas)
     */
    List<FixedAssignment> saveAllAndFlush(List<FixedAssignment> assignments);
}
