package com.aleatica.parking.systemsettings.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Modelo de dominio del ajuste global del sistema (arquitectura hexagonal, change
 * {@code request-auto-assignment}).
 *
 * <p>Representa la <strong>fila unica</strong> de {@code system_settings} (patron singleton,
 * {@code id = 1}): el modo de aprobacion global y la trazabilidad de la ultima modificacion
 * (actor y marca de tiempo). Es <strong>libre de framework</strong> (sin JPA, sin Spring): la
 * persistencia la resuelve un adaptador de infraestructura que mapea este modelo a/desde una
 * entidad JPA. Las referencias a otras tablas se guardan como identificadores ({@code Long}).</p>
 */
public class SystemSettings {

    /** Identificador constante de la unica fila del singleton ({@code CHECK (id = 1)}). */
    public static final short SINGLETON_ID = 1;

    private final short id;
    private ApprovalMode approvalMode;
    private String parkingAddress;
    private boolean weekendReservable;
    private Long updatedById;
    private Instant updatedAt;

    private SystemSettings(
            short id, ApprovalMode approvalMode, String parkingAddress, boolean weekendReservable,
            Long updatedById, Instant updatedAt) {
        this.id = id;
        this.approvalMode = approvalMode;
        this.parkingAddress = parkingAddress;
        this.weekendReservable = weekendReservable;
        this.updatedById = updatedById;
        this.updatedAt = updatedAt;
    }

    /**
     * Reconstituye el ajuste a partir de su estado persistido (uso del mapper de
     * infraestructura; no aplica reglas de transicion).
     *
     * @param id                identificador de la fila (siempre {@link #SINGLETON_ID})
     * @param approvalMode      modo de aprobacion global
     * @param parkingAddress    direccion postal del parking para "Ir al parking"; {@code null} si sin
     *                          configurar (change {@code reservas-employee-admin-reassign})
     * @param weekendReservable si se permiten reservas en sabado/domingo (change
     *                          {@code reservas-employee-admin-reassign}); por defecto {@code false}
     * @param updatedById       empleado (ADMIN) que hizo el ultimo cambio; {@code null} si nunca
     * @param updatedAt         instante del ultimo cambio (UTC); {@code null} si nunca
     * @return el ajuste reconstituido
     */
    public static SystemSettings restore(
            short id, ApprovalMode approvalMode, String parkingAddress, boolean weekendReservable,
            Long updatedById, Instant updatedAt) {
        return new SystemSettings(
                id, approvalMode, parkingAddress, weekendReservable, updatedById, updatedAt);
    }

    /**
     * Devuelve el ajuste por defecto (modo {@link ApprovalMode#MANUAL}, sin direccion de parking,
     * sin reservas de fin de semana) para el caso en que la fila unica aun no exista; garantiza que
     * el sistema no asuma silenciosamente {@code AUTOMATIC} ni reservas de fin de semana.
     *
     * @return un ajuste transitorio con los valores por defecto y sin trazabilidad
     */
    public static SystemSettings defaults() {
        return new SystemSettings(SINGLETON_ID, ApprovalMode.MANUAL, null, false, null, null);
    }

    /**
     * Cambia el modo de aprobacion global, registrando el actor y el instante del cambio.
     *
     * @param mode    nuevo modo de aprobacion
     * @param actorId empleado (ADMIN) que ejecuta el cambio
     * @param now     instante del cambio (UTC)
     */
    public void changeMode(ApprovalMode mode, Long actorId, Instant now) {
        this.approvalMode = mode;
        this.updatedById = actorId;
        this.updatedAt = now;
    }

    /**
     * Cambia la direccion postal del parking (usada por el boton "Ir al parking" del empleado, que
     * abre Google Maps), registrando el actor y el instante del cambio (change
     * {@code reservas-employee-admin-reassign}). Un valor {@code null} o en blanco borra la
     * direccion configurada.
     *
     * @param parkingAddress nueva direccion postal; {@code null}/blanco para dejarla sin configurar
     * @param actorId        empleado (ADMIN) que ejecuta el cambio
     * @param now            instante del cambio (UTC)
     */
    public void changeParkingAddress(String parkingAddress, Long actorId, Instant now) {
        this.parkingAddress = normalizeAddress(parkingAddress);
        this.updatedById = actorId;
        this.updatedAt = now;
    }

    private static String normalizeAddress(String parkingAddress) {
        if (parkingAddress == null) {
            return null;
        }
        String trimmed = parkingAddress.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Cambia si se permiten reservas en fin de semana (sabado/domingo), registrando el actor y el
     * instante del cambio (change {@code reservas-employee-admin-reassign}).
     *
     * @param weekendReservable {@code true} para permitir reservas de fin de semana
     * @param actorId           empleado (ADMIN) que ejecuta el cambio
     * @param now               instante del cambio (UTC)
     */
    public void changeWeekendReservable(boolean weekendReservable, Long actorId, Instant now) {
        this.weekendReservable = weekendReservable;
        this.updatedById = actorId;
        this.updatedAt = now;
    }

    public short getId() {
        return id;
    }

    public ApprovalMode getApprovalMode() {
        return approvalMode;
    }

    public String getParkingAddress() {
        return parkingAddress;
    }

    public boolean isWeekendReservable() {
        return weekendReservable;
    }

    public Long getUpdatedById() {
        return updatedById;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SystemSettings settings)) {
            return false;
        }
        return id == settings.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
