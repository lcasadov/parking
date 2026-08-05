package com.aleatica.parking.employee;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio del histórico de cambios de vehículos de empleado
 * (change {@code employee-vehicle-self-service}, Fase 2).
 */
public interface EmployeeVehicleHistoryRepository extends JpaRepository<EmployeeVehicleHistory, Long> {

    /** Histórico de un vehículo, del más antiguo al más reciente (orden estable). */
    List<EmployeeVehicleHistory> findByVehicleIdOrderByCreatedAtAscIdAsc(Long vehicleId);
}
