/**
 * Capability {@code availability-calendar} (consulta-only): calculo consolidado de
 * disponibilidad de recursos para una fecha y vistas de calendario (disponibilidad puntual,
 * calendario semanal admin y "Mi Semana").
 *
 * <p>No muta estado: solo lee las tablas producidas por {@code parking-spaces},
 * {@code fixed-assignments}, {@code releases}, {@code requests} y {@code visitors}. La regla
 * de disponibilidad vive en {@code AvailabilityService} (dominio) para no divergir de las
 * comprobaciones en linea de {@code requests} y {@code visitors}.</p>
 */
package com.aleatica.parking.availability;
