package com.aleatica.parking.visitor;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de vehiculos de visitante (change {@code visitor-vehicles}). Acceso siempre
 * acotado por {@code visitorId} para respetar la pertenencia 1:N y no filtrar vehiculos de
 * otro visitante.
 */
public interface VisitorVehicleRepository extends JpaRepository<VisitorVehicle, Long> {

    /** Vehiculos del visitante, para el listado del tab "Vehiculos" (orden estable por id). */
    List<VisitorVehicle> findByVisitorIdOrderByIdAsc(Long visitorId);

    /** Un vehiculo por id acotado al visitante (evita acceder al vehiculo de otro visitante). */
    Optional<VisitorVehicle> findByIdAndVisitorId(Long id, Long visitorId);

    /** Comprueba si el visitante ya tiene un vehiculo con esa matricula (normalizada). */
    boolean existsByVisitorIdAndLicensePlate(Long visitorId, String licensePlate);

    /** Igual que {@link #existsByVisitorIdAndLicensePlate} excluyendo un vehiculo (edicion). */
    boolean existsByVisitorIdAndLicensePlateAndIdNot(Long visitorId, String licensePlate, Long id);
}
