package com.aleatica.parking.request.infrastructure;

import com.aleatica.parking.request.domain.RequestStatus;
import com.aleatica.parking.resource.ResourceType;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA de solicitudes (adaptador de salida de infraestructura,
 * change {@code hexagonal-persistence}).
 *
 * <p>Opera con la entidad JPA {@link RequestEntity}. El agregado {@code request} accede a el a
 * traves de {@link RequestPersistenceAdapter} (que mapea a/desde el modelo de dominio); los
 * casos de uso vecinos aun no migrados ({@code availability}, {@code floor-plan}, {@code export},
 * {@code visitor}) lo consultan directamente hasta su propia fase de migracion.</p>
 *
 * <p>Todas las consultas se derivan del nombre del metodo (parametros vinculados, sin
 * concatenacion), eliminando la inyeccion SQL por construccion (OWASP API / security-design §4).
 * La unicidad {@code PENDING} y la de recurso {@code APPROVED} las garantizan los indices unicos
 * filtrados de la BD; las comprobaciones de existencia son la primera capa (UX y mensaje claro),
 * no la red dura frente a concurrencia.</p>
 */
public interface RequestJpaRepository extends JpaRepository<RequestEntity, Long> {

    /**
     * Pagina de las solicitudes de un empleado (listado "mis solicitudes").
     *
     * @param employeeId empleado propietario
     * @param pageable   pagina y orden solicitados
     * @return pagina de solicitudes del empleado
     */
    Page<RequestEntity> findByEmployeeId(Long employeeId, Pageable pageable);

    /**
     * Solicitudes de un empleado (sin paginar), ordenadas por id ascendente.
     *
     * @param employeeId empleado propietario (sujeto de la sesion)
     * @return solicitudes del empleado en orden estable (posiblemente vacia)
     */
    List<RequestEntity> findByEmployeeIdOrderByIdAsc(Long employeeId);

    /**
     * Pagina de las solicitudes de un empleado filtradas por estado.
     *
     * @param employeeId empleado propietario
     * @param status     estado por el que filtrar
     * @param pageable   pagina y orden solicitados
     * @return pagina de solicitudes del empleado en ese estado
     */
    Page<RequestEntity> findByEmployeeIdAndStatus(Long employeeId, RequestStatus status, Pageable pageable);

    /**
     * Pagina de solicitudes en un estado, en orden FIFO por fecha de creacion
     * (listado admin de pendientes).
     *
     * @param status   estado por el que filtrar
     * @param pageable pagina y tamano solicitados
     * @return pagina de solicitudes ordenadas por {@code created_at ASC}
     */
    Page<RequestEntity> findByStatusOrderByCreatedAtAsc(RequestStatus status, Pageable pageable);

    /**
     * Pagina de solicitudes en un estado, en orden de actividad reciente por fecha de creacion
     * descendente (listado admin por estado: aprobadas / rechazadas).
     *
     * @param status   estado por el que filtrar
     * @param pageable pagina y tamano solicitados
     * @return pagina de solicitudes ordenadas por {@code created_at DESC}
     */
    Page<RequestEntity> findByStatusOrderByCreatedAtDesc(RequestStatus status, Pageable pageable);

    /**
     * Pagina de todas las solicitudes (cualquier estado), en orden de actividad reciente por
     * fecha de creacion descendente (listado admin de la pestana "todas").
     *
     * @param pageable pagina y tamano solicitados
     * @return pagina de solicitudes ordenadas por {@code created_at DESC}
     */
    Page<RequestEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Indica si el empleado ya tiene una solicitud en el estado dado para la fecha.
     *
     * @param employeeId    empleado propietario
     * @param requestedDate fecha solicitada
     * @param status        estado a comprobar
     * @return {@code true} si ya existe una solicitud en ese estado para esa fecha
     */
    boolean existsByEmployeeIdAndRequestedDateAndStatus(
            Long employeeId, LocalDate requestedDate, RequestStatus status);

    /**
     * Indica si el empleado ya tiene una solicitud en el estado dado, del tipo de recurso
     * indicado, para la fecha (soporte de la unicidad {@code PENDING} por empleado/tipo/fecha).
     *
     * @param employeeId    empleado propietario
     * @param resourceType  tipo de recurso ({@code PARKING}/{@code DESK})
     * @param requestedDate fecha solicitada
     * @param status        estado a comprobar
     * @return {@code true} si ya existe una solicitud en ese estado, tipo y fecha
     */
    boolean existsByEmployeeIdAndResourceTypeAndRequestedDateAndStatus(
            Long employeeId, ResourceType resourceType, LocalDate requestedDate, RequestStatus status);

    /**
     * Indica si el recurso ya tiene una solicitud en el estado dado para la fecha
     * (soporte de la disponibilidad al aprobar: recurso ya {@code APPROVED} esa fecha).
     *
     * @param resourceId    recurso a comprobar
     * @param resourceType  tipo de recurso (PARKING en el nucleo de parking)
     * @param requestedDate fecha solicitada
     * @param status        estado a comprobar
     * @return {@code true} si ya existe una solicitud en ese estado para ese recurso/fecha
     */
    boolean existsByResourceIdAndResourceTypeAndRequestedDateAndStatus(
            Long resourceId, ResourceType resourceType, LocalDate requestedDate, RequestStatus status);

    /**
     * Solicitudes en un estado cuyo {@code requested_date} cae dentro del intervalo
     * (extremos inclusive).
     *
     * @param status estado por el que filtrar (p. ej. {@code APPROVED})
     * @param start  fecha inicial del intervalo (inclusive)
     * @param end    fecha final del intervalo (inclusive)
     * @return solicitudes del intervalo en ese estado (posiblemente vacia)
     */
    List<RequestEntity> findByStatusAndRequestedDateBetween(
            RequestStatus status, LocalDate start, LocalDate end);

    /**
     * Solicitudes en un estado y de un tipo de recurso cuyo {@code requested_date} cae
     * dentro del intervalo (extremos inclusive).
     *
     * @param status       estado por el que filtrar (p. ej. {@code APPROVED})
     * @param resourceType tipo de recurso ({@code PARKING}/{@code DESK})
     * @param start        fecha inicial del intervalo (inclusive)
     * @param end          fecha final del intervalo (inclusive)
     * @return solicitudes del intervalo en ese estado y tipo (posiblemente vacia)
     */
    List<RequestEntity> findByStatusAndResourceTypeAndRequestedDateBetween(
            RequestStatus status, ResourceType resourceType, LocalDate start, LocalDate end);

    /**
     * Solicitudes de un empleado cuyo {@code requested_date} cae dentro del intervalo
     * (extremos inclusive).
     *
     * @param employeeId empleado solicitante
     * @param start      fecha inicial del intervalo (inclusive)
     * @param end        fecha final del intervalo (inclusive)
     * @return solicitudes propias del intervalo (posiblemente vacia)
     */
    List<RequestEntity> findByEmployeeIdAndRequestedDateBetween(
            Long employeeId, LocalDate start, LocalDate end);

    /**
     * Solicitudes de un empleado y de un tipo de recurso cuyo {@code requested_date} cae
     * dentro del intervalo (extremos inclusive).
     *
     * @param employeeId   empleado solicitante
     * @param resourceType tipo de recurso ({@code PARKING}/{@code DESK})
     * @param start        fecha inicial del intervalo (inclusive)
     * @param end          fecha final del intervalo (inclusive)
     * @return solicitudes propias del intervalo y tipo (posiblemente vacia)
     */
    List<RequestEntity> findByEmployeeIdAndResourceTypeAndRequestedDateBetween(
            Long employeeId, ResourceType resourceType, LocalDate start, LocalDate end);
}
