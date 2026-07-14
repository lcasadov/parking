package com.aleatica.parking.parkingspace;

import com.aleatica.parking.resource.BookableResource;
import com.aleatica.parking.resource.ResourceResolverPort;
import com.aleatica.parking.resource.ResourceType;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Adaptador del puerto {@link ResourceResolverPort} para el tipo
 * {@link ResourceType#PARKING}: resuelve una referencia {@code resource_id} a la
 * {@code ParkingSpace} concreta que la materializa.
 *
 * <p>Es la unica implementacion del nucleo de parking; la capability {@code desks}
 * anadira otro adaptador para {@link ResourceType#DESK} sin tocar la logica de dominio
 * compartida (inversion de dependencias, arquitectura hexagonal).</p>
 */
@Component
public class ParkingSpaceResourceResolver implements ResourceResolverPort {

    private final ParkingSpaceRepository parkingSpaceRepository;

    /**
     * @param parkingSpaceRepository repositorio de plazas (materializacion PARKING)
     */
    public ParkingSpaceResourceResolver(ParkingSpaceRepository parkingSpaceRepository) {
        this.parkingSpaceRepository = parkingSpaceRepository;
    }

    @Override
    public ResourceType supportedType() {
        return ResourceType.PARKING;
    }

    @Override
    public Optional<BookableResource> resolve(Long resourceId) {
        return parkingSpaceRepository.findById(resourceId).map(BookableResource.class::cast);
    }

    @Override
    public Map<Long, BookableResource> resolveAll(Collection<Long> resourceIds) {
        return parkingSpaceRepository.findAllById(resourceIds).stream()
                .collect(Collectors.toMap(ParkingSpace::getId, BookableResource.class::cast));
    }

    @Override
    public boolean exists(Long resourceId) {
        return parkingSpaceRepository.existsById(resourceId);
    }
}
