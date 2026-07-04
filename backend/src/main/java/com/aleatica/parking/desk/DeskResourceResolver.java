package com.aleatica.parking.desk;

import com.aleatica.parking.resource.BookableResource;
import com.aleatica.parking.resource.ResourceResolverPort;
import com.aleatica.parking.resource.ResourceType;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Adaptador del puerto {@link ResourceResolverPort} para el tipo
 * {@link ResourceType#DESK}: resuelve una referencia {@code resource_id} al
 * {@code Desk} concreto que la materializa.
 *
 * <p>Es la segunda implementacion del puerto (junto a la de {@code PARKING}):
 * permite que la logica de dominio compartida (asignacion fija, solicitud, liberacion
 * y disponibilidad) opere sobre puestos sin acoplarse a {@code DeskRepository}
 * (inversion de dependencias, arquitectura hexagonal).</p>
 */
@Component
public class DeskResourceResolver implements ResourceResolverPort {

    private final DeskRepository deskRepository;

    /**
     * @param deskRepository repositorio de puestos (materializacion DESK)
     */
    public DeskResourceResolver(DeskRepository deskRepository) {
        this.deskRepository = deskRepository;
    }

    @Override
    public ResourceType supportedType() {
        return ResourceType.DESK;
    }

    @Override
    public Optional<BookableResource> resolve(Long resourceId) {
        return deskRepository.findById(resourceId).map(BookableResource.class::cast);
    }

    @Override
    public boolean exists(Long resourceId) {
        return deskRepository.existsById(resourceId);
    }
}
