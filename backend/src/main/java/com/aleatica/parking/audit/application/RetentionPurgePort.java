package com.aleatica.parking.audit.application;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Puerto de salida para la purga por lotes de datos historicos (RGPD, 2 anos por defecto).
 *
 * <p>Cada metodo borra las filas de una tabla anteriores al {@code cutoff} en lotes de tamano
 * acotado ({@code DELETE TOP (1000)} en bucle hasta drenar), devolviendo el total borrado.
 * El borrado por lotes evita bloqueos largos de tabla; cada lote es una transaccion corta
 * (design §Decisions). Nunca toca entidades vivas: solo datos historicos por su criterio de
 * fecha.</p>
 */
public interface RetentionPurgePort {

    /**
     * Purga {@code audit_log} por {@code occurred_at < cutoff}.
     *
     * @param cutoff instante limite (exclusivo)
     * @return total de filas borradas
     */
    int purgeAuditLog(Instant cutoff);

    /**
     * Purga {@code login_log} por {@code occurred_at < cutoff}.
     *
     * @param cutoff instante limite (exclusivo)
     * @return total de filas borradas
     */
    int purgeLoginLog(Instant cutoff);

    /**
     * Purga las solicitudes cerradas ({@code status <> 'PENDING'}) por {@code created_at < cutoff}.
     *
     * @param cutoff instante limite (exclusivo)
     * @return total de filas borradas
     */
    int purgeClosedRequests(Instant cutoff);

    /**
     * Purga {@code releases} por {@code release_date < cutoffDate}.
     *
     * @param cutoffDate fecha limite (exclusiva)
     * @return total de filas borradas
     */
    int purgeReleases(LocalDate cutoffDate);

    /**
     * Purga {@code visitor_reservations} por {@code reservation_date < cutoffDate}.
     *
     * @param cutoffDate fecha limite (exclusiva)
     * @return total de filas borradas
     */
    int purgeVisitorReservations(LocalDate cutoffDate);
}
