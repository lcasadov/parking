package com.aleatica.parking.systemsettings.application;

import com.aleatica.parking.systemsettings.dto.SystemSettingsResponse;

/**
 * Puerto de salida para auditar el cambio del modo de aprobacion global.
 *
 * <p>Se define aqui el puerto y un adaptador de registro ({@link LoggingSystemSettingsAuditAdapter})
 * para no acoplar el caso de uso al canal concreto. Se invoca {@code AFTER_COMMIT} desde
 * {@link SystemSettingsEventListener}: un fallo de auditoria nunca revierte el cambio. Recibe
 * DTOs, nunca entidades JPA (S4684 / OWASP API3).</p>
 */
public interface SystemSettingsAuditPort {

    /**
     * Audita el cambio del modo de aprobacion global (accion del {@code ADMIN}).
     *
     * @param settings ajuste tras el cambio
     */
    void auditApprovalModeChanged(SystemSettingsResponse settings);
}
