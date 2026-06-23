package com.aleatica.parking.audit;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Test unitario del adaptador JDBC de auditoria: verifica que inserta en
 * {@code audit_log} con los parametros vinculados (sin concatenacion SQL).
 */
@ExtendWith(MockitoExtension.class)
class JdbcAuditRecorderTest {

    private static final String EXPECTED_SQL =
            "INSERT INTO dbo.audit_log (action, entity_type, details) VALUES (?, ?, ?)";

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void should_insert_audit_row_when_record_called() {
        // Given
        JdbcAuditRecorder recorder = new JdbcAuditRecorder(jdbcTemplate);

        // When
        recorder.record("CREATE_REQUEST", "Request", "{\"id\":1}");

        // Then
        then(jdbcTemplate).should()
                .update(eq(EXPECTED_SQL), eq("CREATE_REQUEST"), eq("Request"), eq("{\"id\":1}"));
    }
}
