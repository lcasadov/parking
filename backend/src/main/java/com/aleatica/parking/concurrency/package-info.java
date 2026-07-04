/**
 * Utilidades transversales de concurrencia de persistencia.
 *
 * <p>Contiene {@link com.aleatica.parking.concurrency.ConcurrencyRetry}, el reintento
 * acotado que convierte a una victima de deadlock (SQL Server error 1205) en el mismo 409
 * controlado que produce la violacion de clave duplicada, evitando que una carrera de
 * insercion concurrente aflore como 500 (issue #57).</p>
 */
package com.aleatica.parking.concurrency;
