package com.aleatica.parking.concurrency;

import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Component;

/**
 * Reintento acotado de operaciones que pueden ser elegidas como <em>victima de
 * deadlock</em> por el motor de BD durante una insercion concurrente sobre un indice
 * unico filtrado (issue #57).
 *
 * <p>SQL Server puede resolver la carrera de dos inserciones simultaneas eligiendo una
 * transaccion como victima de deadlock (error 1205, {@code SQLState 40001}) en lugar de
 * devolver la violacion limpia de clave duplicada (2601/2627). La victima se traduce a
 * {@link PessimisticLockingFailureException} (Spring) — que sin tratamiento acabaria en
 * un 500 — cuando el resultado correcto es el mismo 409 controlado que produce el
 * duplicado.</p>
 *
 * <p><strong>Estrategia:</strong> reintentar la operacion completa un numero acotado de
 * veces. El reintento debe ejecutarse <em>fuera</em> de la transaccion (se invoca desde el
 * adaptador web, no dentro del servicio {@code @Transactional}), de modo que cada intento
 * abra una transaccion nueva. Tras el commit del ganador, el reintento del perdedor
 * encuentra la fila comprometida y produce, por la via existente (comprobacion previa del
 * caso de uso o violacion del indice unico), el 409 identico al del duplicado — con el
 * mismo codigo, mensaje y campos. No se usa {@code Thread.sleep} (S2925): tras el commit
 * del ganador el reintento es determinista y no vuelve a competir.</p>
 */
@Component
public class ConcurrencyRetry {

    /** Numero total de intentos (1 inicial + reintentos). */
    static final int MAX_ATTEMPTS = 3;

    private static final Logger LOG = LoggerFactory.getLogger(ConcurrencyRetry.class);

    private static final String MSG_RETRY =
            "Victima de deadlock/bloqueo en insercion concurrente (intento {}/{}); reintentando";

    /**
     * Ejecuta la operacion reintentandola si el motor la elige como victima de deadlock o
     * no puede adquirir el bloqueo. Cualquier otra excepcion (o el resultado normal) se
     * propaga sin alterar. Si el ultimo intento vuelve a fallar por bloqueo, la excepcion
     * se propaga y el {@code GlobalExceptionHandler} la mapea a 409 (nunca 500).
     *
     * @param operation operacion transaccional a ejecutar (invoca un metodo
     *                  {@code @Transactional} a traves del proxy, abriendo transaccion nueva
     *                  en cada intento)
     * @param <T>       tipo del resultado de la operacion
     * @return el resultado de la operacion cuando culmina sin conflicto de bloqueo
     */
    public <T> T execute(Supplier<T> operation) {
        for (int attempt = 1; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                return operation.get();
            } catch (PessimisticLockingFailureException ex) {
                LOG.warn(MSG_RETRY, attempt, MAX_ATTEMPTS);
            }
        }
        // Ultimo intento sin red: si vuelve a fallar por bloqueo, se propaga como
        // ConcurrencyFailureException y el GlobalExceptionHandler la traduce a 409.
        return operation.get();
    }
}
