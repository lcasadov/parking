package com.aleatica.parking.release.application;

/**
 * Se lanza al liberar un recurso que ya tiene una liberacion para la misma fecha
 * (unicidad recurso+fecha), tanto en la comprobacion previa del servicio como al
 * traducir la violacion del indice unico de BD bajo concurrencia.
 *
 * <p>El manejador global la traduce a {@code 409 Conflict} con
 * {@code error = RESOURCE_ALREADY_RELEASED} y {@code fields.parkingSpaceId} (spec
 * Req 4). El mismo codigo de error cubre el duplicado secuencial y la carrera de dos
 * liberaciones simultaneas.</p>
 */
public class ResourceAlreadyReleasedException extends RuntimeException {

    /**
     * @param message mensaje legible por humanos
     */
    public ResourceAlreadyReleasedException(String message) {
        super(message);
    }
}
