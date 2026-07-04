package com.aleatica.parking.desk;

/**
 * Categoria de un puesto de oficina.
 *
 * <p>Es una <em>distincion visual</em>, no una excepcion de reglas de reserva: un
 * puesto {@link #EXECUTIVE} se asigna, libera y solicita exactamente igual que uno
 * {@link #STANDARD} (design §Decisions). Se persiste como texto
 * ({@code @Enumerated(STRING)}) en la columna {@code category} de {@code desks}, con
 * un {@code CHECK} que restringe los valores admitidos.</p>
 */
public enum DeskCategory {

    /** Puesto estandar; reservable por cualquier empleado. */
    STANDARD,

    /** Puesto ejecutivo; habitualmente asignado L-V a un directivo, liberable igual. */
    EXECUTIVE
}
