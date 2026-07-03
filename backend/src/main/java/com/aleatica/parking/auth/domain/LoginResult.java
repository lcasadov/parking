package com.aleatica.parking.auth.domain;

/**
 * Resultado de un intento de autenticacion, persistido en {@code login_log.result}.
 *
 * <p>Valores identicos al CHECK {@code CK_login_log_result}
 * ({@code docs/data-model.md} §3.9).</p>
 */
public enum LoginResult {

    /** Autenticacion correcta (Fase 1 / Fase 2). */
    OK,

    /** Credenciales invalidas (login inexistente o contrasena incorrecta). */
    INVALID_CREDENTIALS,

    /** Cuenta temporalmente bloqueada por intentos fallidos. */
    LOCKED,

    /** Cuenta inactiva o deshabilitada. */
    INACTIVE,

    /** Sin acceso: el login no esta autorizado en parking (Fase 2). */
    NO_ACCESS,

    /** Autenticacion correcta por el fallback de emergencia (Fase 2). */
    FALLBACK_OK
}
