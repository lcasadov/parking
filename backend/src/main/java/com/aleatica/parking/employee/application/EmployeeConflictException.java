package com.aleatica.parking.employee.application;

/**
 * Senala una colision de unicidad al crear o editar un empleado
 * ({@code login} o {@code email} ya en uso por otro empleado).
 *
 * <p>El manejador global la traduce a {@code 409 Conflict} con el cuerpo
 * uniforme {@code { error, message, fields, timestamp }}, indicando en
 * {@code fields} el campo en conflicto (design §Decisions: unicidad en dos capas).</p>
 */
public class EmployeeConflictException extends RuntimeException {

    /** Nombre del campo en conflicto ({@code login} o {@code email}). */
    private final transient String field;

    /**
     * @param field   campo en conflicto ({@code login} o {@code email})
     * @param message mensaje legible por humanos
     */
    public EmployeeConflictException(String field, String message) {
        super(message);
        this.field = field;
    }

    /**
     * @return el nombre del campo en conflicto
     */
    public String getField() {
        return field;
    }
}
