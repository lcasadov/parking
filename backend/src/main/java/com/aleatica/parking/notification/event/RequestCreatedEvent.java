package com.aleatica.parking.notification.event;

import com.aleatica.parking.request.dto.RequestResponse;

/**
 * Evento de dominio: se ha creado una nueva solicitud puntual. Lo publica el caso de uso
 * de creacion de solicitud y lo consume la capability {@code notifications}
 * {@code AFTER_COMMIT} para notificar por email a todos los administradores activos.
 *
 * <p>Transporta un DTO ({@link RequestResponse}), nunca la entidad JPA (S4684 / OWASP
 * API3). Un fallo del envio nunca revierte la creacion (se engancha post-commit).</p>
 *
 * @param request instantanea de la solicitud creada
 */
public record RequestCreatedEvent(RequestResponse request) {
}
