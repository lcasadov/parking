package com.aleatica.parking.request.domain;

import com.aleatica.parking.resource.ResourceType;
import java.time.Duration;
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

    /**
     * Nota de resolucion que identifica una auto-aprobacion del sistema (no de un ADMIN humano):
     * la solicitud nace {@code APPROVED} con {@code resolvedById = null} y esta nota. Se comparte
     * entre las dos vias que auto-aprueban un puesto elegido -{@code RequestService} (POST
     * /requests) y {@code FloorPlanCommandService} (POST /floor-plan/.../request)- para garantizar
     * paridad de actor/nota/evento y evitar el literal duplicado (S1192).
     */
    public static final String AUTO_APPROVAL_NOTE = "auto";

    /**
     * Nota de resolucion que identifica una asignacion puntual del {@code ADMIN} (change
     * {@code restructure-admin-workflows}, capability {@code admin-punctual-assignment}): la
     * solicitud nace {@code APPROVED} con {@code resolvedById} = admin actuante y esta nota,
     * distinguiendola de la auto-aprobacion del sistema ({@link #AUTO_APPROVAL_NOTE}) y de una
     * aprobacion clasica sobre una {@code PENDING} previa.
     */
    public static final String ADMIN_ASSIGNMENT_NOTE = "admin-assignment";

    /**
     * Nota de resolucion que identifica una reasignacion o intercambio (swap) de recurso por el
     * {@code ADMIN} (change {@code reservas-employee-admin-reassign}, capability
     * {@code admin-resource-reassignment}): la solicitud sigue {@code APPROVED} pero cambia de
     * recurso, distinguiendola de una asignacion puntual ({@link #ADMIN_ASSIGNMENT_NOTE}) y de la
     * auto-aprobacion del sistema ({@link #AUTO_APPROVAL_NOTE}).
     */
    public static final String ADMIN_REASSIGNMENT_NOTE = "admin-reassignment";

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
    private Instant lastRemindedAt;
    private boolean waitlisted;

    /** Constructor privado; las instancias se obtienen por las factorias estaticas. */
    private Request() {
        // factorias
    }

    /**
     * Da de alta una solicitud nueva en estado {@link RequestStatus#PENDING} sin plaza,
     * de tipo {@code PARKING} por defecto, no en lista de espera.
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
     * para un tipo de recurso concreto ({@code PARKING} plaza / {@code DESK} puesto), no en
     * lista de espera.
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
        return create(employeeId, resourceType, requestedDate, now, false);
    }

    /**
     * Da de alta una solicitud nueva en estado {@link RequestStatus#PENDING} sin recurso, para
     * un tipo de recurso concreto, marcando explicitamente si nace en <strong>lista de
     * espera</strong> (change {@code waitlist-requests}): en modo {@code AUTOMATIC} sin
     * disponibilidad y opt-in del empleado, o en modo {@code MANUAL} cuando no habia
     * disponibilidad para ese dia/tipo en el momento de la creacion.
     *
     * @param employeeId    empleado solicitante
     * @param resourceType  tipo del recurso solicitado ({@code PARKING}/{@code DESK})
     * @param requestedDate fecha solicitada
     * @param now           instante de creacion (UTC)
     * @param waitlisted    {@code true} si la solicitud nace en lista de espera
     * @return la solicitud nueva, aun no persistida
     */
    public static Request create(
            Long employeeId, ResourceType resourceType, LocalDate requestedDate, Instant now,
            boolean waitlisted) {
        Request request = new Request();
        request.employeeId = employeeId;
        request.requestedDate = requestedDate;
        request.status = RequestStatus.PENDING;
        request.resourceId = null;
        request.resourceType = resourceType;
        request.createdAt = now;
        request.waitlisted = waitlisted;
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
     * @return la solicitud reconstituida, con {@code lastRemindedAt = null}
     */
    public static Request restore(
            Long id, Long employeeId, LocalDate requestedDate, RequestStatus status,
            Long resourceId, ResourceType resourceType, String approvalNote,
            RejectionReasonCode rejectionReasonCode, String rejectionReason, Long resolvedById,
            Instant resolvedAt, Instant createdAt) {
        return restore(id, employeeId, requestedDate, status, resourceId, resourceType,
                approvalNote, rejectionReasonCode, rejectionReason, resolvedById, resolvedAt,
                createdAt, null);
    }

    /**
     * Reconstituye una solicitud a partir de su estado persistido, incluyendo el instante del
     * ultimo reenvio de aviso, sin marca de lista de espera ({@code waitlisted = false}; uso
     * exclusivo del mapper de infraestructura {@code RequestMapper}; no aplica reglas de
     * transicion). Delega en el canonico {@link #restore(Long, Long, LocalDate, RequestStatus,
     * Long, ResourceType, String, RejectionReasonCode, String, Long, Instant, Instant, Instant,
     * boolean)} para no romper los llamantes previos al change {@code waitlist-requests}.
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
     * @param lastRemindedAt      instante del ultimo reenvio de aviso; {@code null} si nunca
     * @return la solicitud reconstituida, con {@code waitlisted = false}
     */
    public static Request restore(
            Long id, Long employeeId, LocalDate requestedDate, RequestStatus status,
            Long resourceId, ResourceType resourceType, String approvalNote,
            RejectionReasonCode rejectionReasonCode, String rejectionReason, Long resolvedById,
            Instant resolvedAt, Instant createdAt, Instant lastRemindedAt) {
        return restore(id, employeeId, requestedDate, status, resourceId, resourceType,
                approvalNote, rejectionReasonCode, rejectionReason, resolvedById, resolvedAt,
                createdAt, lastRemindedAt, false);
    }

    /**
     * Reconstituye una solicitud a partir de su estado persistido, incluyendo el instante del
     * ultimo reenvio de aviso y la marca de lista de espera (change {@code waitlist-requests};
     * uso exclusivo del mapper de infraestructura {@code RequestMapper}; no aplica reglas de
     * transicion). Es la factoria canonica de reconstitucion: el resto de sobrecargas de
     * {@code restore} delegan aqui con los valores por defecto retrocompatibles.
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
     * @param lastRemindedAt      instante del ultimo reenvio de aviso; {@code null} si nunca
     * @param waitlisted          {@code true} si la solicitud esta en lista de espera
     * @return la solicitud reconstituida
     */
    public static Request restore(
            Long id, Long employeeId, LocalDate requestedDate, RequestStatus status,
            Long resourceId, ResourceType resourceType, String approvalNote,
            RejectionReasonCode rejectionReasonCode, String rejectionReason, Long resolvedById,
            Instant resolvedAt, Instant createdAt, Instant lastRemindedAt, boolean waitlisted) {
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
        request.lastRemindedAt = lastRemindedAt;
        request.waitlisted = waitlisted;
        return request;
    }

    /**
     * @return {@code true} si la solicitud esta en estado {@link RequestStatus#PENDING}.
     */
    public boolean isPending() {
        return status == RequestStatus.PENDING;
    }

    /**
     * @return {@code true} si la solicitud esta en estado {@link RequestStatus#APPROVED}.
     */
    public boolean isApproved() {
        return status == RequestStatus.APPROVED;
    }

    /**
     * Indica si la solicitud admite la transicion a {@link RequestStatus#REJECTED}. Un ADMIN
     * puede rechazar tanto una solicitud {@code PENDING} (rechazo clasico) como una
     * {@code APPROVED} (rechazo posterior que libera el recurso, change
     * {@code request-auto-assignment}). Los estados terminales {@code REJECTED}/{@code CANCELLED}
     * no admiten transicion.
     *
     * @return {@code true} si el estado actual es {@code PENDING} o {@code APPROVED}
     */
    public boolean canBeRejected() {
        return status == RequestStatus.PENDING || status == RequestStatus.APPROVED;
    }

    /**
     * Indica si la solicitud admite la cancelacion por su dueno a fecha {@code today} (change
     * {@code cancel-approved-request}). Una solicitud {@code PENDING} siempre puede cancelarse
     * (no ocupa recurso, comportamiento clasico). Una solicitud {@code APPROVED} solo puede
     * cancelarse cuando su {@code requestedDate} es futura (hoy inclusive): la cancelacion libera
     * el recurso, que reaparece en disponibilidad. Una {@code APPROVED} con fecha pasada no es
     * cancelable (no se libera un recurso de una fecha ya transcurrida). Los estados terminales
     * {@code REJECTED}/{@code CANCELLED} no admiten cancelacion.
     *
     * @param today fecha de referencia ("hoy"), derivada del mismo reloj/zona que la ventana de
     *              creacion ({@code ZoneOffset.UTC}) por el caso de uso
     * @return {@code true} si el estado y la fecha permiten cancelar
     */
    public boolean canBeCancelledBy(LocalDate today) {
        if (status == RequestStatus.PENDING) {
            return true;
        }
        if (status == RequestStatus.APPROVED) {
            return !requestedDate.isBefore(today);
        }
        return false;
    }

    /**
     * Indica si la solicitud admite la cancelacion administrativa por un {@code ADMIN} a fecha
     * {@code today} (change {@code release-occupied-resource}). A diferencia de la cancelacion del
     * empleado ({@link #canBeCancelledBy(LocalDate)}), el administrador solo cancela para liberar
     * un recurso ocupado por una solicitud {@code APPROVED} de fecha futura (hoy inclusive): una
     * {@code PENDING} de otro se resuelve con {@code reject}, y una {@code APPROVED} de fecha pasada
     * no libera nada (recurso ya transcurrido). Los estados terminales
     * {@code REJECTED}/{@code CANCELLED} tampoco admiten cancelacion.
     *
     * @param today fecha de referencia ("hoy"), derivada del mismo reloj/zona que la ventana de
     *              creacion ({@code ZoneOffset.UTC}) por el caso de uso
     * @return {@code true} si el estado es {@code APPROVED} y su fecha no es pasada
     */
    public boolean canBeAdminCancelledBy(LocalDate today) {
        return status == RequestStatus.APPROVED && !requestedDate.isBefore(today);
    }

    /**
     * Indica si el propio empleado puede reenviar el aviso de esta solicitud al {@code ADMIN}
     * en el instante {@code now} (change {@code request-resend-notice}): solo tiene sentido
     * reavisar de una solicitud aun {@link RequestStatus#PENDING}, y solo si ha transcurrido al
     * menos {@code cooldown} desde la referencia mas reciente entre su creacion y su ultimo
     * reenvio (un reenvio previo siempre es posterior a la creacion, por lo que basta con
     * preferir {@code lastRemindedAt} cuando existe).
     *
     * @param now      instante de referencia ("ahora"), del mismo reloj inyectable que el resto
     *                 del caso de uso
     * @param cooldown periodo minimo exigido desde la referencia mas reciente
     * @return {@code true} si el estado es {@code PENDING} y ha transcurrido el periodo minimo
     */
    public boolean canBeResent(Instant now, Duration cooldown) {
        if (status != RequestStatus.PENDING) {
            return false;
        }
        Instant reference = lastRemindedAt != null ? lastRemindedAt : createdAt;
        return !now.isBefore(reference.plus(cooldown));
    }

    /**
     * Registra el instante del reenvio de aviso, avanzando la referencia que usara la proxima
     * comprobacion de {@link #canBeResent(Instant, Duration)} (change
     * {@code request-resend-notice}). No cambia el estado ni ningun otro campo de la solicitud.
     *
     * @param now instante del reenvio (UTC)
     */
    public void markReminded(Instant now) {
        this.lastRemindedAt = now;
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
     * Reasigna el recurso de una solicitud ya {@code APPROVED} a otro recurso, conservando el
     * estado {@code APPROVED} (change {@code reservas-employee-admin-reassign}, capability
     * {@code admin-resource-reassignment}). Registra el {@code ADMIN} actuante como resolutor, marca
     * la nota {@link #ADMIN_REASSIGNMENT_NOTE} y actualiza el instante de resolucion. El recurso
     * anterior queda libre implicitamente: la disponibilidad se recalcula solo sobre filas
     * {@code APPROVED} por {@code resource_id}, de modo que al cambiar el {@code resource_id} el
     * recurso previo reaparece en disponibilidad. La guarda de estado/fecha ({@code APPROVED} de
     * fecha futura) la impone el caso de uso antes de invocar este metodo.
     *
     * @param newResourceId nuevo recurso asignado (del mismo tipo, ya validado como libre)
     * @param resolvedById  empleado (ADMIN) que ejecuta la reasignacion
     * @param now           instante de la reasignacion (UTC)
     * @throws IllegalStateException si la solicitud no esta {@code APPROVED}
     */
    public void reassign(Long newResourceId, Long resolvedById, Instant now) {
        if (status != RequestStatus.APPROVED) {
            throw new IllegalStateException(
                    "Solo se puede reasignar una solicitud APPROVED; estado actual: " + status);
        }
        this.resourceId = newResourceId;
        this.approvalNote = ADMIN_REASSIGNMENT_NOTE;
        this.resolvedById = resolvedById;
        this.resolvedAt = now;
    }

    /**
     * Rechaza la solicitud con un codigo del catalogo y texto libre opcional. Admite el rechazo
     * desde {@link RequestStatus#PENDING} (rechazo clasico) o desde {@link RequestStatus#APPROVED}
     * (rechazo posterior que libera el recurso): al pasar a {@code REJECTED} la fila deja de
     * cumplir el filtro {@code status = 'APPROVED'} del indice unico y la plaza/puesto reaparece
     * en disponibilidad (change {@code request-auto-assignment}). El {@code resourceId} se
     * conserva como traza del recurso liberado (la disponibilidad se recalcula solo sobre filas
     * {@code APPROVED}, no hace falta limpiarlo).
     *
     * @param reasonCode   codigo del catalogo de rechazo
     * @param reason       texto libre (obligatorio si {@code reasonCode = OTHER})
     * @param resolvedById empleado (ADMIN) que resuelve
     * @param now          instante de resolucion (UTC)
     * @throws IllegalStateException si la solicitud esta en un estado terminal
     *                               ({@code REJECTED}/{@code CANCELLED})
     */
    public void reject(RejectionReasonCode reasonCode, String reason, Long resolvedById, Instant now) {
        if (!canBeRejected()) {
            throw new IllegalStateException(
                    "Solo se puede rechazar una solicitud PENDING o APPROVED; estado actual: " + status);
        }
        this.status = RequestStatus.REJECTED;
        this.rejectionReasonCode = reasonCode;
        this.rejectionReason = reason;
        this.resolvedById = resolvedById;
        this.resolvedAt = now;
    }

    /**
     * Cancela la solicitud (transicion a {@link RequestStatus#CANCELLED}) a peticion del propio
     * empleado. Admite la cancelacion desde {@link RequestStatus#PENDING} (cancelacion clasica) y
     * desde {@link RequestStatus#APPROVED} (cancelacion que <strong>libera el recurso</strong>:
     * al pasar a {@code CANCELLED} la fila deja de cumplir el filtro {@code status = 'APPROVED'}
     * del indice unico y la plaza/puesto reaparece en disponibilidad, change
     * {@code cancel-approved-request}). El {@code resourceId} se conserva como traza del recurso
     * liberado (la disponibilidad se recalcula solo sobre filas {@code APPROVED}, no hace falta
     * limpiarlo). La guarda de fecha ({@code APPROVED} solo si es futura) la impone el caso de uso
     * via {@link #canBeCancelledBy(LocalDate)} antes de invocar este metodo.
     *
     * @throws IllegalStateException si la solicitud esta en un estado terminal
     *                               ({@code REJECTED}/{@code CANCELLED})
     */
    public void cancel() {
        if (status == RequestStatus.REJECTED || status == RequestStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Solo se puede cancelar una solicitud PENDING o APPROVED; estado actual: " + status);
        }
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

    public Instant getLastRemindedAt() {
        return lastRemindedAt;
    }

    /**
     * @return {@code true} si la solicitud esta marcada en <strong>lista de espera</strong>
     *         (change {@code waitlist-requests}): una {@code PENDING} para un dia/tipo sin
     *         disponibilidad, candidata a promocion cuando se libere un recurso.
     */
    public boolean isWaitlisted() {
        return waitlisted;
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
