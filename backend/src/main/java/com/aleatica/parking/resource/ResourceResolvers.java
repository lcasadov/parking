package com.aleatica.parking.resource;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Registro de {@link ResourceResolverPort} indexado por {@link ResourceType}.
 *
 * <p>Punto unico de resolucion polimorfica de recursos reservables para la logica de
 * dominio compartida (asignacion fija, solicitud, liberacion y disponibilidad): tras
 * {@code generic-resource-refactor} las referencias son {@code resource_id} +
 * {@code resource_type}, y la referencia ya no tiene una FK a una unica tabla, por lo
 * que la integridad referencial del recurso se comprueba aqui, en la capa de
 * aplicacion, delegando en el adaptador del tipo correspondiente ({@code PARKING} sobre
 * {@code ParkingSpace}, {@code DESK} sobre {@code Desk}).</p>
 */
@Component
public class ResourceResolvers {

    private final Map<ResourceType, ResourceResolverPort> byType;

    /**
     * @param resolvers todos los adaptadores de resolucion disponibles (uno por tipo)
     */
    public ResourceResolvers(List<ResourceResolverPort> resolvers) {
        Map<ResourceType, ResourceResolverPort> map = new EnumMap<>(ResourceType.class);
        for (ResourceResolverPort resolver : resolvers) {
            map.put(resolver.supportedType(), resolver);
        }
        this.byType = Map.copyOf(map);
    }

    /**
     * Indica si existe un recurso del tipo dado con ese identificador.
     *
     * @param resourceId   identificador del recurso
     * @param resourceType tipo del recurso
     * @return {@code true} si el recurso existe
     */
    public boolean exists(Long resourceId, ResourceType resourceType) {
        return resolverFor(resourceType).exists(resourceId);
    }

    /**
     * Resuelve el recurso concreto por su identificador y tipo.
     *
     * @param resourceId   identificador del recurso
     * @param resourceType tipo del recurso
     * @return el recurso reservable, o vacio si no existe
     */
    public Optional<BookableResource> resolve(Long resourceId, ResourceType resourceType) {
        return resolverFor(resourceType).resolve(resourceId);
    }

    private ResourceResolverPort resolverFor(ResourceType resourceType) {
        ResourceResolverPort resolver = byType.get(resourceType);
        if (resolver == null) {
            throw new IllegalStateException("Sin resolver para el tipo de recurso: " + resourceType);
        }
        return resolver;
    }
}
