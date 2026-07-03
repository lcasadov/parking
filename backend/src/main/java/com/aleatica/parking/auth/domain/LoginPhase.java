package com.aleatica.parking.auth.domain;

/**
 * Fase de autenticacion en la que ocurre el intento, persistida en
 * {@code login_log.phase}.
 *
 * <p>Valores identicos al CHECK {@code CK_login_log_phase}
 * ({@code docs/data-model.md} §3.9).</p>
 */
public enum LoginPhase {

    /** Login local de Fase 1. */
    PHASE_1,

    /** SSO de Fase 2. */
    PHASE_2,

    /** Fallback de emergencia de Fase 2. */
    FALLBACK
}
