/**
 * Eventos de dominio de notificacion, publicados por los casos de uso de {@code requests},
 * {@code fixed-assignments} y {@code employees} y consumidos {@code AFTER_COMMIT} por la
 * capability {@code notifications}.
 *
 * <p>Todos transportan DTOs o identificadores, nunca entidades JPA (S4684 / OWASP API3),
 * de modo que el envio (fuera de la transaccion origen) no dependa de contexto de
 * persistencia.</p>
 */
package com.aleatica.parking.notification.event;
