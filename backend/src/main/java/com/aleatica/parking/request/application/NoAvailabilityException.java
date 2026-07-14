package com.aleatica.parking.request.application;

/**
 * Se lanza al crear una solicitud en <strong>modo automatico</strong> para una plaza cuando no
 * hay <em>ninguna</em> plaza libre en ninguna planta para la fecha (la auto-asignacion no puede
 * asignar recurso).
 *
 * <p>El manejador global la traduce a {@code 409 Conflict} con {@code error = NO_AVAILABILITY}
 * (design §D4). En este caso la solicitud <strong>no se crea</strong> (no queda un {@code PENDING}
 * colgado): en modo automatico la promesa es "asignacion inmediata", de modo que un 409 explicito
 * comunica que no hay recurso y el empleado puede reintentar otra fecha.</p>
 */
public class NoAvailabilityException extends RuntimeException {

    /**
     * @param message mensaje legible por humanos
     */
    public NoAvailabilityException(String message) {
        super(message);
    }
}
