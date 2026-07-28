package com.aleatica.parking.notification.application;

import com.aleatica.parking.notification.event.FixedAssignmentRevokedEvent;
import com.aleatica.parking.notification.event.RequestAdminAssignedEvent;
import com.aleatica.parking.notification.event.RequestApprovedEvent;
import com.aleatica.parking.notification.event.RequestCancelledEvent;
import com.aleatica.parking.notification.event.RequestAdminCancelledEvent;
import com.aleatica.parking.notification.event.RequestCreatedEvent;
import com.aleatica.parking.notification.event.RequestRejectedEvent;
import com.aleatica.parking.notification.event.WaitlistAvailableEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Escucha los eventos de dominio de notificacion y delega en {@link NotificationDispatcher}
 * <strong>despues del commit</strong> ({@code AFTER_COMMIT}) de la transaccion que los
 * origino.
 *
 * <p>{@code AFTER_COMMIT} garantiza que solo se notifica lo efectivamente persistido: si la
 * transaccion de negocio se revierte, el evento no se procesa y no sale ningun email (spec
 * §"Transaccion revertida no genera email"). Un fallo del canal de notificacion nunca
 * revierte la operacion de negocio (ya confirmada). Cada metodo delega en un unico caso de
 * uso del dispatcher, manteniendo baja la complejidad (S3776).</p>
 */
@Component
public class EmailNotificationListener {

    private final NotificationDispatcher dispatcher;

    /**
     * @param dispatcher resolutor de destinatarios y contenido de las notificaciones
     */
    public EmailNotificationListener(NotificationDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    /**
     * @param event evento de creacion de solicitud
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRequestCreated(RequestCreatedEvent event) {
        dispatcher.requestCreated(event.request());
    }

    /**
     * @param event evento de aprobacion de solicitud
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRequestApproved(RequestApprovedEvent event) {
        dispatcher.requestApproved(event.request());
    }

    /**
     * @param event evento de rechazo de solicitud
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRequestRejected(RequestRejectedEvent event) {
        dispatcher.requestRejected(event.request());
    }

    /**
     * @param event evento de cancelacion de una solicitud aprobada (recurso liberado)
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRequestCancelled(RequestCancelledEvent event) {
        dispatcher.requestCancelled(event.request());
    }

    /**
     * Cancelacion administrativa de una reserva aprobada -> aviso al empleado afectado
     * (change {@code push-notifications}, design D12).
     *
     * @param event evento de cancelacion administrativa
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRequestAdminCancelled(RequestAdminCancelledEvent event) {
        dispatcher.requestAdminCancelled(event.request());
    }

    /**
     * @param event evento de revocacion de asignacion fija
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFixedAssignmentRevoked(FixedAssignmentRevokedEvent event) {
        dispatcher.assignmentRevoked(event.employeeId());
    }

    /**
     * @param event evento de asignacion puntual del admin
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRequestAdminAssigned(RequestAdminAssignedEvent event) {
        dispatcher.requestAdminAssigned(event.request());
    }

    /**
     * @param event evento de recurso liberado con lista de espera en modo MANUAL
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onWaitlistAvailable(WaitlistAvailableEvent event) {
        dispatcher.waitlistAvailable(event.topWaitlistedRequest());
    }
}
