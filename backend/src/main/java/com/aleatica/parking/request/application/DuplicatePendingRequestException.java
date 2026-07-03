package com.aleatica.parking.request.application;

/**
 * Se lanza cuando un empleado ya tiene una solicitud {@code PENDING} para la misma
 * fecha (unicidad {@code UX_requests_employee_date_pending}).
 *
 * <p>El manejador global la traduce a {@code 409 Conflict} con
 * {@code error = REQUEST_ALREADY_PENDING} (spec Req 1). Es la comprobacion previa
 * (mensaje claro); la red dura frente a concurrencia es el indice unico filtrado, que
 * el manejador de {@code DataIntegrityViolationException} traduce al mismo codigo.</p>
 */
public class DuplicatePendingRequestException extends RuntimeException {

    /**
     * @param message mensaje legible por humanos
     */
    public DuplicatePendingRequestException(String message) {
        super(message);
    }
}
