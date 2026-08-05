package com.aleatica.parking.visitor.dto;

import com.aleatica.parking.visitor.VisitorVehicle;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Vista de un vehiculo de visitante para la capa web (change {@code visitor-vehicles}). Es un DTO,
 * no la entidad JPA (S4684): el controlador nunca expone {@link VisitorVehicle}.
 *
 * @param id           identificador del vehiculo
 * @param visitorId    visitante propietario
 * @param licensePlate matricula
 * @param brand        marca (puede ser {@code null})
 * @param model        modelo (puede ser {@code null})
 * @param color        color (puede ser {@code null})
 * @param createdAt    instante de alta
 */
@Schema(description = "Vehiculo de un visitante")
public record VisitorVehicleResponse(
        Long id,
        Long visitorId,
        String licensePlate,
        String brand,
        String model,
        String color,
        Instant createdAt) {

    /** Proyecta la entidad de dominio a su DTO de respuesta. */
    public static VisitorVehicleResponse from(VisitorVehicle vehicle) {
        return new VisitorVehicleResponse(
                vehicle.getId(),
                vehicle.getVisitorId(),
                vehicle.getLicensePlate(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getColor(),
                vehicle.getCreatedAt());
    }
}
