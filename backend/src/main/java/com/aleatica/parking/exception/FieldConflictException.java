package com.aleatica.parking.exception;

/**
 * Base de las colisiones de unicidad detectadas en la capa de aplicacion
 * (un campo unico ya en uso por otra fila).
 *
 * <p>El manejador global la traduce a {@code 409 Conflict} con el cuerpo uniforme
 * {@code { error, message, fields, timestamp }}, indicando en {@code fields} el
 * campo en conflicto. Cada modulo aporta su subtipo concreto (empleados, plazas,
 * etc.) para dar un mensaje claro; el mapeo HTTP es unico y centralizado
 * (evita manejadores duplicados, S4144).</p>
 */
public abstract class FieldConflictException extends RuntimeException {

    /** Nombre del campo en conflicto (p. ej. {@code login}, {@code email}, {@code label}). */
    private final transient String field;

    /**
     * @param field   campo en conflicto
     * @param message mensaje legible por humanos
     */
    protected FieldConflictException(String field, String message) {
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
