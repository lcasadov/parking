package com.aleatica.parking.audit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.aleatica.parking.audit.AuditLog;
import com.aleatica.parking.audit.AuditLogRepository;
import com.aleatica.parking.audit.dto.AuditLogEntryResponse;
import com.aleatica.parking.audit.AuditEntry;
import com.aleatica.parking.employee.dto.PageResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Tests unitarios del caso de uso de consulta de auditoria: mapea la pagina de entidades a
 * DTOs propagando los filtros, y valida la ventana temporal ({@code from <= to}).
 */
@ExtendWith(MockitoExtension.class)
class AuditQueryServiceTest {

    private static final Instant FROM = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-06-01T00:00:00Z");
    private static final Pageable PAGEABLE = PageRequest.of(0, 20);

    @Mock
    private AuditLogRepository auditLogRepository;

    @Test
    void should_return_filtered_audit_page_when_filters_valid() {
        // Given
        AuditQueryService service = new AuditQueryService(auditLogRepository);
        AuditLog entry = AuditLog.of(
                new AuditEntry(7L, "APPROVE_REQUEST", "Request", 15L, null), TO);
        given(auditLogRepository.search(eq(7L), eq("APPROVE_REQUEST"), eq(FROM), eq(TO), any()))
                .willReturn(new PageImpl<>(List.of(entry)));

        // When
        PageResponse<AuditLogEntryResponse> page =
                service.list(7L, "APPROVE_REQUEST", FROM, TO, PAGEABLE);

        // Then
        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).action()).isEqualTo("APPROVE_REQUEST");
        assertThat(page.content().get(0).actorEmployeeId()).isEqualTo(7L);
    }

    @Test
    void should_throw_invalid_date_range_when_audit_window_from_after_to() {
        // Given
        AuditQueryService service = new AuditQueryService(auditLogRepository);

        // When / Then: from posterior a to -> 400 (InvalidDateRangeException) sin consultar
        assertThatThrownBy(() -> service.list(null, null, TO, FROM, PAGEABLE))
                .isInstanceOf(InvalidDateRangeException.class);
        verify(auditLogRepository, never()).search(any(), any(), any(), any(), any());
    }

    @Test
    void should_query_without_filters_when_all_null() {
        // Given
        AuditQueryService service = new AuditQueryService(auditLogRepository);
        given(auditLogRepository.search(any(), any(), any(), any(), any()))
                .willReturn(new PageImpl<>(List.of()));

        // When
        PageResponse<AuditLogEntryResponse> page = service.list(null, null, null, null, PAGEABLE);

        // Then
        assertThat(page.content()).isEmpty();
    }
}
