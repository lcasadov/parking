package com.aleatica.parking.employee;

/**
 * Rol funcional del empleado dentro de parking.
 *
 * <p>Valores identicos a la lista del CHECK {@code CK_employees_role}
 * (ver {@code docs/data-model.md} §3.1). La autoridad del rol es siempre la
 * columna {@code employees.role}, nunca un claim externo.</p>
 */
public enum Role {

    /** Administrador del parking: configura recursos y resuelve solicitudes. */
    ADMIN,

    /** Empleado: solicita y libera recursos propios. */
    EMPLOYEE,

    /**
     * Agencia externa con privilegio minimo: su unica capacidad es la liberacion
     * administrativa ({@code POST /api/v1/releases/administrative}). No es un ADMIN
     * reducido jerarquicamente, sino un rol lateral fail-closed: queda excluido por
     * defecto de cualquier otro endpoint {@code hasRole('ADMIN')} o del portal de
     * empleado.
     */
    AGENCIA
}
