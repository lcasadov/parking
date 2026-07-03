package com.aleatica.parking.employee.application;

import com.aleatica.parking.exception.FieldConflictException;

/**
 * Senala una colision de unicidad al crear o editar un empleado
 * ({@code login} o {@code email} ya en uso por otro empleado).
 *
 * <p>El manejador global la traduce a {@code 409 Conflict} con el cuerpo
 * uniforme {@code { error, message, fields, timestamp }}, indicando en
 * {@code fields} el campo en conflicto (design §Decisions: unicidad en dos capas).</p>
 */
public class EmployeeConflictException extends FieldConflictException {

    /**
     * @param field   campo en conflicto ({@code login} o {@code email})
     * @param message mensaje legible por humanos
     */
    public EmployeeConflictException(String field, String message) {
        super(field, message);
    }
}
