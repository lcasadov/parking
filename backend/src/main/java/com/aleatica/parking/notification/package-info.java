/**
 * Capability transversal {@code notifications}: envio de emails disparado por eventos de
 * dominio (no por endpoints propios). No expone API REST.
 *
 * <p>Contiene la entidad del almacen de reintento
 * ({@link com.aleatica.parking.notification.EmailOutbox}) y su repositorio, mas los
 * subpaquetes {@code event} (eventos de dominio publicados por {@code requests},
 * {@code fixed-assignments} y {@code employees}), {@code application} (puerto de envio,
 * listener {@code AFTER_COMMIT}, resolucion de destinatarios, entrega resiliente y job de
 * reintento) e {@code infrastructure} (adaptador SMTP y planificacion).</p>
 */
package com.aleatica.parking.notification;
