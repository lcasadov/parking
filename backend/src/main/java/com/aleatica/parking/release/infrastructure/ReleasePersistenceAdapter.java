package com.aleatica.parking.release.infrastructure;

import com.aleatica.parking.release.domain.Release;
import com.aleatica.parking.release.domain.ReleaseRepositoryPort;
import com.aleatica.parking.resource.ResourceType;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/**
 * Adaptador de persistencia del agregado {@code release} (arquitectura hexagonal, change
 * {@code hexagonal-persistence}): implementa el puerto de dominio
 * {@link ReleaseRepositoryPort} delegando en el repositorio Spring Data
 * {@link ReleaseJpaRepository} y traduciendo entidad&harr;dominio con {@link ReleaseMapper}.
 *
 * <p>No abre transacciones propias (design §D5): la frontera {@code @Transactional} permanece en
 * el servicio de aplicacion, de modo que {@link #saveAndFlush(Release)} vuelca dentro de la
 * transaccion del caso de uso y la violacion del indice unico recurso+fecha aflora como
 * {@code DataIntegrityViolationException} para traducirse a 409. La cancelacion es un borrado
 * fisico ({@link #delete(Release)}) de la fila futura.</p>
 */
@Repository
public class ReleasePersistenceAdapter implements ReleaseRepositoryPort {

    private final ReleaseJpaRepository jpaRepository;

    /**
     * @param jpaRepository repositorio Spring Data JPA de liberaciones
     */
    public ReleasePersistenceAdapter(ReleaseJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Release> findById(Long id) {
        return jpaRepository.findById(id).map(ReleaseMapper::toDomain);
    }

    @Override
    public Release saveAndFlush(Release release) {
        return ReleaseMapper.toDomain(jpaRepository.saveAndFlush(ReleaseMapper.toEntity(release)));
    }

    @Override
    public void delete(Release release) {
        jpaRepository.deleteById(release.getId());
    }

    @Override
    public boolean existsByResourceIdAndResourceTypeAndReleaseDate(
            Long resourceId, ResourceType resourceType, LocalDate releaseDate) {
        return jpaRepository.existsByResourceIdAndResourceTypeAndReleaseDate(
                resourceId, resourceType, releaseDate);
    }

    @Override
    public Page<Release> findByEmployeeId(Long employeeId, Pageable pageable) {
        return jpaRepository.findByEmployeeId(employeeId, pageable).map(ReleaseMapper::toDomain);
    }
}
