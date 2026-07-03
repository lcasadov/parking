package com.aleatica.parking.parkingspace.application;

import com.aleatica.parking.exception.FieldConflictException;

/**
 * Senala una colision de unicidad al crear o editar una plaza
 * ({@code label} ya en uso por otra plaza).
 *
 * <p>El manejador global la traduce a {@code 409 Conflict} con el cuerpo uniforme
 * {@code { error, message, fields, timestamp }}, indicando el campo {@code label}
 * en {@code fields} (design §Decisions: unicidad en dos capas).</p>
 */
public class ParkingSpaceConflictException extends FieldConflictException {

    /**
     * @param field   campo en conflicto ({@code label})
     * @param message mensaje legible por humanos
     */
    public ParkingSpaceConflictException(String field, String message) {
        super(field, message);
    }
}
