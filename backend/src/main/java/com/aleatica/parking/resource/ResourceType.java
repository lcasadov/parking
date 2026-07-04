package com.aleatica.parking.resource;

/**
 * Discriminador del tipo de recurso reservable del sistema.
 *
 * <p>Generaliza la referencia reservable del nucleo (antes acoplada a
 * {@code parking_spaces}) para que la logica de asignacion fija, solicitud,
 * liberacion y disponibilidad sea compartida y este parametrizada por tipo. Se
 * persiste como texto ({@code @Enumerated(STRING)}) en la columna
 * {@code resource_type} de {@code fixed_assignments}, {@code requests} y
 * {@code releases}, con un {@code CHECK} que restringe los valores admitidos.</p>
 *
 * <ul>
 *   <li>{@link #PARKING}: plaza de parking ({@code parking_spaces}); unico tipo con
 *       filas en el nucleo actual.</li>
 *   <li>{@link #DESK}: puesto de oficina; valido en el enum pero <em>sin filas</em>
 *       hasta que llegue la capability {@code desks}.</li>
 * </ul>
 */
public enum ResourceType {

    /** Plaza de parking; recurso reservable del nucleo de parking. */
    PARKING,

    /** Puesto de oficina; reservado para la capability {@code desks} (aun sin filas). */
    DESK
}
