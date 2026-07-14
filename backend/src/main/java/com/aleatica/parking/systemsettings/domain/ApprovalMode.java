package com.aleatica.parking.systemsettings.domain;

/**
 * Modo global de aprobacion de solicitudes (parametro unico del sistema, change
 * {@code request-auto-assignment}).
 *
 * <p>Se persiste como {@code VARCHAR} en la columna {@code approval_mode} de la tabla
 * de fila unica {@code system_settings} (ver {@code V22__system_settings.sql}), con un
 * {@code CHECK} que restringe los valores admitidos. Controla la ramificacion del alta de
 * solicitudes: en {@link #MANUAL} la solicitud nace {@code PENDING} y la resuelve el ADMIN;
 * en {@link #AUTOMATIC} la solicitud nace {@code APPROVED} (auto-asignacion de plaza por
 * categoria/planta, o auto-aprobacion del puesto elegido).</p>
 */
public enum ApprovalMode {

    /** Modo manual: la solicitud nace {@code PENDING} y la resuelve el ADMIN (por defecto). */
    MANUAL,

    /** Modo automatico: la solicitud nace {@code APPROVED} con recurso asignado. */
    AUTOMATIC
}
