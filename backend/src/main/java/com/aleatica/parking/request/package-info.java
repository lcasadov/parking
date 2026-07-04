/**
 * Modulo de solicitudes puntuales: la entidad de persistencia
 * {@link com.aleatica.parking.request.Request}, su repositorio JPA, los casos de uso
 * ({@link com.aleatica.parking.request.application.RequestService}) y el adaptador web
 * ({@link com.aleatica.parking.request.RequestController}).
 *
 * <p>Una solicitud es la peticion de un empleado de una plaza para una fecha, con ciclo
 * de vida {@code PENDING} &rarr; {@code APPROVED} | {@code REJECTED} | {@code CANCELLED}.
 * La ventana temporal (hoy..hoy+14) vive en el caso de uso con {@code ClockPort}; la
 * unicidad {@code PENDING} por empleado/fecha y la de plaza {@code APPROVED} por fecha
 * las garantizan indices unicos filtrados de la BD (409, incluida la concurrencia entre
 * administradores). El listado y la cancelacion propios aplican verificacion de
 * pertenencia (BOLA), no solo RBAC. Las notificaciones se disparan {@code AFTER_COMMIT}.
 * La disponibilidad al aprobar es logica temporal que consolidara
 * {@code availability-calendar} (B7).</p>
 */
package com.aleatica.parking.request;
