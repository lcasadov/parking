package com.aleatica.parking.request.application;

import com.aleatica.parking.notification.event.RequestCancelledEvent;
import com.aleatica.parking.release.application.ReleaseAuditEvent;
import com.aleatica.parking.release.dto.ReleaseResponse;
import com.aleatica.parking.request.dto.RequestResponse;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Engancha el motor de promocion de lista de espera ({@link RequestService#promoteWaitlist}) a
 * cada origen de liberacion de un recurso para una fecha (change {@code waitlist-requests}),
 * <strong>despues del commit</strong> de la transaccion que libero el recurso:
 * <ul>
 *   <li>cancelacion de una solicitud {@code APPROVED}, por el propio empleado
 *       ({@code RequestService#cancel}) o por un {@code ADMIN}
 *       ({@code RequestService#adminCancel}) &mdash; ambas publican {@link RequestCancelledEvent};</li>
 *   <li>liberacion de una asignacion fija, voluntaria ({@code ReleaseService#createRelease}) o
 *       administrativa ({@code ReleaseService#createAdministrativeRelease}) &mdash; ambas publican
 *       {@link ReleaseAuditEvent} con el {@code Kind} correspondiente.</li>
 * </ul>
 *
 * <p>{@code AFTER_COMMIT} garantiza que solo se promociona sobre una liberacion efectivamente
 * persistida: si la transaccion de negocio se revierte, no se intenta ninguna promocion. La
 * cancelacion de una liberacion futura propia ({@link ReleaseAuditEvent.Kind#CANCELLED}) no
 * libera nada (al contrario, restaura la ocupacion de la asignacion fija) y se descarta sin
 * disparar promocion.</p>
 */
@Component
public class WaitlistPromotionListener {

    private final RequestService requestService;

    /**
     * @param requestService casos de uso de solicitudes (motor de promocion de lista de espera)
     */
    public WaitlistPromotionListener(RequestService requestService) {
        this.requestService = requestService;
    }

    /**
     * @param event evento de cancelacion de una solicitud {@code APPROVED} (recurso liberado)
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRequestCancelled(RequestCancelledEvent event) {
        RequestResponse request = event.request();
        requestService.promoteWaitlist(request.requestedDate(), request.resourceType());
    }

    /**
     * @param event evento de auditoria de una liberacion de asignacion fija (voluntaria,
     *              administrativa o cancelacion de una liberacion futura)
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReleaseAudit(ReleaseAuditEvent event) {
        if (event.kind() == ReleaseAuditEvent.Kind.CANCELLED) {
            return;
        }
        ReleaseResponse release = event.release();
        requestService.promoteWaitlist(release.releaseDate(), release.resourceType());
    }
}
