package com.aleatica.parking.resource;

import java.util.Optional;

/**
 * Puerto de dominio que resuelve una referencia {@code (resource_id, resource_type)}
 * al recurso concreto ({@link BookableResource}) que la materializa.
 *
 * <p>Invierte la dependencia (arquitectura hexagonal): la logica compartida de
 * asignacion/solicitud/liberacion/disponibilidad depende de esta abstraccion, no de
 * un repositorio concreto. Cada tipo de recurso aporta un adaptador que declara el
 * {@link ResourceType} que soporta; el nucleo de parking provee el de
 * {@link ResourceType#PARKING} sobre {@code ParkingSpace}, y la capability
 * {@code desks} anadira el de {@link ResourceType#DESK} sin tocar la logica comun.</p>
 */
public interface ResourceResolverPort {

    /**
     * @return el tipo de recurso que este adaptador resuelve
     */
    ResourceType supportedType();

    /**
     * Resuelve el recurso concreto por su identificador.
     *
     * @param resourceId identificador del recurso dentro de su tabla concreta
     * @return el recurso reservable, o vacio si no existe
     */
    Optional<BookableResource> resolve(Long resourceId);

    /**
     * Indica si existe un recurso con ese identificador (comprobacion de integridad
     * referencial previa a asignar/aprobar/liberar).
     *
     * @param resourceId identificador del recurso
     * @return {@code true} si el recurso existe
     */
    boolean exists(Long resourceId);
}
