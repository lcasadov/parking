package com.aleatica.parking.desk;

/**
 * Categoria de un puesto de oficina.
 *
 * <p>Determina la auto-asignacion por rango del empleado (change
 * {@code desk-auto-assignment}): {@link #EXECUTIVE} es <strong>exclusivo</strong> de las
 * categorias ALTAS ({@code CEO, CONSEJO, DIRECTOR_N1, DIRECTOR_N2}), que lo prefieren y caen a
 * {@link #STANDARD} si no hay ninguno libre; el resto de categorias solo candidata puestos
 * {@link #STANDARD} y nunca recibe un {@link #EXECUTIVE} en la auto-asignacion. Fuera de la
 * auto-asignacion (asignacion fija, elección explicita del ADMIN/visitante), ambas categorias se
 * asignan, liberan y solicitan igual. Se persiste como texto ({@code @Enumerated(STRING)}) en la
 * columna {@code category} de {@code desks}, con un {@code CHECK} que restringe los valores
 * admitidos.</p>
 */
public enum DeskCategory {

    /** Puesto estandar; candidato de la auto-asignacion para cualquier categoria de empleado. */
    STANDARD,

    /**
     * Puesto ejecutivo; en la auto-asignacion es exclusivo de las categorias ALTAS (fallback a
     * {@link #STANDARD} si no hay ninguno libre); el resto de categorias nunca lo recibe.
     */
    EXECUTIVE
}
