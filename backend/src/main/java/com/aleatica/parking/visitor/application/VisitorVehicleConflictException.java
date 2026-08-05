package com.aleatica.parking.visitor.application;

import com.aleatica.parking.exception.FieldConflictException;

/**
 * Colision de matricula al dar de alta o editar un vehiculo de visitante: el visitante ya
 * tiene otro vehiculo con la misma matricula (normalizada). El manejador global la traduce a
 * {@code 409 Conflict} indicando el campo {@code licensePlate} (change {@code visitor-vehicles}).
 */
public class VisitorVehicleConflictException extends FieldConflictException {

    /**
     * @param field   campo en conflicto (siempre {@code licensePlate})
     * @param message mensaje legible por humanos
     */
    public VisitorVehicleConflictException(String field, String message) {
        super(field, message);
    }
}
