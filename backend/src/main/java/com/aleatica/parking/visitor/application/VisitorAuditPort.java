package com.aleatica.parking.visitor.application;

import com.aleatica.parking.visitor.dto.VisitorReservationResponse;
import com.aleatica.parking.visitor.dto.VisitorResponse;

/**
 * Puerto de salida para auditar las acciones del {@code ADMIN} sobre visitantes y sus
 * reservas (crear/editar ficha, crear/anular reserva).
 *
 * <p>La persistencia enriquecida en {@code audit_log} (actor, IP, user-agent, payload) la
 * aportara la capability {@code audit-retention} (B10); su logica queda
 * <strong>fuera del alcance</strong> de este change. Aqui se define el puerto y un
 * adaptador de registro ({@link LoggingVisitorAuditAdapter}) para no acoplar el caso de
 * uso al canal concreto (consolidacion B10). Se invoca {@code AFTER_COMMIT} desde
 * {@link VisitorEventListener}: un fallo de auditoria nunca revierte la operacion de
 * negocio. Recibe DTOs, nunca entidades JPA (S4684 / OWASP API3).</p>
 */
public interface VisitorAuditPort {

    /**
     * Audita la creacion de una ficha de visitante.
     *
     * @param visitor ficha creada
     */
    void auditVisitorCreated(VisitorResponse visitor);

    /**
     * Audita la edicion de una ficha de visitante.
     *
     * @param visitor ficha actualizada
     */
    void auditVisitorUpdated(VisitorResponse visitor);

    /**
     * Audita la creacion de una reserva de visitante.
     *
     * @param reservation reserva creada
     */
    void auditReservationCreated(VisitorReservationResponse reservation);

    /**
     * Audita la anulacion de una reserva de visitante futura.
     *
     * @param reservation reserva anulada
     */
    void auditReservationCancelled(VisitorReservationResponse reservation);
}
