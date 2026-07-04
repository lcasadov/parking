package com.aleatica.parking.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.aleatica.parking.auth.domain.ClockPort;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test unitario del adaptador JPA de auditoria: verifica que persiste la entrada con el
 * instante del reloj inyectable y todos los atributos del comando.
 */
@ExtendWith(MockitoExtension.class)
class JpaAuditRecorderTest {

    private static final Instant NOW = Instant.parse("2026-05-01T12:00:00Z");

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private ClockPort clock;

    @Test
    void should_persist_audit_row_when_record_called() {
        // Given
        JpaAuditRecorder recorder = new JpaAuditRecorder(auditLogRepository, clock);
        given(clock.now()).willReturn(NOW);
        AuditEntry entry = new AuditEntry(7L, "CREATE_REQUEST", "Request", 1L, "{\"id\":1}");

        // When
        recorder.record(entry);

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        then(auditLogRepository).should().save(captor.capture());
        AuditLog saved = captor.getValue();
        assertThat(saved.getActorEmployeeId()).isEqualTo(7L);
        assertThat(saved.getAction()).isEqualTo("CREATE_REQUEST");
        assertThat(saved.getEntityType()).isEqualTo("Request");
        assertThat(saved.getEntityId()).isEqualTo(1L);
        assertThat(saved.getDetails()).isEqualTo("{\"id\":1}");
        assertThat(saved.getOccurredAt()).isEqualTo(NOW);
    }
}
