package com.aleatica.parking.audit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.aleatica.parking.audit.LoginLogRepository;
import com.aleatica.parking.audit.dto.LoginLogEntryResponse;
import com.aleatica.parking.auth.domain.LoginResult;
import com.aleatica.parking.employee.dto.PageResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Tests unitarios del caso de uso de consulta de logs de login: propaga el filtro por
 * {@code result} y valida la ventana temporal.
 */
@ExtendWith(MockitoExtension.class)
class LoginLogQueryServiceTest {

    private static final Instant FROM = Instant.parse("2026-06-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-06-23T00:00:00Z");
    private static final Pageable PAGEABLE = PageRequest.of(0, 20);

    @Mock
    private LoginLogRepository loginLogRepository;

    @Test
    void should_return_only_invalid_credentials_when_login_log_filtered_by_result() {
        // Given
        LoginLogQueryService service = new LoginLogQueryService(loginLogRepository);
        given(loginLogRepository.search(eq(LoginResult.INVALID_CREDENTIALS), eq(FROM), eq(TO), any()))
                .willReturn(emptyPage());

        // When
        PageResponse<LoginLogEntryResponse> page =
                service.list(LoginResult.INVALID_CREDENTIALS, FROM, TO, PAGEABLE);

        // Then: se consulta con el resultado filtrado (el repositorio aplica el criterio)
        assertThat(page.content()).isEmpty();
        verify(loginLogRepository).search(eq(LoginResult.INVALID_CREDENTIALS), eq(FROM), eq(TO), any());
    }

    @Test
    void should_throw_invalid_date_range_when_login_window_from_after_to() {
        // Given
        LoginLogQueryService service = new LoginLogQueryService(loginLogRepository);

        // When / Then
        assertThatThrownBy(() -> service.list(null, TO, FROM, PAGEABLE))
                .isInstanceOf(InvalidDateRangeException.class);
        verify(loginLogRepository, never()).search(any(), any(), any(), any());
    }

    private static Page<com.aleatica.parking.audit.LoginLog> emptyPage() {
        return new PageImpl<>(List.of());
    }
}
