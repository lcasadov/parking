package com.aleatica.parking.auth;

import com.aleatica.parking.auth.domain.ClockPort;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Adaptador de {@link ClockPort} respaldado por el reloj del sistema en UTC.
 *
 * <p>Es el unico punto que consulta la hora real; los casos de uso dependen del
 * puerto, lo que permite fijar el reloj en los tests sin {@code Thread.sleep}.</p>
 */
@Component
public class SystemClockAdapter implements ClockPort {

    private final Clock clock = Clock.systemUTC();

    @Override
    public Instant now() {
        return clock.instant();
    }
}
