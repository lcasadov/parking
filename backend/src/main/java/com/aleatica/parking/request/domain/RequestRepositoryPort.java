package com.aleatica.parking.request.domain;

import com.aleatica.parking.resource.ResourceType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Puerto de salida de persistencia del agregado {@code request} (arquitectura hexagonal,
 * change {@code hexagonal-persistence}).
 *
 * <p>Declara <strong>solo lo que necesita {@code RequestService}</strong> (design §D3) y opera
 * exclusivamente con el modelo de dominio {@link Request}: nunca expone la entidad JPA ni tipos
 * de Spring Data Repository. La paginacion se mantiene con {@link Page}/{@link Pageable} de
 * {@code org.springframework.data.domain} (value types estables, design §D4), la unica
 * dependencia de Spring admitida en el dominio.</p>
 *
 * <p>Los metodos derivados y {@code @Query} de Spring Data viven en la interfaz
 * {@code RequestJpaRepository} dentro del adaptador de infraestructura, que traduce
 * entidad&harr;dominio; este puerto es agnostico de la tecnologia de persistencia.</p>
 */
public interface RequestRepositoryPort {

    /**
     * Busca una solicitud por su identificador.
     *
     * @param id identificador de la solicitud
     * @return la solicitud de dominio, o vacio si no existe
     */
    Optional<Request> findById(Long id);

    /**
     * Persiste (alta o actualizacion) una solicitud.
     *
     * @param request solicitud de dominio a persistir
     * @return la solicitud persistida (con identificador asignado en el alta)
     */
    Request save(Request request);

    /**
     * Persiste una solicitud y fuerza el volcado inmediato a la BD (flush), de modo que las
     * violaciones de indice unico filtrado (unicidad {@code PENDING}, disponibilidad
     * {@code APPROVED}) afloren como {@code DataIntegrityViolationException} dentro del caso de
     * uso y se traduzcan a 409 (design §D5: la frontera de concurrencia no cambia).
     *
     * @param request solicitud de dominio a persistir
     * @return la solicitud persistida (con identificador asignado en el alta)
     */
    Request saveAndFlush(Request request);

    /**
     * Indica si el empleado ya tiene una solicitud en el estado dado, del tipo de recurso
     * indicado, para la fecha (soporte de la unicidad {@code PENDING} por empleado/tipo/fecha:
     * comprobacion previa antes del alta).
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
     * Pagina de las solicitudes de un empleado (listado "mis solicitudes").
     *
     * @param employeeId empleado propietario
     * @param pageable   pagina y orden solicitados
     * @return pagina de solicitudes de dominio del empleado
     */
    Page<Request> findByEmployeeId(Long employeeId, Pageable pageable);

    /**
     * Pagina de las solicitudes de un empleado filtradas por estado.
     *
     * @param employeeId empleado propietario
     * @param status     estado por el que filtrar
     * @param pageable   pagina y orden solicitados
     * @return pagina de solicitudes de dominio del empleado en ese estado
     */
    Page<Request> findByEmployeeIdAndStatus(Long employeeId, RequestStatus status, Pageable pageable);

    /**
     * Pagina de las solicitudes de un empleado cuyo {@code requestedDate} cae en el intervalo
     * (extremos inclusive), para el filtro por mes de "mis solicitudes" (change
     * {@code reservas-employee-admin-reassign}, Feature D).
     *
     * @param employeeId empleado propietario
     * @param from       fecha de recurso minima (inclusive)
     * @param to         fecha de recurso maxima (inclusive)
     * @param pageable   pagina, tamano y orden solicitados
     * @return pagina de solicitudes propias del intervalo
     */
    Page<Request> findByEmployeeIdAndRequestedDateBetween(
            Long employeeId, LocalDate from, LocalDate to, Pageable pageable);

    /**
     * Pagina de las solicitudes de un empleado filtradas por estado cuyo {@code requestedDate} cae
     * en el intervalo (extremos inclusive), para el filtro por mes de "mis solicitudes" combinado
     * con el filtro de estado (change {@code reservas-employee-admin-reassign}, Feature D).
     *
     * @param employeeId empleado propietario
     * @param status     estado por el que filtrar
     * @param from       fecha de recurso minima (inclusive)
     * @param to         fecha de recurso maxima (inclusive)
     * @param pageable   pagina, tamano y orden solicitados
     * @return pagina de solicitudes propias del intervalo en ese estado
     */
    Page<Request> findByEmployeeIdAndStatusAndRequestedDateBetween(
            Long employeeId, RequestStatus status, LocalDate from, LocalDate to, Pageable pageable);

    /**
     * Pagina de solicitudes en un estado, en orden FIFO por fecha de creacion
     * (listado admin de pendientes).
     *
     * @param status   estado por el que filtrar
     * @param pageable pagina y tamano solicitados
     * @return pagina de solicitudes de dominio ordenadas por {@code created_at ASC}
     */
    Page<Request> findByStatusOrderByCreatedAtAsc(RequestStatus status, Pageable pageable);

    /**
     * Pagina de solicitudes en un estado, en orden de actividad reciente por fecha de creacion
     * descendente (listado admin por estado: aprobadas / rechazadas).
     *
     * @param status   estado por el que filtrar
     * @param pageable pagina y tamano solicitados
     * @return pagina de solicitudes de dominio ordenadas por {@code created_at DESC}
     */
    Page<Request> findByStatusOrderByCreatedAtDesc(RequestStatus status, Pageable pageable);

    /**
     * Pagina de todas las solicitudes (cualquier estado) en orden de actividad reciente por
     * fecha de creacion descendente (listado admin de la pestana "todas").
     *
     * @param pageable pagina y tamano solicitados
     * @return pagina de solicitudes de dominio ordenadas por {@code created_at DESC}
     */
    Page<Request> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Pagina de solicitudes en un estado, con el ORDEN que indique el {@code Pageable} (sin orden
     * fijo en el nombre): permite que el cliente ordene por columnas permitidas (change
     * {@code sortable-table-columns}). El caso de uso aplica el orden por defecto y la whitelist.
     *
     * @param status   estado por el que filtrar
     * @param pageable pagina, tamano y orden solicitados
     * @return pagina de solicitudes de dominio segun el orden del {@code Pageable}
     */
    Page<Request> findByStatus(RequestStatus status, Pageable pageable);

    /**
     * Pagina de todas las solicitudes (cualquier estado) con el ORDEN que indique el
     * {@code Pageable} (sin orden fijo en el nombre); ver {@link #findByStatus}.
     *
     * @param pageable pagina, tamano y orden solicitados
     * @return pagina de solicitudes de dominio segun el orden del {@code Pageable}
     */
    Page<Request> findAll(Pageable pageable);

    /**
     * Candidatas a la <strong>lista de espera</strong> (change {@code waitlist-requests}) de un
     * dia y tipo de recurso: solicitudes {@code PENDING} marcadas {@code waitlisted}, en orden
     * FIFO ({@code created_at ASC}). El motor de promocion ({@code RequestService#promoteWaitlist})
     * reordena en memoria por categoria del empleado antes de FIFO (la categoria no vive en este
     * agregado).
     *
     * @param status        estado a comprobar (siempre {@code PENDING})
     * @param resourceType  tipo de recurso ({@code PARKING}/{@code DESK})
     * @param requestedDate fecha del dia liberado
     * @return las solicitudes en espera de ese dia/tipo en orden FIFO (posiblemente vacia)
     */
    List<Request> findByStatusAndWaitlistedTrueAndResourceTypeAndRequestedDateOrderByCreatedAtAsc(
            RequestStatus status, ResourceType resourceType, LocalDate requestedDate);
}
