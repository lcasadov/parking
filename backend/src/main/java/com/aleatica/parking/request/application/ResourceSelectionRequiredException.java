package com.aleatica.parking.request.application;

/**
 * Se lanza al asignar puntualmente un recurso {@code DESK} (change
 * {@code restructure-admin-workflows}, capability {@code admin-punctual-assignment}) sin indicar
 * {@code resourceId}: a diferencia de {@code PARKING}, no hay un criterio de negocio documentado
 * para auto-asignar un puesto, por lo que el admin debe elegirlo explicitamente.
 *
 * <p>El manejador global la traduce a {@code 400 Bad Request} con {@code error =
 * VALIDATION_ERROR} y {@code fields.resourceId}.</p>
 */
public class ResourceSelectionRequiredException extends RuntimeException {

    /**
     * @param message mensaje legible por humanos
     */
    public ResourceSelectionRequiredException(String message) {
        super(message);
    }
}
