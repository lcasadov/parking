package com.aleatica.parking.audit;

import com.aleatica.parking.audit.application.AuditQueryService;
import com.aleatica.parking.audit.application.LoginLogQueryService;
import com.aleatica.parking.audit.dto.AuditLogEntryResponse;
import com.aleatica.parking.audit.dto.LoginLogEntryResponse;
import com.aleatica.parking.auth.domain.LoginResult;
import com.aleatica.parking.employee.dto.PageResponse;
import com.aleatica.parking.exception.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de consulta de trazabilidad, reservados al rol {@code ADMIN}
 * ({@code @PreAuthorize("hasRole('ADMIN')")}): el rastro de auditoria funcional
 * ({@code GET /audit}) y el registro de intentos de login ({@code GET /login-logs}).
 *
 * <p>El adaptador web no contiene logica de negocio: delega en {@link AuditQueryService} y
 * {@link LoginLogQueryService} y trabaja siempre con DTOs (nunca con entidades JPA, S4684).
 * Un acceso de un {@code EMPLOYEE} produce {@code 403} (fail closed). El registro en
 * {@code audit_log}/{@code login_log} no tiene endpoint de escritura: se produce
 * automaticamente (AOP / filtro de auth).</p>
 */
@Tag(name = "Audit", description = "Consulta de auditoria y logs de login (solo ADMIN)")
@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private final AuditQueryService auditQueryService;
    private final LoginLogQueryService loginLogQueryService;

    /**
     * @param auditQueryService    caso de uso de consulta de auditoria
     * @param loginLogQueryService caso de uso de consulta de logs de login
     */
    public AuditController(
            AuditQueryService auditQueryService, LoginLogQueryService loginLogQueryService) {
        this.auditQueryService = auditQueryService;
        this.loginLogQueryService = loginLogQueryService;
    }

    /**
     * Lista el rastro de auditoria funcional de forma paginada, con filtros opcionales.
     *
     * @param actorEmployeeId filtro por empleado actor (opcional)
     * @param action          filtro por accion (opcional)
     * @param from            limite inferior de la ventana temporal (opcional, ISO-8601)
     * @param to              limite superior de la ventana temporal (opcional, ISO-8601)
     * @param pageable        pagina y tamano (por defecto orden {@code occurredAt} descendente)
     * @return {@code 200} con la pagina de entradas de auditoria
     */
    @Operation(summary = "Consulta de auditoria funcional (ADMIN)",
            security = @SecurityRequirement(name = "sessionCookie"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de entradas de auditoria"),
            @ApiResponse(responseCode = "400", description = "Ventana temporal invalida",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/audit")
    public ResponseEntity<PageResponse<AuditLogEntryResponse>> listAuditLog(
            @Parameter(description = "Filtro por empleado actor")
            @RequestParam(name = "actorEmployeeId", required = false) Long actorEmployeeId,
            @Parameter(description = "Filtro por accion")
            @RequestParam(name = "action", required = false) String action,
            @Parameter(description = "Inicio de la ventana temporal (ISO-8601)")
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @Parameter(description = "Fin de la ventana temporal (ISO-8601)")
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(
                auditQueryService.list(actorEmployeeId, action, from, to, pageable));
    }

    /**
     * Lista el registro de intentos de login de forma paginada, con filtros opcionales.
     *
     * @param result   filtro por resultado del intento (opcional)
     * @param from     limite inferior de la ventana temporal (opcional, ISO-8601)
     * @param to       limite superior de la ventana temporal (opcional, ISO-8601)
     * @param pageable pagina y tamano (por defecto orden {@code occurredAt} descendente)
     * @return {@code 200} con la pagina de intentos de login
     */
    @Operation(summary = "Consulta de logs de login (ADMIN)",
            security = @SecurityRequirement(name = "sessionCookie"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de intentos de login"),
            @ApiResponse(responseCode = "400", description = "Resultado o ventana invalidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/login-logs")
    public ResponseEntity<PageResponse<LoginLogEntryResponse>> listLoginLog(
            @Parameter(description = "Filtro por resultado del intento")
            @RequestParam(name = "result", required = false) LoginResult result,
            @Parameter(description = "Inicio de la ventana temporal (ISO-8601)")
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @Parameter(description = "Fin de la ventana temporal (ISO-8601)")
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(loginLogQueryService.list(result, from, to, pageable));
    }
}
