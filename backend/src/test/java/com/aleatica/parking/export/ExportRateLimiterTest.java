package com.aleatica.parking.export;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aleatica.parking.auth.domain.ClockPort;
import com.aleatica.parking.export.application.ExportRateLimitExceededException;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests unitarios del limitador de tasa de exportaciones (spec Req 4): permite 5 por ventana de
 * un minuto y rechaza la sexta con {@code 429}; la ventana desliza con el reloj inyectable (sin
 * {@code Thread.sleep}, S2925); {@code reset()} vacia el estado; el limite es por usuario.
 */
class ExportRateLimiterTest {

    private static final String USER = "jperez";
    private static final Instant T0 = Instant.parse("2026-07-04T10:00:00Z");

    private MutableClock clock;
    private ExportRateLimiter limiter;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(T0);
        limiter = new ExportRateLimiter(clock);
    }

    @Test
    void shouldAllowFiveAndRejectSixth_whenWithinOneMinute() {
        // Act: cinco exportaciones dentro de la misma ventana
        for (int i = 0; i < 5; i++) {
            assertThatCode(() -> limiter.acquire(USER)).doesNotThrowAnyException();
        }

        // Assert (1.9): la sexta en la misma ventana se rechaza
        assertThatThrownBy(() -> limiter.acquire(USER))
                .isInstanceOf(ExportRateLimitExceededException.class);
    }

    @Test
    void shouldAllowAgain_whenWindowHasSlid() {
        // Arrange: consume el cupo
        for (int i = 0; i < 5; i++) {
            limiter.acquire(USER);
        }

        // Act: avanza el reloj mas alla de la ventana (61 s)
        clock.advanceSeconds(61);

        // Assert: vuelve a permitir (las marcas antiguas salieron de la ventana)
        assertThatCode(() -> limiter.acquire(USER)).doesNotThrowAnyException();
    }

    @Test
    void shouldTrackLimitPerUser_whenDifferentUsers() {
        // Arrange: un usuario agota su cupo
        for (int i = 0; i < 5; i++) {
            limiter.acquire(USER);
        }

        // Act / Assert: otro usuario no se ve afectado
        assertThatCode(() -> limiter.acquire("otro")).doesNotThrowAnyException();
    }

    @Test
    void shouldClearState_whenReset() {
        // Arrange
        for (int i = 0; i < 5; i++) {
            limiter.acquire(USER);
        }

        // Act
        limiter.reset();

        // Assert: tras el reset el cupo vuelve a estar disponible
        assertThatCode(() -> limiter.acquire(USER)).doesNotThrowAnyException();
    }

    /** Reloj mutable de test: permite avanzar el instante sin {@code Thread.sleep} (S2925). */
    private static final class MutableClock implements ClockPort {
        private Instant now;

        private MutableClock(Instant start) {
            this.now = start;
        }

        private void advanceSeconds(long seconds) {
            now = now.plusSeconds(seconds);
        }

        @Override
        public Instant now() {
            return now;
        }
    }
}
