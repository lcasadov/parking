package com.aleatica.parking.notification.application;

import com.aleatica.parking.resource.ResourceType;

/**
 * Value object interno del canal de notificacion con el recurso asignado ya resuelto a su
 * NUMERO real (no al {@code resource_id} interno), para renderizar el correo de aprobacion.
 *
 * <p>Lo construye el {@code NotificationDispatcher} a partir de {@code resourceId} +
 * {@code resourceType} consultando el repositorio del tipo correspondiente
 * (design §Decision 1). Se pasa al renderer, que solo necesita el numero y la planta; no
 * viaja a la capa web ni contamina el DTO publico {@code RequestResponse}.</p>
 *
 * @param type   tipo del recurso ({@link ResourceType#PARKING} o {@link ResourceType#DESK})
 * @param number numero humano del recurso (plaza o puesto)
 * @param floor  planta del recurso; {@code null} para puestos (no tienen planta derivada)
 */
public record ResolvedResource(ResourceType type, Integer number, Integer floor) {
}
