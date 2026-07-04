package com.aleatica.parking.audit;

import com.aleatica.parking.auth.domain.ClockPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementacion JPA del {@link AuditRecorder}: persiste en {@code audit_log} a traves de
 * {@link AuditLogRepository}.
 *
 * <p>El registro corre en una <strong>transaccion propia</strong>
 * ({@link Propagation#REQUIRES_NEW}): asi la escritura de auditoria se confirma o revierte
 * de forma aislada y un fallo de auditoria no arrastra ni doomea la transaccion de negocio
 * (registro best-effort; el fallo lo captura y loguea quien invoca, p. ej.
 * {@link AuditAspect}). El {@code occurred_at} lo fija el reloj inyectable ({@link ClockPort})
 * para un comportamiento determinista y testeable (S2925: sin dependencia del reloj real).</p>
 */
@Component
public class JpaAuditRecorder implements AuditRecorder {

    private final AuditLogRepository auditLogRepository;
    private final ClockPort clock;

    /**
     * @param auditLogRepository repositorio de auditoria
     * @param clock              reloj inyectable (UTC) para {@code occurred_at}
     */
    public JpaAuditRecorder(AuditLogRepository auditLogRepository, ClockPort clock) {
        this.auditLogRepository = auditLogRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditEntry entry) {
        auditLogRepository.save(AuditLog.of(entry, clock.now()));
    }
}
