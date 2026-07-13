package com.aleatica.parking.employee;

/**
 * Categoria jerarquica (rango organizativo) de un empleado.
 *
 * <p>Valores identicos a la lista del CHECK {@code CK_employees_category}
 * (ver {@code V20__employee_category.sql}). El <strong>orden de declaracion</strong>
 * es jerarquico de mayor a menor rango: los consumidores (p.ej. el futuro change
 * {@code request-auto-assignment}) pueden comparar por {@link #ordinal()}. No debe
 * alterarse el orden sin revisar dichos consumidores.</p>
 *
 * <p>Se persiste como {@code VARCHAR} con {@code @Enumerated(EnumType.STRING)} para
 * no acoplar el ordinal a la BD.</p>
 */
public enum EmployeeCategory {

    /** Consejero delegado (mayor rango). */
    CEO,

    /** Miembro del consejo. */
    CONSEJO,

    /** Director de nivel 1. */
    DIRECTOR_N1,

    /** Director de nivel 2. */
    DIRECTOR_N2,

    /** Gerente. */
    GERENTE,

    /** Mando intermedio. */
    MANDO_INTERMEDIO,

    /** Empleado sin rango de mando (menor rango; valor por defecto). */
    EMPLEADO
}
