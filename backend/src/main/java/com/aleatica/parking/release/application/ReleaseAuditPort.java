package com.aleatica.parking.release.application;

import com.aleatica.parking.release.dto.ReleaseResponse;

/**
 * Puerto de salida para auditar los eventos de una liberacion (crear voluntaria,
 * crear administrativa, cancelar).
 *
 * <p>La persistencia enriquecida en {@code audit_log} (actor, IP, user-agent, payload)
 * la aportara la capability {@code audit-retention} (B10); su logica queda
 * <strong>fuera del alcance</strong> de este change. Aqui se define el puerto y un
 * adaptador de registro ({@link LoggingReleaseAuditAdapter}) para no acoplar el caso de
 * uso al canal concreto (consolidacion B10). Se invoca {@code AFTER_COMMIT} desde
 * {@link ReleaseEventListener}: un fallo de auditoria nunca revierte la operacion de
 * negocio. Recibe DTOs, nunca entidades JPA (S4684 / OWASP API3).</p>
 */
public interface ReleaseAuditPort {

    /**
     * Audita una liberacion voluntaria (accion del empleado titular).
     *
     * @param release liberacion creada
     */
    void auditVoluntaryRelease(ReleaseResponse release);

    /**
     * Audita una liberacion administrativa (accion del {@code ADMIN}).
     *
     * @param release liberacion creada
     */
    void auditAdministrativeRelease(ReleaseResponse release);

    /**
     * Audita la cancelacion de una liberacion futura propia.
     *
     * @param release liberacion anulada
     */
    void auditCancellation(ReleaseResponse release);
}
