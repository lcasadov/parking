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
    private Long updatedById;
    private Instant updatedAt;

    private SystemSettings(
            short id, ApprovalMode approvalMode, Long updatedById, Instant updatedAt) {
        this.id = id;
        this.approvalMode = approvalMode;
        this.updatedById = updatedById;
        this.updatedAt = updatedAt;
    }

    /**
     * Reconstituye el ajuste a partir de su estado persistido (uso del mapper de
     * infraestructura; no aplica reglas de transicion).
     *
     * @param id           identificador de la fila (siempre {@link #SINGLETON_ID})
     * @param approvalMode modo de aprobacion global
     * @param updatedById  empleado (ADMIN) que hizo el ultimo cambio; {@code null} si nunca
     * @param updatedAt    instante del ultimo cambio (UTC); {@code null} si nunca
     * @return el ajuste reconstituido
     */
    public static SystemSettings restore(
            short id, ApprovalMode approvalMode, Long updatedById, Instant updatedAt) {
        return new SystemSettings(id, approvalMode, updatedById, updatedAt);
    }

    /**
     * Devuelve el ajuste por defecto (modo {@link ApprovalMode#MANUAL}) para el caso en que la
     * fila unica aun no exista; garantiza que el sistema no asuma silenciosamente
     * {@code AUTOMATIC}.
     *
     * @return un ajuste transitorio con modo {@code MANUAL} y sin trazabilidad
     */
    public static SystemSettings defaults() {
        return new SystemSettings(SINGLETON_ID, ApprovalMode.MANUAL, null, null);
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

    public short getId() {
        return id;
    }

    public ApprovalMode getApprovalMode() {
        return approvalMode;
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
