package com.aleatica.parking.audit.application;

import com.aleatica.parking.auth.domain.ClockPort;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Caso de uso de purga de datos historicos (RGPD): calcula el {@code cutoff} a partir de la
 * ventana configurable {@code parking.retention.years} (default 2) y delega el borrado por
 * lotes en {@link RetentionPurgePort}.
 *
 * <p>El {@code cutoff} se calcula con el reloj inyectable ({@link ClockPort}) para poder
 * fijarlo en los tests de forma determinista (S2925: sin dependencia del reloj real). Solo
 * afecta a datos historicos ({@code audit_log}, {@code login_log}, solicitudes cerradas,
 * liberaciones y reservas de visitante); nunca a entidades vivas (empleados, plazas,
 * visitantes, asignaciones fijas activas). Para las tablas basadas en fecha el corte se
 * compara contra la columna de fecha (no {@code occurred_at}).</p>
 */
@Service
public class RetentionPurgeService {

    private static final Logger LOG = LoggerFactory.getLogger(RetentionPurgeService.class);

    private final RetentionPurgePort purgePort;
    private final ClockPort clock;
    private final int retentionYears;

    /**
     * @param purgePort      puerto de purga por lotes
     * @param clock          reloj inyectable (UTC) para el calculo del {@code cutoff}
     * @param retentionYears ventana de retencion en anos ({@code parking.retention.years})
     */
    public RetentionPurgeService(
            RetentionPurgePort purgePort,
            ClockPort clock,
            @Value("${parking.retention.years:2}") int retentionYears) {
        this.purgePort = purgePort;
        this.clock = clock;
        this.retentionYears = retentionYears;
    }

    /**
     * Ejecuta la purga de todas las tablas historicas con antiguedad mayor que la ventana de
     * retencion. Devuelve el total de filas borradas en el conjunto.
     *
     * @return total de filas historicas borradas
     */
    public int purge() {
        Instant cutoff = clock.now().atZone(ZoneOffset.UTC).minusYears(retentionYears).toInstant();
        LocalDate cutoffDate = cutoff.atZone(ZoneOffset.UTC).toLocalDate();

        int deleted = purgePort.purgeAuditLog(cutoff)
                + purgePort.purgeLoginLog(cutoff)
                + purgePort.purgeClosedRequests(cutoff)
                + purgePort.purgeReleases(cutoffDate)
                + purgePort.purgeVisitorReservations(cutoffDate);

        LOG.info("Purga de retencion completada (ventana {} anos, cutoff {}): {} filas borradas",
                retentionYears, cutoff, deleted);
        return deleted;
    }
}
