package com.aleatica.parking.employee;

/**
 * Origen de autenticacion de la cuenta del empleado.
 *
 * <p>Valores identicos al CHECK {@code CK_employees_auth_origin}
 * ({@code docs/data-model.md} §3.1). Es independiente de {@code is_corporate}:
 * un empleado corporativo puede conservar una contrasena {@code LOCAL} de
 * fallback.</p>
 */
public enum AuthOrigin {

    /** Autenticacion con contrasena local (Fase 1 / fallback de emergencia). */
    LOCAL,

    /** Autenticacion delegada en la landing corporativa via EntraID (Fase 2). */
    ENTRA_ID
}
