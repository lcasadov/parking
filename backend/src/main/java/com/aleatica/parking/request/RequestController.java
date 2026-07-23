package com.aleatica.parking.request;

import com.aleatica.parking.concurrency.ConcurrencyRetry;
import com.aleatica.parking.employee.dto.PageResponse;
import com.aleatica.parking.exception.ApiError;
import com.aleatica.parking.request.application.RequestService;
import com.aleatica.parking.request.domain.RequestStatus;
import com.aleatica.parking.request.dto.RequestAdminAssignRequest;
import com.aleatica.parking.request.dto.RequestAdminCancelRequest;
import com.aleatica.parking.request.dto.RequestApproveRequest;
import com.aleatica.parking.request.dto.RequestCreateRequest;
import com.aleatica.parking.request.dto.RequestRejectRequest;
import com.aleatica.parking.request.dto.RequestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de solicitudes puntuales de plaza (crear, listar propias, cola de
 * pendientes FIFO, detalle, cancelar, aprobar y rechazar).
 *
 * <p>El adaptador web no contiene logica de negocio: delega en {@link RequestService}
 * y trabaja siempre con DTOs (nunca con la entidad JPA, S4684). La autorizacion es de
 * dos niveles: el RBAC ({@code @PreAuthorize}) concede la <em>funcion</em> (crear/
 * cancelar solo {@code EMPLOYEE}; listar pendientes/detalle/aprobar/rechazar solo
 * {@code ADMIN}), y la verificacion de pertenencia (BOLA) del servicio concede el
 * <em>dato concreto</em> (un {@code EMPLOYEE} solo lista/cancela las suyas), rechazando
 * con 403 el acceso a las de otro empleado.</p>
 */
@Tag(name = "Requests", description = "Solicitudes puntuales de plaza")
@RestController
@RequestMapping("/api/v1/requests")
public class RequestController {

    private static final String SESSION_COOKIE = "sessionCookie";

    private final RequestService requestService;
    private final ConcurrencyRetry concurrencyRetry;

    /**
     * @param requestService   casos de uso de solicitudes
     * @param concurrencyRetry reintento acotado ante victima de deadlock (issue #57)
     */
    public RequestController(RequestService requestService, ConcurrencyRetry concurrencyRetry) {
        this.requestService = requestService;
        this.concurrencyRetry = concurrencyRetry;
    }

    /**
     * Crea una solicitud para una fecha desde hoy en adelante (hoy o cualquier fecha futura,
     * sin limite superior; no se permiten fechas pasadas; solo {@code EMPLOYEE}). Maximo una
     * solicitud {@code PENDING} por empleado y fecha.
     *
     * @param request        fecha solicitada
     * @param authentication autenticacion resuelta de la sesion (solicitante)
     * @return {@code 201} con la solicitud creada en estado {@code PENDING}
     */
    @Operation(summary = "Crea una solicitud para una fecha (EMPLOYEE)",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Solicitud creada en estado PENDING"),
            @ApiResponse(responseCode = "400", description = "Fecha pasada (anterior a hoy) o invalida",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Ya existe una solicitud pendiente esa fecha",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<RequestResponse> createRequest(
            @Valid @RequestBody RequestCreateRequest request, Authentication authentication) {
        RequestResponse created = requestService.create(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Asigna puntualmente un recurso a un empleado para una fecha concreta (solo {@code ADMIN});
     * change {@code restructure-admin-workflows}, capability {@code admin-punctual-assignment}. La
     * asignacion nace directamente {@code APPROVED} (sin pasar por {@code PENDING}), reutilizando
     * la validacion de disponibilidad y la auto-asignacion por categoria/planta ya existentes en
     * {@link RequestService}, y queda trazada en auditoria con el admin como actor.
     *
     * @param request        empleado, fecha, tipo de recurso y (opcional) recurso elegido
     * @param authentication autenticacion resuelta de la sesion (admin actuante)
     * @return {@code 201} con la asignacion creada en estado {@code APPROVED}
     */
    @Operation(summary = "Asignacion puntual de un recurso a un empleado (ADMIN)",
            description = "Crea una asignacion para una fecha concreta que nace APPROVED, sin pasar "
                    + "por PENDING. Si se omite resourceId en PARKING, auto-asigna una plaza libre "
                    + "por categoria/planta; en DESK el puesto es obligatorio (400 si se omite). "
                    + "Queda trazada en auditoria con el admin como actor.",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Asignacion creada en estado APPROVED"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos, fecha pasada o "
                    + "recurso obligatorio (DESK) no indicado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos (rol distinto de ADMIN)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Empleado o recurso no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Recurso no disponible esa fecha, o "
                    + "NO_AVAILABILITY (auto-asignacion sin plaza libre)",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RequestResponse> adminAssignRequest(
            @Valid @RequestBody RequestAdminAssignRequest request, Authentication authentication) {
        RequestResponse created = concurrencyRetry.execute(
                () -> requestService.adminAssign(authentication.getName(), request));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Lista de forma paginada las solicitudes propias del empleado de la sesion, con
     * filtro opcional por estado (solo {@code EMPLOYEE}).
     *
     * @param status         filtro opcional por estado
     * @param pageable       pagina y tamano (parametros {@code page}/{@code size})
     * @param authentication autenticacion resuelta de la sesion (propietario)
     * @return {@code 200} con la pagina de solicitudes propias
     */
    @Operation(summary = "Mis solicitudes (EMPLOYEE)",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de solicitudes propias"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/mine")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<PageResponse<RequestResponse>> listMyRequests(
            @Parameter(description = "Filtro opcional por estado")
            @RequestParam(name = "status", required = false) RequestStatus status,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {
        return ResponseEntity.ok(
                requestService.listMine(authentication.getName(), status, pageable));
    }

    /**
     * Lista de forma paginada las solicitudes pendientes en orden FIFO
     * ({@code created_at ASC}); solo {@code ADMIN}.
     *
     * @param pageable pagina y tamano (parametros {@code page}/{@code size})
     * @return {@code 200} con la pagina de solicitudes pendientes
     */
    @Operation(summary = "Solicitudes pendientes, orden FIFO (ADMIN)",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de solicitudes pendientes"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<RequestResponse>> listPendingRequests(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(requestService.listPending(pageable));
    }

    /**
     * Lista de forma paginada las solicitudes por estado en orden de actividad reciente
     * ({@code created_at DESC}); solo {@code ADMIN}. Habilita las pestanas "aprobadas",
     * "rechazadas" y "todas" de la bandeja admin. Un {@code status} ausente devuelve todas las
     * solicitudes (pestana "todas"). Para las {@code APPROVED} se resuelve el numero humano del
     * recurso (plaza/puesto), igual que en el listado propio.
     *
     * @param status   filtro opcional por estado (ausente = todas)
     * @param pageable pagina y tamano (parametros {@code page}/{@code size})
     * @return {@code 200} con la pagina de solicitudes en ese estado
     */
    @Operation(summary = "Solicitudes por estado, orden actividad reciente (ADMIN)",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de solicitudes por estado"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<RequestResponse>> listRequestsByStatus(
            @Parameter(description = "Filtro opcional por estado (ausente = todas)")
            @RequestParam(name = "status", required = false) RequestStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(requestService.listByStatus(status, pageable));
    }

    /**
     * Devuelve el detalle de una solicitud por su id (solo {@code ADMIN}).
     *
     * @param id identificador de la solicitud
     * @return {@code 200} con la solicitud; {@code 404} si no existe
     */
    @Operation(summary = "Detalle de una solicitud (ADMIN)",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitud"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RequestResponse> getRequest(
            @Parameter(description = "Id de la solicitud") @PathVariable Long id) {
        return ResponseEntity.ok(requestService.get(id));
    }

    /**
     * Cancela la propia solicitud del empleado (solo {@code EMPLOYEE}; verificacion de pertenencia
     * en el servicio). Admite cancelar una solicitud {@code PENDING} (cualquier fecha) o una
     * {@code APPROVED} de fecha futura (hoy inclusive), en cuyo caso libera el recurso asignado
     * (change {@code cancel-approved-request}).
     *
     * @param id             identificador de la solicitud
     * @param authentication autenticacion resuelta de la sesion (propietario)
     * @return {@code 200} con la solicitud en estado {@code CANCELLED}
     */
    @Operation(summary = "Cancela la propia solicitud PENDING o APPROVED futura (EMPLOYEE)",
            description = "Cancela una solicitud PENDING (cualquier fecha) o una APPROVED cuya "
                    + "fecha es hoy o posterior, liberando el recurso. Una APPROVED de fecha "
                    + "pasada o un estado terminal (REJECTED/CANCELLED) responden 409.",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Solicitud cancelada (PENDING, o APPROVED futura con recurso liberado)"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos o solicitud ajena",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409",
                    description = "APPROVED de fecha pasada o estado terminal (REJECTED/CANCELLED)",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<RequestResponse> cancelRequest(
            @Parameter(description = "Id de la solicitud") @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(requestService.cancel(id, authentication.getName()));
    }

    /**
     * Cancela administrativamente una solicitud {@code APPROVED} de fecha futura de cualquier
     * empleado para liberar el recurso que ocupa ({@code ADMIN} o {@code AGENCIA}; changes
     * {@code release-occupied-resource} y {@code admin-release-by-employee-week}). Exige un motivo
     * obligatorio, que queda trazado en auditoria. Una solicitud que no esta {@code APPROVED}
     * (p. ej. {@code PENDING}, que se resuelve con {@code reject}) o de fecha pasada responde
     * {@code 409}. La logica de servicio no cambia: sigue exigiendo {@code APPROVED} futura y
     * motivo; solo se amplia el RBAC para que {@code AGENCIA} pueda liberar por solicitud, en
     * paridad con la liberacion administrativa de recurso fijo.
     *
     * @param id             identificador de la solicitud
     * @param body           motivo obligatorio de la cancelacion
     * @param authentication autenticacion resuelta de la sesion (administrador o agencia)
     * @return {@code 200} con la solicitud en estado {@code CANCELLED} y el recurso liberado
     */
    @Operation(summary = "Cancela administrativamente una solicitud APPROVED futura (ADMIN/AGENCIA)",
            description = "Cancela la solicitud APPROVED de fecha futura de un empleado, liberando "
                    + "el recurso ocupado. Requiere un motivo, que se registra en auditoria. Una "
                    + "solicitud no APPROVED o de fecha pasada responde 409.",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Solicitud cancelada y recurso liberado"),
            @ApiResponse(responseCode = "400", description = "Motivo ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409",
                    description = "La solicitud no esta APPROVED o es de fecha pasada",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{id}/admin-cancel")
    @PreAuthorize("hasAnyRole('ADMIN','AGENCIA')")
    public ResponseEntity<RequestResponse> adminCancelRequest(
            @Parameter(description = "Id de la solicitud") @PathVariable Long id,
            @Valid @RequestBody RequestAdminCancelRequest body,
            Authentication authentication) {
        return ResponseEntity.ok(
                requestService.adminCancel(id, authentication.getName(), body.reason()));
    }

    /**
     * Aprueba una solicitud {@code PENDING} asignando una plaza disponible (solo
     * {@code ADMIN}). Si la plaza no esta disponible o colisiona por concurrencia,
     * devuelve {@code 409}.
     *
     * @param id             identificador de la solicitud
     * @param body           plaza a asignar y nota opcional
     * @param authentication autenticacion resuelta de la sesion (resolutor)
     * @return {@code 200} con la solicitud en estado {@code APPROVED}
     */
    @Operation(summary = "Aprueba una solicitud asignando plaza (ADMIN)",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitud aprobada"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos (plaza ausente)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Solicitud o plaza no encontrada",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Plaza no disponible, concurrencia o estado no PENDING",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RequestResponse> approveRequest(
            @Parameter(description = "Id de la solicitud") @PathVariable Long id,
            @Valid @RequestBody RequestApproveRequest body,
            Authentication authentication) {
        return ResponseEntity.ok(concurrencyRetry.execute(
                () -> requestService.approve(id, body, authentication.getName())));
    }

    /**
     * Rechaza una solicitud {@code PENDING} con un codigo del catalogo; exige texto
     * libre (&ge;5) si el codigo es {@code OTHER} (solo {@code ADMIN}).
     *
     * @param id             identificador de la solicitud
     * @param body           codigo del catalogo y texto libre opcional
     * @param authentication autenticacion resuelta de la sesion (resolutor)
     * @return {@code 200} con la solicitud en estado {@code REJECTED}
     */
    @Operation(summary = "Rechaza una solicitud con motivo (ADMIN)",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitud rechazada"),
            @ApiResponse(responseCode = "400", description = "Motivo OTHER sin texto libre valido",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "La solicitud no esta en PENDING",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RequestResponse> rejectRequest(
            @Parameter(description = "Id de la solicitud") @PathVariable Long id,
            @Valid @RequestBody RequestRejectRequest body,
            Authentication authentication) {
        return ResponseEntity.ok(requestService.reject(id, body, authentication.getName()));
    }
}
