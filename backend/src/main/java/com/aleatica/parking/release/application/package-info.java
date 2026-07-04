/**
 * Casos de uso y puertos del modulo de liberaciones.
 *
 * <p>{@link com.aleatica.parking.release.application.ReleaseService} orquesta la creacion
 * voluntaria y administrativa, el listado propio y la cancelacion, con la ventana temporal
 * ({@code ClockPort}), la resolucion de la plaza fija, la unicidad recurso+fecha y la
 * verificacion de pertenencia (BOLA). Las excepciones de negocio las traduce el manejador
 * global a {@code ApiError}. La auditoria se abstrae en
 * {@link com.aleatica.parking.release.application.ReleaseAuditPort} (adaptador de log en
 * Fase 1) y se dispara {@code AFTER_COMMIT} via
 * {@link com.aleatica.parking.release.application.ReleaseEventListener}.</p>
 */
package com.aleatica.parking.release.application;
