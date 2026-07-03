package com.aleatica.parking.auth.application;

import com.aleatica.parking.auth.domain.LoginPhase;
import com.aleatica.parking.auth.domain.LoginResult;

/**
 * Puerto de salida para registrar cada intento de autenticacion en {@code login_log}.
 *
 * <p>Separado de la auditoria funcional ({@code audit_log}) para no contaminarla
 * con ruido de autenticacion (security-design §8). La implementacion no debe
 * propagar fallos de escritura: el registro es trazabilidad, no debe tumbar el
 * login.</p>
 */
public interface LoginLogRecorder {

    /**
     * Registra un intento de login.
     *
     * @param loginAttempted login tal cual lo intento el usuario (nunca {@code null})
     * @param employeeId     id del empleado resuelto, o {@code null} si no se resolvio
     * @param result         resultado del intento
     * @param phase          fase de autenticacion
     */
    void record(String loginAttempted, Long employeeId, LoginResult result, LoginPhase phase);
}
