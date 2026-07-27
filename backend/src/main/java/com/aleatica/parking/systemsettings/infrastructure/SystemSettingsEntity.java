package com.aleatica.parking.systemsettings.infrastructure;

import com.aleatica.parking.systemsettings.domain.ApprovalMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entidad de persistencia (adaptador de salida) del ajuste global de sistema (tabla de fila
 * unica {@code dbo.system_settings}). Arquitectura hexagonal, change
 * {@code request-auto-assignment}.
 *
 * <p>Es la representacion JPA del singleton; el modelo de negocio vive en
 * {@link com.aleatica.parking.systemsettings.domain.SystemSettings} y el puente
 * entidad&harr;dominio lo resuelve {@link SystemSettingsMapper}. Nunca se expone en la capa web
 * (S4684); el controlador trabaja con DTOs. El {@code id} es {@code TINYINT} constante
 * ({@code = 1}, garantizado por {@code CHECK} en BD): no se genera, lo fija el mapper.</p>
 */
@Entity
@Table(name = "system_settings")
public class SystemSettingsEntity {

    @Id
    @Column(name = "id", nullable = false)
    private Byte id;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_mode", nullable = false, length = 10)
    private ApprovalMode approvalMode;

    @Column(name = "parking_address", length = 500)
    private String parkingAddress;

    @Column(name = "weekend_reservable", nullable = false)
    private boolean weekendReservable;

    @Column(name = "updated_by_id")
    private Long updatedById;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "updated_at")
    private Instant updatedAt;

    /** Constructor sin argumentos requerido por JPA. */
    protected SystemSettingsEntity() {
        // JPA
    }

    /**
     * Constructor de todos los campos usado por {@link SystemSettingsMapper}.
     *
     * @param id                identificador constante de la fila ({@code = 1})
     * @param approvalMode      modo de aprobacion global
     * @param parkingAddress    direccion postal del parking; {@code null} si sin configurar
     * @param weekendReservable si se permiten reservas en fin de semana
     * @param updatedById       empleado (ADMIN) que hizo el ultimo cambio; {@code null} si nunca
     * @param updatedAt         instante del ultimo cambio (UTC); {@code null} si nunca
     */
    public SystemSettingsEntity(
            Byte id, ApprovalMode approvalMode, String parkingAddress, boolean weekendReservable,
            Long updatedById, Instant updatedAt) {
        this.id = id;
        this.approvalMode = approvalMode;
        this.parkingAddress = parkingAddress;
        this.weekendReservable = weekendReservable;
        this.updatedById = updatedById;
        this.updatedAt = updatedAt;
    }

    public Byte getId() {
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
        if (!(other instanceof SystemSettingsEntity entity)) {
            return false;
        }
        return Objects.equals(id, entity.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
