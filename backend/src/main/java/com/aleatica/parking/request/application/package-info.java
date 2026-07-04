/**
 * Casos de uso de solicitudes (capa de aplicacion, arquitectura hexagonal):
 * {@link com.aleatica.parking.request.application.RequestService} orquesta creacion,
 * listados, cancelacion y resolucion (aprobar/rechazar), sin depender de la web.
 *
 * <p>Define las excepciones de negocio (ventana, unicidad, estado, disponibilidad,
 * motivo de rechazo) que el manejador global traduce al cuerpo uniforme
 * {@code ApiError}. Las notificaciones por email las gestiona la capability
 * {@code notifications}, que consume {@code AFTER_COMMIT} los eventos de dominio
 * ({@code RequestCreatedEvent}, {@code RequestApprovedEvent}, {@code RequestRejectedEvent})
 * publicados por {@link com.aleatica.parking.request.application.RequestService}.</p>
 */
package com.aleatica.parking.request.application;
