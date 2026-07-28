package com.aleatica.parking.release;

import com.aleatica.parking.availability.application.AvailabilityService;
import com.aleatica.parking.availability.dto.EmployeeWeekOccupancyResponse;
import com.aleatica.parking.employee.application.EmployeeService;
import com.aleatica.parking.employee.dto.EmployeeOptionResponse;
import com.aleatica.parking.exception.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de solo lectura que alimentan el selector del flujo de liberacion administrativa
 * ({@code ADMIN} y {@code AGENCIA}): (a) el listado de empleados seleccionables y (b) la
 * ocupacion de un empleado por semana (plaza y puesto), base del navegador de semana desde el
 * que se marcan las reservas a liberar.
 *
 * <p>El adaptador web no contiene logica de negocio: delega en {@link EmployeeService} y
 * {@link AvailabilityService} y trabaja siempre con DTOs (nunca con la entidad JPA, S4684).
 * Ambos endpoints son <em>solo lectura</em> e independientes del CRUD de empleados (reservado a
 * {@code ADMIN}): el RBAC ({@code hasAnyRole('ADMIN','AGENCIA')}) concede el acceso a ambos roles
 * sin relajar la gestion de empleados. Cualquier otro rol (incluido {@code EMPLOYEE}) recibe
 * {@code 403} (fail closed).</p>
 */
@Tag(name = "Releases", description = "Seleccion de empleado y ocupacion semanal para liberacion")
@RestController
@RequestMapping("/api/v1/releases")
public class ReleaseSelectionController {

    private static final String SESSION_COOKIE = "sessionCookie";

    private final EmployeeService employeeService;
    private final AvailabilityService availabilityService;

    /**
     * @param employeeService     casos de uso de empleados (listado seleccionable)
     * @param availabilityService servicio de dominio de disponibilidad y ocupacion
     */
    public ReleaseSelectionController(
            EmployeeService employeeService, AvailabilityService availabilityService) {
        this.employeeService = employeeService;
        this.availabilityService = availabilityService;
    }

    /**
     * Lista los empleados activos seleccionables (datos minimos: id, nombre y categoria) para el
     * flujo de liberacion administrativa ({@code ADMIN} o {@code AGENCIA}).
     *
     * @return {@code 200} con las opciones de empleado (id + nombre + categoria)
     */
    @Operation(summary = "Empleados seleccionables para liberacion (ADMIN/AGENCIA)",
            description = "Listado de solo lectura de empleados activos (id + nombre + categoria) "
                    + "para poblar el selector de liberacion. No expone datos de contacto ni "
                    + "credenciales y es independiente del CRUD de empleados (reservado a ADMIN).",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Empleados seleccionables"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos (rol distinto de ADMIN/AGENCIA)",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/employees")
    @PreAuthorize("hasAnyRole('ADMIN','AGENCIA')")
    public ResponseEntity<List<EmployeeOptionResponse>> listSelectableEmployees() {
        return ResponseEntity.ok(employeeService.listSelectableForRelease());
    }

    /**
     * Devuelve la ocupacion de un empleado para la semana indicada (plaza y puesto por dia, con
     * origen y {@code requestId} cuando la ocupacion proviene de una solicitud aprobada); solo
     * {@code ADMIN} o {@code AGENCIA}.
     *
     * @param employeeId empleado a consultar
     * @param weekStart  lunes de la semana a consultar (ISO-8601; se normaliza al lunes de esa semana)
     * @return {@code 200} con la ocupacion semanal del empleado
     */
    @Operation(summary = "Ocupacion semanal de un empleado para liberacion (ADMIN/AGENCIA)",
            description = "Por cada dia de la semana, las reservas del empleado en plaza y puesto "
                    + "con su origen (asignacion fija / solicitud aprobada) y el requestId cuando "
                    + "proviene de una solicitud aprobada.",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ocupacion semanal del empleado"),
            @ApiResponse(responseCode = "400", description = "weekStart ausente o con formato invalido",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos (rol distinto de ADMIN/AGENCIA)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Empleado no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/employees/{employeeId}/occupancy")
    @PreAuthorize("hasAnyRole('ADMIN','AGENCIA')")
    public ResponseEntity<EmployeeWeekOccupancyResponse> getEmployeeWeekOccupancy(
            @Parameter(description = "Id del empleado", required = true, example = "15")
            @PathVariable Long employeeId,
            @Parameter(description = "Lunes de la semana a consultar (ISO-8601)", required = true,
                    example = "2026-07-06")
            @RequestParam(name = "weekStart")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        return ResponseEntity.ok(
                availabilityService.employeeWeekOccupancy(employeeId, weekStart));
    }
}
