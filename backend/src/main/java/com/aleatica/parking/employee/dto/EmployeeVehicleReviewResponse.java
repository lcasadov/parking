package com.aleatica.parking.employee.dto;

import com.aleatica.parking.employee.EmployeeVehicle;
import com.aleatica.parking.employee.VehicleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Vista de un vehículo para la bandeja de validación del administrador (change
 * {@code employee-vehicle-self-service}, Fase 2): incluye los datos del empleado propietario para
 * que el admin sepa de quién es. DTO, nunca la entidad JPA (S4684).
 *
 * @param vehicleId    identificador del vehículo
 * @param employee     empleado propietario (id, nombre, departamento)
 * @param licensePlate matrícula
 * @param brand        marca (puede ser {@code null})
 * @param model        modelo (puede ser {@code null})
 * @param color        color (puede ser {@code null})
 * @param status       estado de validación
 * @param rejectionReason motivo del rechazo (solo si {@code REJECTED})
 * @param submittedAt  instante de alta del vehículo
 */
@Schema(description = "Vehículo en la bandeja de validación del administrador")
public record EmployeeVehicleReviewResponse(
        Long vehicleId,
        Owner employee,
        String licensePlate,
        String brand,
        String model,
        String color,
        VehicleStatus status,
        String rejectionReason,
        Instant submittedAt) {

    /** Empleado propietario del vehículo. */
    public record Owner(Long id, String fullName, String department) {
    }

    /** Proyecta la entidad + los datos del empleado al DTO de la bandeja. */
    public static EmployeeVehicleReviewResponse of(EmployeeVehicle vehicle, Owner owner) {
        return new EmployeeVehicleReviewResponse(
                vehicle.getId(),
                owner,
                vehicle.getLicensePlate(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getColor(),
                vehicle.getStatus(),
                vehicle.getRejectionReason(),
                vehicle.getCreatedAt());
    }
}
