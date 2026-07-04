/**
 * Capability {@code desks}: gestion de los 65 puestos de oficina como segundo tipo de
 * recurso reservable ({@link com.aleatica.parking.resource.ResourceType#DESK}).
 *
 * <p>Aporta la entidad {@link com.aleatica.parking.desk.Desk} y su CRUD (alta con numero
 * unico 1-65, edicion de categoria/coordenadas y activacion), y conecta los puestos al
 * ciclo de reserva existente (asignacion fija, solicitud, liberacion y disponibilidad)
 * mediante la abstraccion {@link com.aleatica.parking.resource.BookableResource} sin
 * duplicar logica: el adaptador {@link com.aleatica.parking.desk.DeskResourceResolver}
 * resuelve las referencias {@code (resource_id, DESK)} de {@code fixed_assignments},
 * {@code requests} y {@code releases}.</p>
 */
package com.aleatica.parking.desk;
