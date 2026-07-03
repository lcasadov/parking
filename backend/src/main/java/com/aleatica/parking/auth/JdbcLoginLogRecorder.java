package com.aleatica.parking.auth;

import com.aleatica.parking.auth.application.LoginLogRecorder;
import com.aleatica.parking.auth.domain.LoginPhase;
import com.aleatica.parking.auth.domain.LoginResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Implementacion JDBC del {@link LoginLogRecorder}: inserta en {@code login_log}.
 *
 * <p>Se usa JDBC (no JPA) porque la entidad {@code LoginLog} pertenece al change
 * de auditoria; aqui solo existe la tabla (creada por Flyway en {@code V2}). Un
 * fallo de escritura del log se registra y se traga: la trazabilidad no debe
 * impedir el flujo de autenticacion.</p>
 */
@Component
public class JdbcLoginLogRecorder implements LoginLogRecorder {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcLoginLogRecorder.class);

    private static final String INSERT_SQL =
            "INSERT INTO dbo.login_log (login_attempted, employee_id, result, phase) "
            + "VALUES (?, ?, ?, ?)";

    private final JdbcTemplate jdbcTemplate;

    /**
     * @param jdbcTemplate plantilla JDBC sobre el datasource de la aplicacion
     */
    public JdbcLoginLogRecorder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void record(String loginAttempted, Long employeeId, LoginResult result, LoginPhase phase) {
        try {
            jdbcTemplate.update(INSERT_SQL, loginAttempted, employeeId, result.name(), phase.name());
        } catch (org.springframework.dao.DataAccessException ex) {
            LOG.warn("No se pudo registrar el intento de login en login_log: {}", ex.getMessage());
        }
    }
}
