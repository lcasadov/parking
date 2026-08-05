package com.aleatica.parking.employee;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Repositorio de vehiculos de empleado (change {@code employee-vehicles}). Acceso siempre
 * acotado por {@code employeeId} para respetar la pertenencia 1:N y no filtrar vehiculos de
 * otro empleado.
 */
public interface EmployeeVehicleRepository extends JpaRepository<EmployeeVehicle, Long> {

    /** Vehiculos del empleado, para el listado del tab "Vehiculos" (orden estable por id). */
    List<EmployeeVehicle> findByEmployeeIdOrderByIdAsc(Long employeeId);

    /** Un vehiculo por id acotado al empleado (evita acceder al vehiculo de otro empleado). */
    Optional<EmployeeVehicle> findByIdAndEmployeeId(Long id, Long employeeId);

    /** Comprueba si el empleado ya tiene un vehiculo con esa matricula (normalizada). */
    boolean existsByEmployeeIdAndLicensePlate(Long employeeId, String licensePlate);

    /** Igual que {@link #existsByEmployeeIdAndLicensePlate} excluyendo un vehiculo (edicion). */
    boolean existsByEmployeeIdAndLicensePlateAndIdNot(Long employeeId, String licensePlate, Long id);

    /** Bandeja de validacion (Fase 2): pagina de vehiculos en un estado dado. */
    Page<EmployeeVehicle> findByStatus(VehicleStatus status, Pageable pageable);

    /** Bandeja de validacion (Fase 2): pagina de vehiculos en cualquiera de los estados dados. */
    Page<EmployeeVehicle> findByStatusIn(Collection<VehicleStatus> statuses, Pageable pageable);

    /** Contador de pendientes de accion para el badge (PENDING + PENDING_DELETION). */
    long countByStatusIn(Collection<VehicleStatus> statuses);

    /** Recuento de vehiculos agrupado por estado, para los contadores de los filtros (Fase 2). */
    @Query("select v.status as status, count(v) as count from EmployeeVehicle v group by v.status")
    List<StatusCount> countGroupedByStatus();

    /** Proyeccion (estado, recuento) para {@link #countGroupedByStatus()}. */
    interface StatusCount {
        VehicleStatus getStatus();

        long getCount();
    }
}
