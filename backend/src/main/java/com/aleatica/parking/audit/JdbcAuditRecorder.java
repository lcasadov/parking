package com.aleatica.parking.audit;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Implementacion JDBC del {@link AuditRecorder}: inserta en {@code audit_log}.
 *
 * <p>Se usa JDBC en lugar de una entidad JPA porque la entidad {@code AuditLog} y
 * su repositorio pertenecen al change funcional de {@code audit-retention}; aqui
 * solo existe la tabla (creada por Flyway). En el arranque ningun caso de uso
 * dispara este registro.</p>
 */
@Component
public class JdbcAuditRecorder implements AuditRecorder {

    private static final String INSERT_SQL =
            "INSERT INTO dbo.audit_log (action, entity_type, details) VALUES (?, ?, ?)";

    private final JdbcTemplate jdbcTemplate;

    /**
     * @param jdbcTemplate plantilla JDBC sobre el datasource de la aplicacion
     */
    public JdbcAuditRecorder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void record(String action, String entityType, String details) {
        jdbcTemplate.update(INSERT_SQL, action, entityType, details);
    }
}
