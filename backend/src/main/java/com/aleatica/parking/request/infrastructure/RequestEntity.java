package com.aleatica.parking.request.infrastructure;

import com.aleatica.parking.request.domain.RejectionReasonCode;
import com.aleatica.parking.request.domain.RequestStatus;
import com.aleatica.parking.resource.ResourceType;
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
 * Entidad de persistencia (adaptador de salida) de una solicitud puntual (tabla
 * {@code dbo.requests}). Arquitectura hexagonal, change {@code hexagonal-persistence}.
 *
 * <p>Es la representacion JPA del agregado; el modelo de negocio vive en
 * {@link com.aleatica.parking.request.domain.Request} y el puente entidad&harr;dominio lo
 * resuelve {@link RequestMapper}. Nunca se expone en la capa web (S4684); el controlador
 * trabaja con DTOs. Las referencias a otras tablas se guardan como identificadores
 * ({@code Long}), evitando por diseno consultas N+1 y manteniendo el agregado desacoplado.</p>
 *
 * <p>Conserva las factorias de conveniencia ({@link #create}, {@link #createForResource}) y los
 * mutadores de transicion usados por los casos de uso vecinos aun no migrados
 * ({@code availability}, {@code floor-plan}, {@code export}) que hoy consultan esta tabla
 * directamente; el mapeo domino&rarr;entidad de este agregado usa el constructor de todos los
 * campos.</p>
 */
@Entity
@Table(name = "requests")
public class RequestEntity {

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

    @Column(name = "resource_id")
    private Long resourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 10)
    private ResourceType resourceType;

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
    protected RequestEntity() {
        // JPA
    }

    /**
     * Constructor de todos los campos usado por {@link RequestMapper} para mapear el modelo de
     * dominio a la entidad (alta con {@code id == null}, actualizacion con {@code id} presente).
     *
     * @param id                  identificador ({@code null} en el alta)
     * @param employeeId          empleado solicitante
     * @param requestedDate       fecha solicitada
     * @param status              estado del ciclo de vida
     * @param resourceId          recurso asignado; {@code null} mientras {@code PENDING}
     * @param resourceType        tipo de recurso solicitado
     * @param approvalNote        nota del administrador al aprobar
     * @param rejectionReasonCode codigo del catalogo de rechazo
     * @param rejectionReason     texto libre del rechazo
     * @param resolvedById        empleado (ADMIN) que resolvio
     * @param resolvedAt          instante de resolucion (UTC)
     * @param createdAt           instante de creacion (UTC)
     */
    public RequestEntity(
            Long id, Long employeeId, LocalDate requestedDate, RequestStatus status,
            Long resourceId, ResourceType resourceType, String approvalNote,
            RejectionReasonCode rejectionReasonCode, String rejectionReason, Long resolvedById,
            Instant resolvedAt, Instant createdAt) {
        this.id = id;
        this.employeeId = employeeId;
        this.requestedDate = requestedDate;
        this.status = status;
        this.resourceId = resourceId;
        this.resourceType = resourceType;
        this.approvalNote = approvalNote;
        this.rejectionReasonCode = rejectionReasonCode;
        this.rejectionReason = rejectionReason;
        this.resolvedById = resolvedById;
        this.resolvedAt = resolvedAt;
        this.createdAt = createdAt;
    }

    /**
     * Da de alta una entidad de solicitud en estado {@link RequestStatus#PENDING} sin plaza
     * (tipo {@code PARKING} por defecto).
     *
     * @param employeeId    empleado solicitante
     * @param requestedDate fecha solicitada
     * @param now           instante de creacion (UTC)
     * @return la entidad nueva, aun no persistida
     */
    public static RequestEntity create(Long employeeId, LocalDate requestedDate, Instant now) {
        return create(employeeId, ResourceType.PARKING, requestedDate, now);
    }

    /**
     * Da de alta una entidad de solicitud en estado {@link RequestStatus#PENDING} sin recurso,
     * para un tipo de recurso concreto.
     *
     * @param employeeId    empleado solicitante
     * @param resourceType  tipo del recurso solicitado ({@code PARKING}/{@code DESK})
     * @param requestedDate fecha solicitada
     * @param now           instante de creacion (UTC)
     * @return la entidad nueva, aun no persistida
     */
    public static RequestEntity create(
            Long employeeId, ResourceType resourceType, LocalDate requestedDate, Instant now) {
        RequestEntity request = new RequestEntity();
        request.employeeId = employeeId;
        request.requestedDate = requestedDate;
        request.status = RequestStatus.PENDING;
        request.resourceId = null;
        request.resourceType = resourceType;
        request.createdAt = now;
        return request;
    }

    /**
     * Da de alta una entidad de solicitud en estado {@link RequestStatus#PENDING} ya vinculada a
     * un recurso concreto (solicitud puesto-especifica del plano, capability {@code floor-plan}).
     *
     * @param employeeId    empleado solicitante
     * @param resourceType  tipo del recurso solicitado ({@code DESK} en el plano)
     * @param resourceId    recurso concreto solicitado (puesto pinchado)
     * @param requestedDate fecha solicitada
     * @param now           instante de creacion (UTC)
     * @return la entidad nueva, aun no persistida
     */
    public static RequestEntity createForResource(
            Long employeeId, ResourceType resourceType, Long resourceId,
            LocalDate requestedDate, Instant now) {
        RequestEntity request = create(employeeId, resourceType, requestedDate, now);
        request.resourceId = resourceId;
        return request;
    }

    /**
     * @return {@code true} si la solicitud esta en estado {@link RequestStatus#PENDING}.
     */
    public boolean isPending() {
        return status == RequestStatus.PENDING;
    }

    /**
     * Aprueba la solicitud asignando recurso, resolutor y nota opcional.
     *
     * @param resourceId   recurso asignado (plaza en el nucleo de parking)
     * @param resolvedById empleado (ADMIN) que resuelve
     * @param approvalNote nota libre del administrador (puede ser {@code null})
     * @param now          instante de resolucion (UTC)
     */
    public void approve(Long resourceId, Long resolvedById, String approvalNote, Instant now) {
        this.status = RequestStatus.APPROVED;
        this.resourceId = resourceId;
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
     * Cancela la solicitud (transicion a {@link RequestStatus#CANCELLED}).
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

    public Long getResourceId() {
        return resourceId;
    }

    public ResourceType getResourceType() {
        return resourceType;
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
        if (!(other instanceof RequestEntity request)) {
            return false;
        }
        return id != null && id.equals(request.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
