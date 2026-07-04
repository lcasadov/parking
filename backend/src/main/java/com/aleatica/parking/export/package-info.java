/**
 * Capability {@code exports}: exportacion transversal de datos a CSV/XLSX.
 *
 * <p>Arquitectura hexagonal. El adaptador web ({@link com.aleatica.parking.export.ExportController})
 * valida el formato, aplica el limite de tasa ({@link com.aleatica.parking.export.ExportRateLimiter})
 * y serializa en streaming con el {@link com.aleatica.parking.export.ExportWriterPort} resuelto por
 * {@link com.aleatica.parking.export.ExportWriters} (adaptadores CSV y XLSX). El caso de uso
 * ({@link com.aleatica.parking.export.application.ExportService}) decide que datos y para quien,
 * excluye campos sensibles de credenciales, aplica la comprobacion de objeto (BOLA) en las
 * exportaciones propias y registra cada exportacion en {@code audit_log}. La sanitizacion de
 * inyeccion de formulas la centraliza {@link com.aleatica.parking.export.FormulaSanitizer}.</p>
 */
package com.aleatica.parking.export;
