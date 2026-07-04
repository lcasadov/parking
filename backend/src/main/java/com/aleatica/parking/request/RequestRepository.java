package com.aleatica.parking.request;

import com.aleatica.parking.resource.ResourceType;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Adaptador de salida de persistencia de solicitudes (Spring Data JPA).
 *
 * <p>Todas las consultas se derivan del nombre del metodo (parametros vinculados,
 * sin concatenacion), eliminando la inyeccion SQL por construccion (OWASP API /
 * security-design §4). La unicidad {@code PENDING} por empleado/fecha y la de plaza
 * {@code APPROVED} por fecha las garantizan los indices unicos filtrados de la BD;
 * las comprobaciones de existencia de este repositorio son la primera capa (UX y
 * mensaje claro), no la red dura frente a concurrencia.</p>
 */
public interface RequestRepository extends JpaRepository<Request, Long> {

    /**
     * Pagina de las solicitudes de un empleado (listado "mis solicitudes").
     *
     * @param employeeId empleado propietario
     * @param pageable   pagina y orden solicitados
     * @return pagina de solicitudes del empleado
     */
    Page<Request> findByEmployeeId(Long employeeId, Pageable pageable);

    /**
     * Solicitudes de un empleado (sin paginar), ordenadas por id ascendente.
     *
     * <p>Origen de la exportacion "mis solicitudes" ({@code exportMyRequests}): restringe por
     * construccion al sujeto de la sesion (comprobacion de objeto / BOLA), de modo que un
     * empleado nunca exporte solicitudes ajenas.</p>
     *
     * @param employeeId empleado propietario (sujeto de la sesion)
     * @return solicitudes del empleado en orden estable (posiblemente vacia)
     */
    List<Request> findByEmployeeIdOrderByIdAsc(Long employeeId);

    /**
     * Pagina de las solicitudes de un empleado filtradas por estado.
     *
     * @param employeeId empleado propietario
     * @param status     estado por el que filtrar
     * @param pageable   pagina y orden solicitados
     * @return pagina de solicitudes del empleado en ese estado
     */
    Page<Request> findByEmployeeIdAndStatus(Long employeeId, RequestStatus status, Pageable pageable);

    /**
     * Pagina de solicitudes en un estado, en orden FIFO por fecha de creacion
     * (listado admin de pendientes).
     *
     * @param status   estado por el que filtrar
     * @param pageable pagina y tamano solicitados
     * @return pagina de solicitudes ordenadas por {@code created_at ASC}
     */
    Page<Request> findByStatusOrderByCreatedAtAsc(RequestStatus status, Pageable pageable);

    /**
     * Indica si el empleado ya tiene una solicitud en el estado dado para la fecha
     * (soporte de la unicidad {@code PENDING}: comprobacion previa antes del alta).
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
     * indicado, para la fecha (soporte de la unicidad {@code PENDING} por
     * empleado/tipo/fecha: un empleado puede tener una solicitud de plaza y otra de puesto
     * pendientes la misma fecha, pero no dos del mismo tipo).
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
     * Indica si la plaza ya tiene una solicitud en el estado dado para la fecha
     * (soporte de la disponibilidad al aprobar: plaza ya {@code APPROVED} esa fecha).
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
     * <p>Carga por rango para el calendario semanal admin ({@code availability-calendar}):
     * una sola consulta para todas las solicitudes {@code APPROVED} de la semana, evitando
     * N+1 al pintar los siete dias.</p>
     *
     * @param status estado por el que filtrar (p. ej. {@code APPROVED})
     * @param start  fecha inicial del intervalo (inclusive)
     * @param end    fecha final del intervalo (inclusive)
     * @return solicitudes del intervalo en ese estado (posiblemente vacia)
     */
    List<Request> findByStatusAndRequestedDateBetween(
            RequestStatus status, LocalDate start, LocalDate end);

    /**
     * Solicitudes en un estado y de un tipo de recurso cuyo {@code requested_date} cae
     * dentro del intervalo (extremos inclusive).
     *
     * <p>Variante filtrada por {@code resource_type} de {@link #findByStatusAndRequestedDateBetween}:
     * la disponibilidad y el calendario cargan solo las solicitudes del tipo consultado, de
     * modo que un recurso {@code DESK} con el mismo {@code resource_id} que una plaza no
     * contamine el calculo (los identificadores no son unicos entre tablas de recurso).</p>
     *
     * @param status       estado por el que filtrar (p. ej. {@code APPROVED})
     * @param resourceType tipo de recurso ({@code PARKING}/{@code DESK})
     * @param start        fecha inicial del intervalo (inclusive)
     * @param end          fecha final del intervalo (inclusive)
     * @return solicitudes del intervalo en ese estado y tipo (posiblemente vacia)
     */
    List<Request> findByStatusAndResourceTypeAndRequestedDateBetween(
            RequestStatus status, ResourceType resourceType, LocalDate start, LocalDate end);

    /**
     * Solicitudes de un empleado cuyo {@code requested_date} cae dentro del intervalo
     * (extremos inclusive).
     *
     * <p>Carga por rango para la vista "Mi Semana" ({@code availability-calendar}):
     * restringe a las solicitudes propias del solicitante en una sola consulta; el estado
     * ({@code PENDING}/{@code APPROVED}) se cruza en memoria.</p>
     *
     * @param employeeId empleado solicitante
     * @param start      fecha inicial del intervalo (inclusive)
     * @param end        fecha final del intervalo (inclusive)
     * @return solicitudes propias del intervalo (posiblemente vacia)
     */
    List<Request> findByEmployeeIdAndRequestedDateBetween(
            Long employeeId, LocalDate start, LocalDate end);

    /**
     * Solicitudes de un empleado y de un tipo de recurso cuyo {@code requested_date} cae
     * dentro del intervalo (extremos inclusive): variante filtrada por {@code resource_type}
     * de {@link #findByEmployeeIdAndRequestedDateBetween} para la vista "Mi Semana" por tipo.
     *
     * @param employeeId   empleado solicitante
     * @param resourceType tipo de recurso ({@code PARKING}/{@code DESK})
     * @param start        fecha inicial del intervalo (inclusive)
     * @param end          fecha final del intervalo (inclusive)
     * @return solicitudes propias del intervalo y tipo (posiblemente vacia)
     */
    List<Request> findByEmployeeIdAndResourceTypeAndRequestedDateBetween(
            Long employeeId, ResourceType resourceType, LocalDate start, LocalDate end);
}
