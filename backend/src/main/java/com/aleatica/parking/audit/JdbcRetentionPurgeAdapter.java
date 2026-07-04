package com.aleatica.parking.audit;

import com.aleatica.parking.audit.application.RetentionPurgePort;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Implementacion JDBC de la purga por lotes ({@link RetentionPurgePort}) sobre SQL Server.
 *
 * <p>Usa {@code DELETE TOP (1000)} en bucle hasta que un lote borra menos de {@code BATCH_SIZE}
 * filas: asi cada lote es una transaccion corta que evita bloqueos largos y el crecimiento del
 * log de transacciones (design §Decisions). Las consultas usan parametros vinculados (sin
 * concatenacion, OWASP API / security-design §4). Se apoya en los indices
 * {@code IX_audit_log_occurred_at} / {@code IX_login_log_occurred_at} para el escaneo por
 * fecha.</p>
 */
@Component
public class JdbcRetentionPurgeAdapter implements RetentionPurgePort {

    private static final int BATCH_SIZE = 1000;

    private static final String DELETE_AUDIT_LOG =
            "DELETE TOP (" + BATCH_SIZE + ") FROM dbo.audit_log WHERE occurred_at < ?";
    private static final String DELETE_LOGIN_LOG =
            "DELETE TOP (" + BATCH_SIZE + ") FROM dbo.login_log WHERE occurred_at < ?";
    private static final String DELETE_CLOSED_REQUESTS =
            "DELETE TOP (" + BATCH_SIZE + ") FROM dbo.requests "
                    + "WHERE status <> 'PENDING' AND created_at < ?";
    private static final String DELETE_RELEASES =
            "DELETE TOP (" + BATCH_SIZE + ") FROM dbo.releases WHERE release_date < ?";
    private static final String DELETE_VISITOR_RESERVATIONS =
            "DELETE TOP (" + BATCH_SIZE + ") FROM dbo.visitor_reservations WHERE reservation_date < ?";

    private final JdbcTemplate jdbcTemplate;

    /**
     * @param jdbcTemplate plantilla JDBC sobre el datasource de la aplicacion
     */
    public JdbcRetentionPurgeAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public int purgeAuditLog(Instant cutoff) {
        return batchDelete(DELETE_AUDIT_LOG, Timestamp.from(cutoff));
    }

    @Override
    public int purgeLoginLog(Instant cutoff) {
        return batchDelete(DELETE_LOGIN_LOG, Timestamp.from(cutoff));
    }

    @Override
    public int purgeClosedRequests(Instant cutoff) {
        return batchDelete(DELETE_CLOSED_REQUESTS, Timestamp.from(cutoff));
    }

    @Override
    public int purgeReleases(LocalDate cutoffDate) {
        return batchDelete(DELETE_RELEASES, Date.valueOf(cutoffDate));
    }

    @Override
    public int purgeVisitorReservations(LocalDate cutoffDate) {
        return batchDelete(DELETE_VISITOR_RESERVATIONS, Date.valueOf(cutoffDate));
    }

    private int batchDelete(String sql, Object cutoffParam) {
        int total = 0;
        int affected;
        do {
            affected = jdbcTemplate.update(sql, cutoffParam);
            total += affected;
        } while (affected == BATCH_SIZE);
        return total;
    }
}
