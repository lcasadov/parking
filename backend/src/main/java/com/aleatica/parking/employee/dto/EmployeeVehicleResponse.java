package com.aleatica.parking.employee.dto;

import com.aleatica.parking.employee.EmployeeVehicle;
import com.aleatica.parking.employee.VehicleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Vista de un vehiculo de empleado para la capa web (change {@code employee-vehicles}). Es un DTO,
 * no la entidad JPA (S4684): el controlador nunca expone {@link EmployeeVehicle}.
 *
 * @param id           identificador del vehiculo
 * @param employeeId   empleado propietario
 * @param licensePlate matricula
 * @param brand        marca (puede ser {@code null})
 * @param model        modelo (puede ser {@code null})
 * @param color           color (puede ser {@code null})
 * @param status          estado de validacion (PENDING/APPROVED/REJECTED)
 * @param rejectionReason motivo del rechazo (solo si {@code status = REJECTED})
 * @param createdAt       instante de alta
 */
@Schema(description = "Vehiculo de un empleado")
public record EmployeeVehicleResponse(
        Long id,
        Long employeeId,
        String licensePlate,
        String brand,
        String model,
        String color,
        VehicleStatus status,
        String rejectionReason,
        Instant createdAt) {

    /** Proyecta la entidad de dominio a su DTO de respuesta. */
    public static EmployeeVehicleResponse from(EmployeeVehicle vehicle) {
        return new EmployeeVehicleResponse(
                vehicle.getId(),
                vehicle.getEmployeeId(),
                vehicle.getLicensePlate(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getColor(),
                vehicle.getStatus(),
                vehicle.getRejectionReason(),
                vehicle.getCreatedAt());
    }
}
