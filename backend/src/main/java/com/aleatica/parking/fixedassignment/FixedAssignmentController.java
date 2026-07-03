package com.aleatica.parking.fixedassignment;

import com.aleatica.parking.employee.dto.PageResponse;
import com.aleatica.parking.exception.ApiError;
import com.aleatica.parking.fixedassignment.application.FixedAssignmentService;
import com.aleatica.parking.fixedassignment.dto.FixedAssignmentPutRequest;
import com.aleatica.parking.fixedassignment.dto.FixedAssignmentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de asignaciones fijas plaza/empleado por dia de la semana.
 *
 * <p>El adaptador web no contiene logica de negocio: delega en
 * {@link FixedAssignmentService} y trabaja siempre con DTOs (nunca con la entidad
 * JPA, S4684). La autorizacion es de dos niveles: el RBAC ({@code @PreAuthorize})
 * concede la <em>funcion</em> (listar/crear/revocar solo {@code ADMIN}; consultar por
 * empleado {@code ADMIN} o {@code EMPLOYEE}), y la verificacion de pertenencia (BOLA)
 * del servicio concede el <em>dato concreto</em> (un {@code EMPLOYEE} solo ve las
 * suyas), rechazando con 403 el acceso a las de otro empleado.</p>
 */
@Tag(name = "FixedAssignments", description = "Asignaciones fijas plaza/empleado por dia")
@RestController
@RequestMapping("/api/v1/fixed-assignments")
public class FixedAssignmentController {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final FixedAssignmentService fixedAssignmentService;

    /**
     * @param fixedAssignmentService casos de uso de asignaciones fijas
     */
    public FixedAssignmentController(FixedAssignmentService fixedAssignmentService) {
        this.fixedAssignmentService = fixedAssignmentService;
    }

    /**
     * Lista de forma paginada todas las asignaciones fijas activas (solo {@code ADMIN}).
     *
     * @param pageable pagina y tamano (parametros {@code page}/{@code size})
     * @return {@code 200} con la pagina de asignaciones activas
     */
    @Operation(summary = "Lista todas las asignaciones fijas activas (ADMIN)",
            security = @SecurityRequirement(name = "sessionCookie"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de asignaciones fijas"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<FixedAssignmentResponse>> listFixedAssignments(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(fixedAssignmentService.list(pageable));
    }

    /**
     * Devuelve las asignaciones fijas de un empleado. {@code ADMIN} cualquiera;
     * {@code EMPLOYEE} solo las suyas (verificacion de pertenencia, 403 en otro caso).
     *
     * @param employeeId     empleado consultado
     * @param authentication autenticacion resuelta de la sesion
     * @return {@code 200} con el array de asignaciones activas del empleado
     */
    @Operation(summary = "Asignaciones fijas de un empleado (ADMIN o propias)",
            security = @SecurityRequirement(name = "sessionCookie"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Asignaciones fijas del empleado"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos o empleado ajeno",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<List<FixedAssignmentResponse>> getEmployeeFixedAssignments(
            @Parameter(description = "Id del empleado") @PathVariable Long employeeId,
            Authentication authentication) {
        List<FixedAssignmentResponse> assignments = fixedAssignmentService.getEmployeeAssignments(
                employeeId, authentication.getName(), isAdmin(authentication));
        return ResponseEntity.ok(assignments);
    }

    /**
     * Establece la asignacion fija de un empleado (plaza + dias de la semana),
     * reemplazando el conjunto de dias de esa plaza (solo {@code ADMIN}).
     *
     * @param employeeId     empleado titular
     * @param request        plaza y dias de la semana validados
     * @param authentication autenticacion resuelta de la sesion (autor del cambio)
     * @return {@code 200} con el array de asignaciones activas resultante
     */
    @Operation(summary = "Asigna/modifica la asignacion fija de un empleado (ADMIN)",
            security = @SecurityRequirement(name = "sessionCookie"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Asignacion fija establecida"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos (dias fuera de rango)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Empleado o plaza no encontrados",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Conflicto de unicidad plaza/dia o empleado/dia",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/employee/{employeeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<FixedAssignmentResponse>> setEmployeeFixedAssignments(
            @PathVariable Long employeeId,
            @Valid @RequestBody FixedAssignmentPutRequest request,
            Authentication authentication) {
        List<FixedAssignmentResponse> assignments =
                fixedAssignmentService.setAssignments(employeeId, request, authentication.getName());
        return ResponseEntity.ok(assignments);
    }

    /**
     * Revoca (logicamente) la asignacion fija de un empleado (solo {@code ADMIN}).
     *
     * @param employeeId     empleado cuyas asignaciones se revocan
     * @param authentication autenticacion resuelta de la sesion (autor de la revocacion)
     * @return {@code 204} sin contenido
     */
    @Operation(summary = "Revoca (logicamente) la asignacion fija de un empleado (ADMIN)",
            security = @SecurityRequirement(name = "sessionCookie"))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Asignacion revocada"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Sin asignacion activa que revocar",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/employee/{employeeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> revokeEmployeeFixedAssignment(
            @PathVariable Long employeeId, Authentication authentication) {
        fixedAssignmentService.revoke(employeeId, authentication.getName());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    private static boolean isAdmin(Authentication authentication) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (ROLE_ADMIN.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
