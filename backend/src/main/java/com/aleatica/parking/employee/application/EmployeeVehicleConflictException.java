package com.aleatica.parking.employee.application;

import com.aleatica.parking.exception.FieldConflictException;

/**
 * Colision de matricula al dar de alta o editar un vehiculo de empleado: el empleado ya
 * tiene otro vehiculo con la misma matricula (normalizada). El manejador global la traduce a
 * {@code 409 Conflict} indicando el campo {@code licensePlate} (change {@code employee-vehicles}).
 */
public class EmployeeVehicleConflictException extends FieldConflictException {

    /**
     * @param field   campo en conflicto (siempre {@code licensePlate})
     * @param message mensaje legible por humanos
     */
    public EmployeeVehicleConflictException(String field, String message) {
        super(field, message);
    }
}
