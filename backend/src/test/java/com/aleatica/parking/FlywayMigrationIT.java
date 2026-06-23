package com.aleatica.parking;

import static org.assertj.core.api.Assertions.assertThat;

import com.aleatica.parking.support.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Verifica que Flyway aplica las migraciones de infraestructura y crea las tablas
 * de sesion y de auditoria con sus indices, contra un SQL Server real.
 */
class FlywayMigrationIT extends BaseIntegrationTest {

    private static final String COUNT_TABLE_SQL =
            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ?";
    private static final String COUNT_INDEX_SQL =
            "SELECT COUNT(*) FROM sys.indexes WHERE name = ?";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void should_apply_flyway_migrations_when_starting() {
        assertThat(tableExists("SPRING_SESSION")).isTrue();
        assertThat(tableExists("SPRING_SESSION_ATTRIBUTES")).isTrue();
        assertThat(tableExists("audit_log")).isTrue();
        assertThat(tableExists("login_log")).isTrue();
    }

    @Test
    void should_create_empty_audit_tables_when_starting() {
        assertThat(rowCount("dbo.audit_log")).isZero();
        assertThat(rowCount("dbo.login_log")).isZero();
    }

    @Test
    void should_create_occurred_at_indexes_when_starting() {
        assertThat(indexExists("IX_audit_log_occurred_at")).isTrue();
        assertThat(indexExists("IX_login_log_occurred_at")).isTrue();
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(COUNT_TABLE_SQL, Integer.class, tableName);
        return count != null && count > 0;
    }

    private boolean indexExists(String indexName) {
        Integer count = jdbcTemplate.queryForObject(COUNT_INDEX_SQL, Integer.class, indexName);
        return count != null && count > 0;
    }

    private long rowCount(String qualifiedTable) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + qualifiedTable, Long.class);
        return count == null ? 0L : count;
    }
}
