package com.aleatica.parking.employee.application;

import com.aleatica.parking.employee.Employee;

/**
 * Puerto de salida para notificar al empleado su contrasena temporal tras un
 * reset administrativo.
 *
 * <p>En Fase 2 🔵 lo implementa la capability {@code notifications} (email); su
 * logica queda <strong>fuera del alcance</strong> de este change. Aqui se define
 * el puerto y un adaptador de registro ({@link LoggingPasswordResetNotifier})
 * para no acoplar el caso de uso al canal concreto.</p>
 */
public interface PasswordResetNotifier {

    /**
     * Notifica al empleado la contrasena temporal generada.
     *
     * @param employee          empleado destinatario
     * @param temporaryPassword contrasena temporal en claro
     */
    void notifyReset(Employee employee, String temporaryPassword);
}
