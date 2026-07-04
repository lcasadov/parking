package com.aleatica.parking.export;

import com.aleatica.parking.auth.domain.ClockPort;
import com.aleatica.parking.export.application.ExportRateLimitExceededException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Limitador de tasa de exportaciones: 5 por minuto y usuario ({@code docs/security-design.md}
 * §7, spec Req 4), para frenar la exfiltracion masiva de datos personales.
 *
 * <p>Ventana deslizante en memoria (Fase 1, un unico WAR): por cada usuario guarda las marcas
 * de tiempo de sus exportaciones recientes; al pedir una nueva descarta las que caen fuera de
 * la ventana y, si quedan {@link #MAX_REQUESTS} o mas, rechaza con
 * {@link ExportRateLimitExceededException} (429). El instante lo aporta un {@link ClockPort}
 * inyectable, de modo que el limite es determinista y testeable sin {@code Thread.sleep}
 * (S2925). Si el despliegue pasa a varias instancias, el contador en memoria dejaria de ser
 * global y requeriria un backend compartido (Redis), marcado como futuro en §7.</p>
 */
@Component
public class ExportRateLimiter {

    private static final int MAX_REQUESTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final String MSG_LIMIT =
            "Ha superado el limite de exportaciones permitido; intentelo de nuevo en un minuto";

    private final ClockPort clock;
    private final Map<String, Deque<Instant>> hitsByUser = new ConcurrentHashMap<>();

    /**
     * @param clock reloj inyectable (UTC) para la ventana deslizante
     */
    public ExportRateLimiter(ClockPort clock) {
        this.clock = clock;
    }

    /**
     * Registra una exportacion del usuario y verifica que no supere el limite de la ventana.
     *
     * @param userKey clave del usuario (login de la sesion)
     * @throws ExportRateLimitExceededException si ya alcanzo el maximo dentro de la ventana
     */
    public void acquire(String userKey) {
        Instant now = clock.now();
        Instant threshold = now.minus(WINDOW);
        Deque<Instant> hits = hitsByUser.computeIfAbsent(userKey, key -> new ArrayDeque<>());
        synchronized (hits) {
            evictOlderThan(hits, threshold);
            if (hits.size() >= MAX_REQUESTS) {
                throw new ExportRateLimitExceededException(MSG_LIMIT);
            }
            hits.addLast(now);
        }
    }

    /**
     * Vacia el estado del limitador (aislamiento entre tests de integracion: evita que el conteo
     * de un test filtre al siguiente segun el orden de ejecucion).
     */
    public void reset() {
        hitsByUser.clear();
    }

    private void evictOlderThan(Deque<Instant> hits, Instant threshold) {
        while (!hits.isEmpty() && !hits.peekFirst().isAfter(threshold)) {
            hits.pollFirst();
        }
    }
}
