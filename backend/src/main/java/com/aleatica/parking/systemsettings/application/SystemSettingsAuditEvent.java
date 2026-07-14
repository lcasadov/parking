package com.aleatica.parking.systemsettings.application;

import com.aleatica.parking.systemsettings.dto.SystemSettingsResponse;

/**
 * Evento de dominio publicado por {@link SystemSettingsService} tras cambiar el modo global,
 * consumido {@code AFTER_COMMIT} por {@link SystemSettingsEventListener}.
 *
 * <p>Desacopla la operacion de negocio (dentro de la transaccion) de la auditoria (fuera de
 * ella): un fallo de registro nunca revierte el cambio de ajuste. Transporta un DTO, nunca la
 * entidad JPA.</p>
 *
 * @param settings instantanea del ajuste tras el cambio
 */
public record SystemSettingsAuditEvent(SystemSettingsResponse settings) {
}
