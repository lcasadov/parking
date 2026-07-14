/**
 * Modulo del ajuste global del sistema (arquitectura hexagonal, change
 * {@code request-auto-assignment}): el modelo de dominio
 * {@link com.aleatica.parking.systemsettings.domain.SystemSettings} y su puerto de salida
 * {@link com.aleatica.parking.systemsettings.domain.SystemSettingsRepositoryPort}, el adaptador
 * de persistencia ({@code systemsettings.infrastructure}: entidad JPA de fila unica, repositorio
 * Spring Data, mapper y adaptador), los casos de uso
 * ({@link com.aleatica.parking.systemsettings.application.SystemSettingsService}) y el adaptador
 * web ({@link com.aleatica.parking.systemsettings.SystemSettingsController}).
 *
 * <p>Es un singleton (fila unica {@code id = 1}, garantizada por {@code CHECK (id = 1)}) que
 * expone el parametro global {@code approvalMode} ({@code MANUAL}/{@code AUTOMATIC}). El ADMIN lo
 * lee y lo cambia en caliente ({@code GET}/{@code PUT /api/v1/admin/settings}); el alta de
 * solicitudes ({@code request}) lo consulta para ramificar entre el flujo manual (nace
 * {@code PENDING}) y el automatico (nace {@code APPROVED}). El cambio de modo se audita
 * {@code AFTER_COMMIT} via un puerto de auditoria (consolidacion de {@code audit-retention}).</p>
 */
package com.aleatica.parking.systemsettings;
