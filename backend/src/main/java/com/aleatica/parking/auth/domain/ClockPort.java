package com.aleatica.parking.auth.domain;

import java.time.Instant;

/**
 * Puerto de salida para obtener el instante actual.
 *
 * <p>Inyectable para poder fijar el reloj en los tests (ventana de bloqueo de
 * 15 min, rotacion) sin {@code Thread.sleep} (S2925). En produccion lo implementa
 * un adaptador respaldado por {@link java.time.Clock#systemUTC()}.</p>
 */
public interface ClockPort {

    /**
     * @return el instante actual en UTC
     */
    Instant now();
}
