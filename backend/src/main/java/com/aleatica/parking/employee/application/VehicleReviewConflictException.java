package com.aleatica.parking.employee.application;

import com.aleatica.parking.exception.FieldConflictException;

/**
 * La acción de validación no es aplicable al estado actual del vehículo (change
 * {@code employee-vehicle-self-service}, Fase 2): p.ej. aprobar/rechazar uno que ya fue procesado
 * por otro administrador, o confirmar/restaurar uno que no está pendiente de borrado. El manejador
 * global la traduce a {@code 409 Conflict} para que la interfaz refresque el estado real.
 */
public class VehicleReviewConflictException extends FieldConflictException {

    /**
     * @param message mensaje legible por humanos
     */
    public VehicleReviewConflictException(String message) {
        super("status", message);
    }
}
