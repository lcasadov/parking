/**
 * Casos de uso de solicitudes (capa de aplicacion, arquitectura hexagonal):
 * {@link com.aleatica.parking.request.application.RequestService} orquesta creacion,
 * listados, cancelacion y resolucion (aprobar/rechazar), sin depender de la web.
 *
 * <p>Define las excepciones de negocio (ventana, unicidad, estado, disponibilidad,
 * motivo de rechazo) que el manejador global traduce al cuerpo uniforme
 * {@code ApiError}, el puerto de notificacion
 * ({@link com.aleatica.parking.request.application.RequestNotificationPort}) con su
 * adaptador de log de Fase 1, y el disparo de eventos {@code AFTER_COMMIT}
 * ({@link com.aleatica.parking.request.application.RequestEventListener}).</p>
 */
package com.aleatica.parking.request.application;
