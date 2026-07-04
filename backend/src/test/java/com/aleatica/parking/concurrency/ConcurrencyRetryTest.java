package com.aleatica.parking.concurrency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DeadlockLoserDataAccessException;

/**
 * Tests unitarios del reintento acotado {@link ConcurrencyRetry}: simula el camino de la
 * victima de deadlock sin depender de la BD, comprobando que la operacion se reintenta,
 * que el resultado del reintento se devuelve, que las excepciones ajenas no se reintentan
 * y que un bloqueo persistente se propaga (para que el manejador global lo mapee a 409).
 */
class ConcurrencyRetryTest {

    private final ConcurrencyRetry retry = new ConcurrencyRetry();

    @Test
    void shouldReturnResult_whenFirstAttemptSucceeds() {
        // Arrange
        AtomicInteger calls = new AtomicInteger();

        // Act
        String result = retry.execute(() -> {
            calls.incrementAndGet();
            return "ok";
        });

        // Assert: sin conflicto, una sola ejecucion
        assertThat(result).isEqualTo("ok");
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void shouldRetryAndSucceed_whenFirstAttemptIsDeadlockVictim() {
        // Arrange: el primer intento es victima de deadlock (1205); el reintento (ganador ya
        // comprometido) tiene exito, como ocurre tras el commit del ganador.
        AtomicInteger calls = new AtomicInteger();
        Supplier<String> operation = () -> {
            if (calls.incrementAndGet() == 1) {
                throw new CannotAcquireLockException("deadlock victim 1205");
            }
            return "recovered";
        };

        // Act
        String result = retry.execute(operation);

        // Assert: exactamente un reintento y resultado del segundo intento
        assertThat(result).isEqualTo("recovered");
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void shouldRetryDeadlockLoser_whenSpringTranslatesToDeadlockLoserException() {
        // Arrange: variante DeadlockLoserDataAccessException (tambien PessimisticLocking...)
        AtomicInteger calls = new AtomicInteger();
        Supplier<Integer> operation = () -> {
            if (calls.incrementAndGet() < 2) {
                throw new DeadlockLoserDataAccessException("loser", null);
            }
            return calls.get();
        };

        // Act
        Integer result = retry.execute(operation);

        // Assert
        assertThat(result).isEqualTo(2);
    }

    @Test
    void shouldNotRetry_whenExceptionIsNotLockFailure() {
        // Arrange: un duplicado limpio (DataIntegrityViolationException) NO es un deadlock;
        // debe propagarse tal cual, sin reintentos (lo mapea handleDataIntegrity a 409).
        AtomicInteger calls = new AtomicInteger();
        Supplier<String> operation = () -> {
            calls.incrementAndGet();
            throw new DataIntegrityViolationException("Violation of UX_requests_space_date_approved");
        };

        // Act / Assert
        assertThatThrownBy(() -> retry.execute(operation))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void shouldPropagateLockFailure_whenEveryAttemptDeadlocks() {
        // Arrange: bloqueo persistente en los MAX_ATTEMPTS intentos.
        AtomicInteger calls = new AtomicInteger();
        Supplier<String> operation = () -> {
            calls.incrementAndGet();
            throw new CannotAcquireLockException("persistent deadlock");
        };

        // Act / Assert: se propaga (el GlobalExceptionHandler lo traduce a 409, nunca 500)
        assertThatThrownBy(() -> retry.execute(operation))
                .isInstanceOf(CannotAcquireLockException.class);
        assertThat(calls.get()).isEqualTo(ConcurrencyRetry.MAX_ATTEMPTS);
    }
}
