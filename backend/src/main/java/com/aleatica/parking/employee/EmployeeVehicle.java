package com.aleatica.parking.employee;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entidad de persistencia de un vehiculo de un empleado (tabla {@code dbo.employee_vehicles}).
 *
 * <p>Relacion 1:N con {@link Employee} por {@code employee_id}: un empleado puede tener varios
 * vehiculos. La matricula ({@code license_plate}) es obligatoria y unica por empleado; marca,
 * modelo y color son opcionales. Es un adaptador de salida: nunca se expone en la capa web
 * (S4684); el controlador trabaja con DTOs.</p>
 */
@Entity
@Table(name = "employee_vehicles")
public class EmployeeVehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "license_plate", nullable = false, length = 15)
    private String licensePlate;

    @Column(name = "brand", length = 60)
    private String brand;

    @Column(name = "model", length = 60)
    private String model;

    @Column(name = "color", length = 30)
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VehicleStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 20)
    private VehicleStatus previousStatus;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected EmployeeVehicle() {
        // Requerido por JPA.
    }

    /**
     * Crea un vehiculo nuevo para un empleado con el estado inicial indicado. La matricula ya debe
     * venir normalizada (trim + mayusculas) por el servicio; marca/modelo/color pueden ser
     * {@code null}. Alta por el ADMIN -> {@code APPROVED}; alta por el empleado -> {@code PENDING}.
     */
    public static EmployeeVehicle create(
            Long employeeId, String licensePlate, String brand, String model, String color,
            VehicleStatus status) {
        EmployeeVehicle vehicle = new EmployeeVehicle();
        vehicle.employeeId = employeeId;
        vehicle.licensePlate = licensePlate;
        vehicle.brand = brand;
        vehicle.model = model;
        vehicle.color = color;
        vehicle.status = status;
        return vehicle;
    }

    /** Aplica los cambios de una edicion (matricula ya normalizada por el servicio). */
    public void update(String licensePlate, String brand, String model, String color) {
        this.licensePlate = licensePlate;
        this.brand = brand;
        this.model = model;
        this.color = color;
    }

    /**
     * Devuelve el vehiculo a {@code PENDING} (edicion por el propio empleado), limpiando el
     * resultado de una validacion previa (motivo de rechazo y auditoria de revision).
     */
    public void markPending() {
        this.status = VehicleStatus.PENDING;
        this.previousStatus = null;
        this.rejectionReason = null;
        this.reviewedAt = null;
        this.reviewedBy = null;
    }

    /** Marca el vehiculo como "en tramite" por un administrador (Fase 2). */
    public void markInProgress(Long reviewerId, Instant reviewedAt) {
        this.status = VehicleStatus.IN_PROGRESS;
        this.previousStatus = null;
        this.rejectionReason = null;
        this.reviewedBy = reviewerId;
        this.reviewedAt = reviewedAt;
    }

    /** Marca el vehiculo como aprobado por un administrador (Fase 2). */
    public void approve(Long reviewerId, Instant reviewedAt) {
        this.status = VehicleStatus.APPROVED;
        this.previousStatus = null;
        this.rejectionReason = null;
        this.reviewedBy = reviewerId;
        this.reviewedAt = reviewedAt;
    }

    /** Marca el vehiculo como rechazado con motivo por un administrador (Fase 2). */
    public void reject(String reason, Long reviewerId, Instant reviewedAt) {
        this.status = VehicleStatus.REJECTED;
        this.previousStatus = null;
        this.rejectionReason = reason;
        this.reviewedBy = reviewerId;
        this.reviewedAt = reviewedAt;
    }

    /**
     * El empleado pide borrar un vehiculo que estaba {@code IN_PROGRESS} o {@code APPROVED}: no se
     * borra, queda {@code PENDING_DELETION} recordando el estado previo para poder restaurarlo.
     */
    public void requestDeletion() {
        this.previousStatus = this.status;
        this.status = VehicleStatus.PENDING_DELETION;
    }

    /** El administrador restaura un vehiculo {@code PENDING_DELETION} a su estado previo (Fase 2). */
    public void restore() {
        this.status = this.previousStatus == null ? VehicleStatus.PENDING : this.previousStatus;
        this.previousStatus = null;
    }

    /** Indica si el vehiculo puede recibir una decision de validacion (pendiente o en tramite). */
    public boolean isReviewable() {
        return this.status == VehicleStatus.PENDING || this.status == VehicleStatus.IN_PROGRESS;
    }

    public Long getId() {
        return id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    public String getColor() {
        return color;
    }

    public VehicleStatus getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public Long getReviewedBy() {
        return reviewedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
