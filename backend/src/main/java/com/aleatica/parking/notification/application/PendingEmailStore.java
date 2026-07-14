package com.aleatica.parking.notification.application;

import com.aleatica.parking.notification.EmailOutbox;
import com.aleatica.parking.notification.EmailOutboxRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persiste una notificacion pendiente de reintento en una transaccion NUEVA
 * ({@code REQUIRES_NEW}).
 *
 * <p>El encolado ocurre en la fase {@code AFTER_COMMIT} del evento origen, cuando la
 * transaccion de negocio ya se ha completado: un {@code save} con propagacion por defecto
 * ({@code REQUIRED}) se uniria a esa transaccion en proceso de cierre y nunca se volcaria a
 * la BD. {@code REQUIRES_NEW} fuerza una transaccion propia que confirma el {@code INSERT}
 * de forma independiente. Es un bean aparte (no un metodo privado) para que la anotacion
 * transaccional se aplique via proxy (la auto-invocacion no la activaria).</p>
 */
@Component
public class PendingEmailStore {

    private final EmailOutboxRepository outboxRepository;

    /**
     * @param outboxRepository almacen de reintento
     */
    public PendingEmailStore(EmailOutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    /**
     * Encola la entrada {@code PENDING} en una transaccion nueva.
     *
     * @param outbox entrada del outbox (datos del evento) que no pudo enviarse
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void queue(EmailOutbox outbox) {
        outboxRepository.save(outbox);
    }
}
