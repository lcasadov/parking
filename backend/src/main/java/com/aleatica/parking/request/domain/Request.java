package com.aleatica.parking.request.domain;

import com.aleatica.parking.resource.ResourceType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Modelo de dominio de una solicitud puntual (arquitectura hexagonal, change
 * {@code hexagonal-persistence}).
 *
 * <p>Es el nucleo de negocio del agregado {@code request}: <strong>libre de framework</strong>
 * (sin JPA, sin Spring, sin Hibernate). Encapsula la maquina de estados de la solicitud, cuyas
 * transiciones ({@link #approve}, {@link #reject}, {@link #cancel}) garantizan la invariante de
 * que solo se resuelve desde {@link RequestStatus#PENDING} (la comprobacion de la invariante la
 * hace el caso de uso via {@link #isPending()} antes de transitar). La persistencia la resuelve
 * un adaptador de infraestructura que mapea este modelo a/desde una entidad JPA
 * ({@code RequestEntity}) a traves de {@code RequestMapper}.</p>
 *
 * <p>Una solicitud nace en {@link RequestStatus#PENDING} con {@code resourceId = null} y un
 * {@code resourceType} fijado desde la creacion; el recurso concreto se asigna solo al aprobar
 * (salvo la solicitud puesto-especifica del plano, que ya nace vinculada). Las referencias a
 * otras tablas se guardan como identificadores ({@code Long}), manteniendo el agregado
 * desacoplado.</p>
 */
public class Request {

    private Long id;
    private Long employeeId;
    private LocalDate requestedDate;
    private RequestStatus status;
    private Long resourceId;
    private ResourceType resourceType;
    private String approvalNote;
    private RejectionReasonCode rejectionReasonCode;
    private String rejectionReason;
    private Long resolvedById;
    private Instant resolvedAt;
    private Instant createdAt;

    /** Constructor privado; las instancias se obtienen por las factorias estaticas. */
    private Request() {
        // factorias
    }

    /**
     * Da de alta una solicitud nueva en estado {@link RequestStatus#PENDING} sin plaza,
     * de tipo {@code PARKING} por defecto.
     *
     * @param employeeId    empleado solicitante
     * @param requestedDate fecha solicitada
     * @param now           instante de creacion (UTC)
     * @return la solicitud nueva, aun no persistida
     */
    public static Request create(Long employeeId, LocalDate requestedDate, Instant now) {
        return create(employeeId, ResourceType.PARKING, requestedDate, now);
    }

    /**
     * Da de alta una solicitud nueva en estado {@link RequestStatus#PENDING} sin recurso,
     * para un tipo de recurso concreto ({@code PARKING} plaza / {@code DESK} puesto).
     *
     * <p>Nace con {@code resourceId = null}; el recurso concreto se asigna al aprobar. El
     * {@code resourceType} se fija desde la creacion para que la unicidad {@code PENDING}
     * por empleado/tipo/fecha permita a un empleado pedir plaza y puesto la misma fecha.</p>
     *
     * @param employeeId    empleado solicitante
     * @param resourceType  tipo del recurso solicitado ({@code PARKING}/{@code DESK})
     * @param requestedDate fecha solicitada
     * @param now           instante de creacion (UTC)
     * @return la solicitud nueva, aun no persistida
     */
    public static Request create(
            Long employeeId, ResourceType resourceType, LocalDate requestedDate, Instant now) {
        Request request = new Request();
        request.employeeId = employeeId;
        request.requestedDate = requestedDate;
        request.status = RequestStatus.PENDING;
        request.resourceId = null;
        request.resourceType = resourceType;
        request.createdAt = now;
        return request;
    }

    /**
     * Da de alta una solicitud nueva en estado {@link RequestStatus#PENDING} ya vinculada a
     * un recurso concreto (solicitud puesto-especifica del plano interactivo, capability
     * {@code floor-plan}).
     *
     * @param employeeId    empleado solicitante
     * @param resourceType  tipo del recurso solicitado ({@code DESK} en el plano)
     * @param resourceId    recurso concreto solicitado (puesto pinchado)
     * @param requestedDate fecha solicitada
     * @param now           instante de creacion (UTC)
     * @return la solicitud nueva, aun no persistida
     */
    public static Request createForResource(
            Long employeeId, ResourceType resourceType, Long resourceId,
            LocalDate requestedDate, Instant now) {
        Request request = create(employeeId, resourceType, requestedDate, now);
        request.resourceId = resourceId;
        return request;
    }

    /**
     * Reconstituye una solicitud a partir de su estado persistido (uso exclusivo del mapper de
     * infraestructura {@code RequestMapper}; no aplica reglas de transicion).
     *
     * @param id                  identificador
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
     * @return la solicitud reconstituida
     */
    public static Request restore(
            Long id, Long employeeId, LocalDate requestedDate, RequestStatus status,
            Long resourceId, ResourceType resourceType, String approvalNote,
            RejectionReasonCode rejectionReasonCode, String rejectionReason, Long resolvedById,
            Instant resolvedAt, Instant createdAt) {
        Request request = new Request();
        request.id = id;
        request.employeeId = employeeId;
        request.requestedDate = requestedDate;
        request.status = status;
        request.resourceId = resourceId;
        request.resourceType = resourceType;
        request.approvalNote = approvalNote;
        request.rejectionReasonCode = rejectionReasonCode;
        request.rejectionReason = rejectionReason;
        request.resolvedById = resolvedById;
        request.resolvedAt = resolvedAt;
        request.createdAt = createdAt;
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
