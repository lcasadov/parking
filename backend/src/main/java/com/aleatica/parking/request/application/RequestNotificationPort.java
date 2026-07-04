package com.aleatica.parking.request.application;

import com.aleatica.parking.request.dto.RequestResponse;

/**
 * Puerto de salida para notificar los eventos de una solicitud.
 *
 * <p>En Fase 2 🔵 lo implementa la capability {@code notifications} (email, B9); su
 * logica queda <strong>fuera del alcance</strong> de este change. Aqui se define el
 * puerto y un adaptador de registro ({@link LoggingRequestNotificationAdapter}) para
 * no acoplar el caso de uso al canal concreto. Se invoca {@code AFTER_COMMIT} desde
 * {@link RequestEventListener}: un fallo de notificacion nunca revierte la resolucion
 * (design §Risks). Recibe DTOs, nunca entidades JPA (S4684 / OWASP API3).</p>
 */
public interface RequestNotificationPort {

    /**
     * Notifica a los administradores la creacion de una nueva solicitud.
     *
     * @param request solicitud creada
     */
    void notifyCreated(RequestResponse request);

    /**
     * Notifica al empleado la aprobacion de su solicitud (con {@code approvalNote}).
     *
     * @param request solicitud aprobada
     */
    void notifyApproved(RequestResponse request);

    /**
     * Notifica al empleado el rechazo de su solicitud (con el motivo).
     *
     * @param request solicitud rechazada
     */
    void notifyRejected(RequestResponse request);
}
