package com.aleatica.parking.request;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entidad de persistencia de una solicitud puntual (tabla {@code dbo.requests}).
 *
 * <p>Solicitud de un empleado para una plaza de parking en una fecha concreta, con
 * ciclo de vida de aprobacion/rechazo gestionado por el {@code ADMIN}. Nace en
 * {@link RequestStatus#PENDING} con {@code parking_space_id = NULL}; la plaza se
 * asigna solo al aprobar.</p>
 *
 * <p>Es un adaptador de salida: nunca se expone en la capa web (S4684); el
 * controlador trabaja con DTOs. Las referencias a otras tablas se guardan como
 * identificadores ({@code Long}), evitando por diseno consultas N+1 y manteniendo
 * el agregado desacoplado. Las transiciones de estado son metodos de dominio que
 * garantizan la invariante (solo desde {@code PENDING}).</p>
 */
@Entity
@Table(name = "requests")
public class Request {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "requested_date", nullable = false)
    private LocalDate requestedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private RequestStatus status;

    @Column(name = "parking_space_id")
    private Long parkingSpaceId;

    @Column(name = "approval_note", length = 500)
    private String approvalNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "rejection_reason_code", length = 40)
    private RejectionReasonCode rejectionReasonCode;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "resolved_by_id")
    private Long resolvedById;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Constructor sin argumentos requerido por JPA. */
    protected Request() {
        // JPA
    }

    /**
     * Da de alta una solicitud nueva en estado {@link RequestStatus#PENDING} sin plaza.
     *
     * @param employeeId    empleado solicitante
     * @param requestedDate fecha solicitada
     * @param now           instante de creacion (UTC)
     * @return la solicitud nueva, aun no persistida
     */
    public static Request create(Long employeeId, LocalDate requestedDate, Instant now) {
        Request request = new Request();
        request.employeeId = employeeId;
        request.requestedDate = requestedDate;
        request.status = RequestStatus.PENDING;
        request.parkingSpaceId = null;
        request.createdAt = now;
        return request;
    }

    /**
     * @return {@code true} si la solicitud esta en estado {@link RequestStatus#PENDING}.
     */
    public boolean isPending() {
        return status == RequestStatus.PENDING;
    }

    /**
     * Aprueba la solicitud asignando plaza, resolutor y nota opcional.
     *
     * @param parkingSpaceId plaza asignada
     * @param resolvedById   empleado (ADMIN) que resuelve
     * @param approvalNote   nota libre del administrador (puede ser {@code null})
     * @param now            instante de resolucion (UTC)
     */
    public void approve(Long parkingSpaceId, Long resolvedById, String approvalNote, Instant now) {
        this.status = RequestStatus.APPROVED;
        this.parkingSpaceId = parkingSpaceId;
        this.approvalNote = approvalNote;
        this.resolvedById = resolvedById;
        this.resolvedAt = now;
    }

    /**
     * Rechaza la solicitud con un codigo del catalogo y texto libre opcional.
     *
     * @param reasonCode   codigo del catalogo de rechazo
     * @param reason       texto libre (obligatorio si {@code reasonCode = OTHER})
     * @param resolvedById empleado (ADMIN) que resuelve
     * @param now          instante de resolucion (UTC)
     */
    public void reject(RejectionReasonCode reasonCode, String reason, Long resolvedById, Instant now) {
        this.status = RequestStatus.REJECTED;
        this.rejectionReasonCode = reasonCode;
        this.rejectionReason = reason;
        this.resolvedById = resolvedById;
        this.resolvedAt = now;
    }

    /**
     * Cancela la solicitud (transicion a {@link RequestStatus#CANCELLED}) a peticion
     * del propio empleado.
     */
    public void cancel() {
        this.status = RequestStatus.CANCELLED;
    }

    public Long getId() {
        return id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public LocalDate getRequestedDate() {
        return requestedDate;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public Long getParkingSpaceId() {
        return parkingSpaceId;
    }

    public String getApprovalNote() {
        return approvalNote;
    }

    public RejectionReasonCode getRejectionReasonCode() {
        return rejectionReasonCode;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public Long getResolvedById() {
        return resolvedById;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Request request)) {
            return false;
        }
        return id != null && id.equals(request.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
