package com.aleatica.parking.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.aleatica.parking.audit.application.RetentionPurgeService;
import com.aleatica.parking.support.BaseIntegrationTest;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Tests de integracion de la purga de retencion contra un SQL Server real (Testcontainers).
 * Verifica lo que solo se observa con la BD real: el borrado por lotes ({@code DELETE TOP
 * (1000)}) drena mas de un lote (spec Req 4), y la purga nunca toca las entidades vivas
 * (empleados, plazas, visitantes, asignaciones fijas activas) mientras si elimina los datos
 * historicos por su criterio de fecha. La purga se invoca directamente (sin temporizadores,
 * S2925). El aislamiento entre ITs lo garantiza {@link BaseIntegrationTest#resetDomainState()}.
 */
class RetentionPurgeIT extends BaseIntegrationTest {

    private static final int OLD_AUDIT_ROWS = 2500;

    @Autowired
    private RetentionPurgeService purgeService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final Instant oldInstant = Instant.now().atZone(ZoneOffset.UTC).minusYears(3).toInstant();
    private final LocalDate oldDate = LocalDate.now(ZoneOffset.UTC).minusYears(3);

    // ---- 1.9: borrado por lotes ----

    @Test
    void should_delete_old_audit_rows_in_batches_when_purge_runs() {
        // Arrange: 2500 filas antiguas (> 1 lote de 1000) y 3 recientes
        insertOldAuditRows(OLD_AUDIT_ROWS);
        insertRecentAuditRows(3);

        // Act
        purgeService.purge();

        // Assert: se drenaron todas las antiguas (prueba el bucle multi-lote) y sobreviven las recientes
        assertThat(auditCountBefore(cutoffTimestamp())).isZero();
        assertThat(auditCountAfter(cutoffTimestamp())).isEqualTo(3);
    }

    // ---- 1.10: entidades vivas intactas + historicos purgados ----

    @Test
    void should_not_delete_live_entities_when_purge_runs() {
        // Arrange: entidades vivas con creacion antigua
        long empId = insertEmployee("ittest.purge.emp");
        long spaceId = insertSpace("P-PURGE-01");
        long visitorId = insertVisitor("PURGE-DNI-1");
        insertActiveFixedAssignment(spaceId, empId);
        // Datos historicos antiguos (deben purgarse)
        insertClosedRequest(empId, oldInstant);
        long pendingRequest = insertPendingRequest(empId);
        insertRelease(spaceId, empId, oldDate);
        insertReservation(visitorId, spaceId, oldDate);
        insertOldAuditRows(1);

        // Act
        purgeService.purge();

        // Assert: entidades vivas intactas
        assertThat(countWhereId("SELECT COUNT(*) FROM dbo.employees WHERE id = ?", empId)).isOne();
        assertThat(countWhereId("SELECT COUNT(*) FROM dbo.parking_spaces WHERE id = ?", spaceId))
                .isOne();
        assertThat(countWhereId("SELECT COUNT(*) FROM dbo.visitors WHERE id = ?", visitorId)).isOne();
        assertThat(countOf("SELECT COUNT(*) FROM dbo.fixed_assignments")).isEqualTo(1);
        // Historicos antiguos purgados; la solicitud PENDING (no cerrada) sobrevive
        assertThat(countOf("SELECT COUNT(*) FROM dbo.releases")).isZero();
        assertThat(countOf("SELECT COUNT(*) FROM dbo.visitor_reservations")).isZero();
        assertThat(countWhereId("SELECT COUNT(*) FROM dbo.requests WHERE id = ?", pendingRequest))
                .isOne();
        assertThat(countOf("SELECT COUNT(*) FROM dbo.requests WHERE status <> 'PENDING'")).isZero();
    }

    // ---- helpers ----

    private Timestamp cutoffTimestamp() {
        Instant cutoff = Instant.now().atZone(ZoneOffset.UTC).minusYears(2).toInstant();
        return Timestamp.from(cutoff);
    }

    private void insertOldAuditRows(int n) {
        List<Object[]> batch = new ArrayList<>(n);
        Timestamp old = Timestamp.from(oldInstant);
        for (int i = 0; i < n; i++) {
            batch.add(new Object[] {"OLD_ACTION", "Sample", old});
        }
        jdbcTemplate.batchUpdate(
                "INSERT INTO dbo.audit_log (action, entity_type, occurred_at) VALUES (?, ?, ?)",
                batch);
    }

    private void insertRecentAuditRows(int n) {
        Timestamp now = Timestamp.from(Instant.now());
        for (int i = 0; i < n; i++) {
            jdbcTemplate.update(
                    "INSERT INTO dbo.audit_log (action, entity_type, occurred_at) VALUES (?, ?, ?)",
                    "RECENT_ACTION", "Sample", now);
        }
    }

    private int auditCountBefore(Timestamp cutoff) {
        Integer c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.audit_log WHERE occurred_at < ?", Integer.class, cutoff);
        return c == null ? 0 : c;
    }

    private int auditCountAfter(Timestamp cutoff) {
        Integer c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.audit_log WHERE occurred_at >= ?", Integer.class, cutoff);
        return c == null ? 0 : c;
    }

    private long insertEmployee(String login) {
        jdbcTemplate.update(
                "INSERT INTO dbo.employees "
                        + "(first_name, last_name, login, email, password_must_change, is_corporate, "
                        + "auth_origin, role, enabled, active) "
                        + "VALUES ('IT', 'Purge', ?, ?, 0, 0, 'LOCAL', 'EMPLOYEE', 1, 1)",
                login, login + "@aleatica.com");
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM dbo.employees WHERE login = ?", Long.class, login);
        return id == null ? 0L : id;
    }

    private long insertSpace(String label) {
        jdbcTemplate.update("INSERT INTO dbo.parking_spaces (number, label, active) VALUES ((SELECT ISNULL(MAX(number),1000)+1 FROM dbo.parking_spaces), ?, 1)", label);
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM dbo.parking_spaces WHERE label = ?", Long.class, label);
        return id == null ? 0L : id;
    }

    private long insertVisitor(String nationalId) {
        jdbcTemplate.update(
                "INSERT INTO dbo.visitors (first_name, last_name, national_id, created_by_id) "
                        + "VALUES ('Visita', 'Purge', ?, ?)",
                nationalId, seedAdminId());
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM dbo.visitors WHERE national_id = ?", Long.class, nationalId);
        return id == null ? 0L : id;
    }

    private void insertActiveFixedAssignment(long spaceId, long empId) {
        jdbcTemplate.update(
                "INSERT INTO dbo.fixed_assignments (resource_id, employee_id, day_of_week, "
                        + "active, created_by_id, created_at) VALUES (?, ?, 1, 1, ?, ?)",
                spaceId, empId, seedAdminId(), Timestamp.from(oldInstant));
    }

    private void insertClosedRequest(long empId, Instant createdAt) {
        jdbcTemplate.update(
                "INSERT INTO dbo.requests (employee_id, requested_date, status, created_at) "
                        + "VALUES (?, ?, 'REJECTED', ?)",
                empId, Date.valueOf(oldDate), Timestamp.from(createdAt));
    }

    private long insertPendingRequest(long empId) {
        jdbcTemplate.update(
                "INSERT INTO dbo.requests (employee_id, requested_date, status, created_at) "
                        + "VALUES (?, ?, 'PENDING', ?)",
                empId, Date.valueOf(oldDate), Timestamp.from(oldInstant));
        Long id = jdbcTemplate.queryForObject(
                "SELECT TOP 1 id FROM dbo.requests WHERE employee_id = ? AND status = 'PENDING' "
                        + "ORDER BY id DESC", Long.class, empId);
        return id == null ? 0L : id;
    }

    private void insertRelease(long spaceId, long empId, LocalDate date) {
        jdbcTemplate.update(
                "INSERT INTO dbo.releases (resource_id, employee_id, release_date, type, "
                        + "released_by_id, created_at) VALUES (?, ?, ?, 'VOLUNTARY', ?, ?)",
                spaceId, empId, Date.valueOf(date), empId, Timestamp.from(oldInstant));
    }

    private void insertReservation(long visitorId, long spaceId, LocalDate date) {
        jdbcTemplate.update(
                "INSERT INTO dbo.visitor_reservations (visitor_id, resource_type, resource_id, "
                        + "reservation_date, created_by_id, created_at) VALUES (?, 'PARKING', ?, ?, ?, ?)",
                visitorId, spaceId, Date.valueOf(date), seedAdminId(), Timestamp.from(oldInstant));
    }

    private long seedAdminId() {
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM dbo.employees WHERE login = ?", Long.class, SEED_ADMIN_LOGIN);
        return id == null ? 0L : id;
    }

    private int countWhereId(String sql, long id) {
        Integer c = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return c == null ? 0 : c;
    }

    private int countOf(String sql) {
        Integer c = jdbcTemplate.queryForObject(sql, Integer.class);
        return c == null ? 0 : c;
    }
}
