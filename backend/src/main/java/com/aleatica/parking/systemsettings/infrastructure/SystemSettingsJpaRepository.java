package com.aleatica.parking.systemsettings.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA del singleton {@code system-settings} (adaptador de salida de
 * infraestructura, change {@code request-auto-assignment}).
 *
 * <p>Opera con la entidad JPA {@link SystemSettingsEntity}; el caso de uso accede a el a traves
 * de {@link SystemSettingsPersistenceAdapter} (que mapea a/desde el modelo de dominio). Al ser
 * un singleton, se accede siempre por la clave constante ({@code id = 1}).</p>
 */
public interface SystemSettingsJpaRepository extends JpaRepository<SystemSettingsEntity, Byte> {
}
