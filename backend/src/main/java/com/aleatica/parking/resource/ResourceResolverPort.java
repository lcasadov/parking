package com.aleatica.parking.resource;

import java.util.Collection;
import java.util.Map;
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
     * Resuelve en lote los recursos concretos de este tipo por sus identificadores.
     *
     * <p>Materializa el numero humano de una lista de referencias en una unica consulta
     * (evita el N+1 que provocaria invocar {@link #resolve(Long)} por cada elemento al
     * pintar, p. ej., "Mis solicitudes"). Los identificadores inexistentes se omiten del
     * mapa resultante.</p>
     *
     * @param resourceIds identificadores a resolver
     * @return mapa {@code resource_id -> recurso}; vacio si {@code resourceIds} lo esta
     */
    Map<Long, BookableResource> resolveAll(Collection<Long> resourceIds);

    /**
     * Indica si existe un recurso con ese identificador (comprobacion de integridad
     * referencial previa a asignar/aprobar/liberar).
     *
     * @param resourceId identificador del recurso
     * @return {@code true} si el recurso existe
     */
    boolean exists(Long resourceId);
}
