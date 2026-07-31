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
 * Entrada del histórico de cambios de un vehículo de empleado (tabla
 * {@code dbo.employee_vehicle_history}, change {@code employee-vehicle-self-service}, Fase 2).
 * Adaptador de salida: nunca se expone en la capa web (S4684); se proyecta a DTO.
 */
@Entity
@Table(name = "employee_vehicle_history")
public class EmployeeVehicleHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private VehicleHistoryEventType eventType;

    @Column(name = "actor_employee_id")
    private Long actorEmployeeId;

    @Column(name = "actor_role", length = 20)
    private String actorRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private VehicleStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 20)
    private VehicleStatus toStatus;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "snapshot_json")
    private String snapshotJson;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected EmployeeVehicleHistory() {
        // Requerido por JPA.
    }

    private EmployeeVehicleHistory(
            Long vehicleId, VehicleHistoryEventType eventType, Long actorEmployeeId, String actorRole,
            VehicleStatus fromStatus, VehicleStatus toStatus, String note, String snapshotJson) {
        this.vehicleId = vehicleId;
        this.eventType = eventType;
        this.actorEmployeeId = actorEmployeeId;
        this.actorRole = actorRole;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.note = note;
        this.snapshotJson = snapshotJson;
    }

    /** Crea una entrada del histórico (la fecha la fija la BD). */
    public static EmployeeVehicleHistory of(
            Long vehicleId, VehicleHistoryEventType eventType, Long actorEmployeeId, String actorRole,
            VehicleStatus fromStatus, VehicleStatus toStatus, String note, String snapshotJson) {
        return new EmployeeVehicleHistory(
                vehicleId, eventType, actorEmployeeId, actorRole, fromStatus, toStatus, note, snapshotJson);
    }

    public Long getId() {
        return id;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public VehicleHistoryEventType getEventType() {
        return eventType;
    }

    public Long getActorEmployeeId() {
        return actorEmployeeId;
    }

    public String getActorRole() {
        return actorRole;
    }

    public VehicleStatus getFromStatus() {
        return fromStatus;
    }

    public VehicleStatus getToStatus() {
        return toStatus;
    }

    public String getNote() {
        return note;
    }

    public String getSnapshotJson() {
        return snapshotJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
