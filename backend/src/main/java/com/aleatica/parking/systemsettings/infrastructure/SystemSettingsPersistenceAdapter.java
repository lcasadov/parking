package com.aleatica.parking.systemsettings.infrastructure;

import com.aleatica.parking.systemsettings.domain.SystemSettings;
import com.aleatica.parking.systemsettings.domain.SystemSettingsRepositoryPort;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Adaptador de persistencia del singleton {@code system-settings} (arquitectura hexagonal):
 * implementa el puerto de dominio {@link SystemSettingsRepositoryPort} delegando en el
 * repositorio Spring Data {@link SystemSettingsJpaRepository} y traduciendo entidad&harr;dominio
 * con {@link SystemSettingsMapper}.
 *
 * <p>No abre transacciones propias: la frontera {@code @Transactional} permanece en el servicio
 * de aplicacion. La lectura y la escritura operan sobre la unica fila
 * ({@code id = SINGLETON_ID}); {@code save} sobre un modelo con el id constante presente produce
 * un {@code merge} JPA (actualizacion) o un {@code insert} si la fila faltara.</p>
 */
@Repository
public class SystemSettingsPersistenceAdapter implements SystemSettingsRepositoryPort {

    private final SystemSettingsJpaRepository jpaRepository;

    /**
     * @param jpaRepository repositorio Spring Data JPA del ajuste global
     */
    public SystemSettingsPersistenceAdapter(SystemSettingsJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<SystemSettings> find() {
        return jpaRepository.findById((byte) SystemSettings.SINGLETON_ID)
                .map(SystemSettingsMapper::toDomain);
    }

    @Override
    public SystemSettings save(SystemSettings settings) {
        return SystemSettingsMapper.toDomain(
                jpaRepository.save(SystemSettingsMapper.toEntity(settings)));
    }
}
