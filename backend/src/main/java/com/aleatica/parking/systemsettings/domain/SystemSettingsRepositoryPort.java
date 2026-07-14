package com.aleatica.parking.systemsettings.domain;

import java.util.Optional;

/**
 * Puerto de salida de persistencia del ajuste global (arquitectura hexagonal, change
 * {@code request-auto-assignment}).
 *
 * <p>Declara <strong>solo lo que necesita {@code SystemSettingsService}</strong> y opera
 * exclusivamente con el modelo de dominio {@link SystemSettings}: nunca expone la entidad JPA
 * ni tipos de Spring Data. Al ser un singleton, la lectura es de la unica fila
 * ({@code id = 1}) y la escritura la actualiza (o la crea si faltara).</p>
 */
public interface SystemSettingsRepositoryPort {

    /**
     * Lee el ajuste global (la fila unica {@code id = 1}).
     *
     * @return el ajuste de dominio, o vacio si la fila aun no existe
     */
    Optional<SystemSettings> find();

    /**
     * Persiste (alta o actualizacion) el ajuste global.
     *
     * @param settings ajuste de dominio a persistir
     * @return el ajuste persistido
     */
    SystemSettings save(SystemSettings settings);
}
