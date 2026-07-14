/**
 * Adaptador de salida de persistencia del ajuste global (arquitectura hexagonal): la entidad
 * JPA de fila unica {@link com.aleatica.parking.systemsettings.infrastructure.SystemSettingsEntity},
 * el repositorio Spring Data
 * {@link com.aleatica.parking.systemsettings.infrastructure.SystemSettingsJpaRepository}, el mapper
 * {@link com.aleatica.parking.systemsettings.infrastructure.SystemSettingsMapper} y el adaptador
 * {@link com.aleatica.parking.systemsettings.infrastructure.SystemSettingsPersistenceAdapter} que
 * implementa el puerto de dominio.
 */
package com.aleatica.parking.systemsettings.infrastructure;
