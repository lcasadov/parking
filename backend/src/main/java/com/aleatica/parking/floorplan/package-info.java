/**
 * Capability {@code floor-plan}: plano interactivo de los 65 puestos de oficina como cara
 * visual del modelo de puestos y principal canal de reserva del empleado.
 *
 * <p>No introduce tablas ni reglas de disponibilidad nuevas: reutiliza la tabla
 * {@code desks} (capability {@link com.aleatica.parking.desk}) y el ciclo de reserva
 * generico ({@link com.aleatica.parking.resource.ResourceType#DESK}). Ofrece tres casos de
 * uso: proyectar el estado y la posicion de cada puesto para una fecha
 * ({@link com.aleatica.parking.floorplan.application.FloorPlanQueryService}, sin N+1);
 * solicitar un puesto pinchandolo en el plano reutilizando el ciclo de
 * {@code requests} pero fijando el puesto desde la creacion para bloquear la concurrencia; y
 * reposicionar los marcadores desde el editor de arrastre del {@code ADMIN}
 * ({@link com.aleatica.parking.floorplan.application.FloorPlanCommandService}).</p>
 *
 * <p>El estado por puesto ({@link com.aleatica.parking.floorplan.FloorPlanDeskState}) es
 * derivado (no persistido) y relativo al empleado que consulta: solo su propio puesto aparece
 * como {@code MINE}; los de terceros como {@code ASSIGNED}/{@code REQUESTED} sin revelar
 * titulares (RGPD / minimizacion, {@code docs/security-design.md}).</p>
 */
package com.aleatica.parking.floorplan;
