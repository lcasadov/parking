package com.aleatica.parking.desk.application;

import com.aleatica.parking.exception.FieldConflictException;

/**
 * Senala una colision de unicidad al crear un puesto ({@code number} ya en uso por otro
 * puesto).
 *
 * <p>El manejador global la traduce a {@code 409 Conflict} con el cuerpo uniforme
 * {@code { error, message, fields, timestamp }}, indicando el campo {@code number} en
 * {@code fields}. La comprobacion previa del caso de uso da el mensaje claro; el indice
 * unico {@code UX_desks_number} es la red dura frente a concurrencia (design §Decisions:
 * unicidad en dos capas).</p>
 */
public class DeskConflictException extends FieldConflictException {

    /**
     * @param field   campo en conflicto ({@code number})
     * @param message mensaje legible por humanos
     */
    public DeskConflictException(String field, String message) {
        super(field, message);
    }
}
