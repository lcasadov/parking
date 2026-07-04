package com.aleatica.parking.request;

import java.time.LocalDate;
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
     * Indica si la plaza ya tiene una solicitud en el estado dado para la fecha
     * (soporte de la disponibilidad al aprobar: plaza ya {@code APPROVED} esa fecha).
     *
     * @param parkingSpaceId plaza a comprobar
     * @param requestedDate  fecha solicitada
     * @param status         estado a comprobar
     * @return {@code true} si ya existe una solicitud en ese estado para esa plaza/fecha
     */
    boolean existsByParkingSpaceIdAndRequestedDateAndStatus(
            Long parkingSpaceId, LocalDate requestedDate, RequestStatus status);
}
