package com.aleatica.parking.audit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.aleatica.parking.auth.domain.ClockPort;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests unitarios del caso de uso de purga: calcula el {@code cutoff} desde la ventana
 * configurable con un reloj fijo y suma las filas borradas de cada tabla, sin depender del
 * reloj real (S2925).
 */
@ExtendWith(MockitoExtension.class)
class RetentionPurgeServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-04T09:00:00Z");

    @Mock
    private RetentionPurgePort purgePort;

    @Mock
    private ClockPort clock;

    @Test
    void should_compute_cutoff_from_configured_years_when_retention_reconfigured() {
        // Given: ventana reconfigurada a 5 anos y reloj fijo
        RetentionPurgeService service = new RetentionPurgeService(purgePort, clock, 5);
        given(clock.now()).willReturn(NOW);

        // When
        service.purge();

        // Then: cutoff = ahora - 5 anos (no el default de 2) en cada tabla
        Instant expectedCutoff = Instant.parse("2021-07-04T09:00:00Z");
        LocalDate expectedCutoffDate = LocalDate.parse("2021-07-04");
        then(purgePort).should().purgeAuditLog(expectedCutoff);
        then(purgePort).should().purgeLoginLog(expectedCutoff);
        then(purgePort).should().purgeClosedRequests(expectedCutoff);
        then(purgePort).should().purgeReleases(expectedCutoffDate);
        then(purgePort).should().purgeVisitorReservations(expectedCutoffDate);
    }

    @Test
    void should_compute_cutoff_from_default_two_years_and_sum_deleted() {
        // Given: ventana por defecto (2 anos)
        RetentionPurgeService service = new RetentionPurgeService(purgePort, clock, 2);
        given(clock.now()).willReturn(NOW);
        given(purgePort.purgeAuditLog(any())).willReturn(2500);
        given(purgePort.purgeLoginLog(any())).willReturn(10);
        given(purgePort.purgeClosedRequests(any())).willReturn(5);
        given(purgePort.purgeReleases(anyDate())).willReturn(3);
        given(purgePort.purgeVisitorReservations(anyDate())).willReturn(2);

        // When
        int deleted = service.purge();

        // Then: cutoff a 2 anos y suma total
        ArgumentCaptor<Instant> cutoff = ArgumentCaptor.forClass(Instant.class);
        then(purgePort).should().purgeAuditLog(cutoff.capture());
        assertThat(cutoff.getValue()).isEqualTo(Instant.parse("2024-07-04T09:00:00Z"));
        assertThat(deleted).isEqualTo(2520);
    }

    private static Instant any() {
        return org.mockito.ArgumentMatchers.any();
    }

    private static LocalDate anyDate() {
        return org.mockito.ArgumentMatchers.any();
    }
}
