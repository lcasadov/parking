package com.aleatica.parking.visitor.application;

import com.aleatica.parking.exception.FieldConflictException;

/**
 * Colision de unicidad del {@code nationalId} de una ficha de visitante detectada en la
 * capa de aplicacion (comprobacion previa antes del alta/edicion).
 *
 * <p>El manejador global la traduce a {@code 409 Conflict} con el campo {@code nationalId}
 * en {@code fields} (subtipo de {@link FieldConflictException}, mapeo HTTP unico). La
 * violacion del indice unico {@code UX_visitors_national_id} bajo concurrencia produce el
 * mismo {@code 409} via {@code DataIntegrityViolation}.</p>
 */
public class DuplicateNationalIdException extends FieldConflictException {

    /** Nombre del campo en conflicto, coherente con la clave contractual de la API. */
    public static final String FIELD = "nationalId";

    /**
     * @param message mensaje legible por humanos
     */
    public DuplicateNationalIdException(String message) {
        super(FIELD, message);
    }
}
