package com.aleatica.parking.push;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repositorio JPA de suscripciones Web Push (change {@code push-notifications}).
 */
public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

    /** Todas las suscripciones activas de un empleado (fan-out del envio). */
    List<PushSubscription> findByEmployeeId(Long employeeId);

    /** Suscripcion por endpoint (upsert idempotente al re-suscribir). */
    Optional<PushSubscription> findByEndpoint(String endpoint);

    /** Baja de una suscripcion por endpoint (solo la del propio usuario, validado en el servicio). */
    @Modifying
    @Transactional
    void deleteByEndpoint(String endpoint);

    /** Baja de todas las suscripciones de un empleado (al desactivarlo/eliminarlo). */
    @Modifying
    @Transactional
    void deleteByEmployeeId(Long employeeId);
}
