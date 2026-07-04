package com.aleatica.parking.audit;

import com.aleatica.parking.audit.application.RetentionPurgeService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job programado diario que dispara la purga de datos historicos (RGPD).
 *
 * <p>Se ejecuta a la hora configurada por {@code parking.retention.cron} (por defecto a las
 * 03:00) y delega en {@link RetentionPurgeService#purge()}, que es idempotente (solo borra lo
 * anterior al {@code cutoff}). La planificacion global la habilita
 * {@code NotificationSchedulingConfig} ({@code @EnableScheduling}). En los tests el metodo se
 * invoca directamente para no depender de temporizadores (S2925: sin {@code Thread.sleep}).</p>
 */
@Component
public class RetentionPurgeJob {

    private final RetentionPurgeService purgeService;

    /**
     * @param purgeService caso de uso de purga de datos historicos
     */
    public RetentionPurgeJob(RetentionPurgeService purgeService) {
        this.purgeService = purgeService;
    }

    /**
     * Ejecuta la purga diaria de datos historicos a la hora configurada.
     */
    @Scheduled(cron = "${parking.retention.cron:0 0 3 * * *}")
    public void runDailyPurge() {
        purgeService.purge();
    }
}
