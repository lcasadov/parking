package com.aleatica.parking.employee;

import com.aleatica.parking.employee.application.EmployeeService;
import com.aleatica.parking.employee.dto.EmployeeCreateRequest;
import com.aleatica.parking.employee.dto.EmployeeResetPasswordResponse;
import com.aleatica.parking.employee.dto.EmployeeResponse;
import com.aleatica.parking.employee.dto.EmployeeUpdateRequest;
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
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de gestion de empleados, reservados al rol {@code ADMIN}
 * ({@code @PreAuthorize("hasRole('ADMIN')")}).
 *
 * <p>El adaptador web no contiene logica de negocio: delega en
 * {@link EmployeeService} y trabaja siempre con DTOs (nunca con la entidad JPA,
 * S4684). Un acceso de un {@code EMPLOYEE} produce {@code 403} (fail closed).</p>
 */
@Tag(name = "Employees", description = "Gestion de empleados (solo ADMIN)")
@RestController
@RequestMapping("/api/v1/employees")
@PreAuthorize("hasRole('ADMIN')")
public class EmployeeController {

    private static final String CSV_HEADER =
            "id,firstName,lastName,login,email,department,mobilePhone,"
                    + "licensePlate,corporate,authOrigin,role,enabled,active";
    private static final char CSV_SEPARATOR = ',';
    private static final String CSV_FILENAME = "employees.csv";

    private final EmployeeService employeeService;

    /**
     * @param employeeService casos de uso de gestion de empleados
     */
    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    /**
     * Lista empleados de forma paginada con busqueda libre y filtro por estado.
     *
     * @param q        texto de busqueda libre (nombre, apellidos, login, email)
     * @param active   filtro por estado de baja logica (opcional)
     * @param pageable pagina y tamano (parametros {@code page}/{@code size})
     * @return {@code 200} con la pagina de empleados
     */
    @Operation(summary = "Lista empleados (ADMIN)",
            security = @SecurityRequirement(name = "sessionCookie"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de empleados"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public ResponseEntity<PageResponse<EmployeeResponse>> listEmployees(
            @Parameter(description = "Texto de busqueda libre")
            @RequestParam(name = "q", required = false) String q,
            @Parameter(description = "Filtro por estado activo")
            @RequestParam(name = "active", required = false) Boolean active,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(employeeService.list(q, active, pageable));
    }

    /**
     * Crea un empleado; {@code login} y {@code email} deben ser unicos.
     *
     * @param request datos de alta validados
     * @return {@code 201} con el empleado creado
     */
    @Operation(summary = "Crea un empleado (ADMIN)",
            security = @SecurityRequirement(name = "sessionCookie"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Empleado creado",
                    content = @Content(schema = @Schema(implementation = EmployeeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Login o email ya en uso",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<EmployeeResponse> createEmployee(
            @Valid @RequestBody EmployeeCreateRequest request) {
        EmployeeResponse created = employeeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Modifica un empleado existente.
     *
     * @param id      id del empleado
     * @param request datos de edicion validados
     * @return {@code 200} con el empleado actualizado
     */
    @Operation(summary = "Modifica un empleado (ADMIN)",
            security = @SecurityRequirement(name = "sessionCookie"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Empleado actualizado",
                    content = @Content(schema = @Schema(implementation = EmployeeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Empleado no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Email ya en uso por otro empleado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @PathVariable Long id, @Valid @RequestBody EmployeeUpdateRequest request) {
        return ResponseEntity.ok(employeeService.update(id, request));
    }

    /**
     * Da de baja logicamente a un empleado ({@code active = false}).
     *
     * @param id id del empleado
     * @return {@code 204} sin contenido
     */
    @Operation(summary = "Baja logica de un empleado (ADMIN)",
            security = @SecurityRequirement(name = "sessionCookie"))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Empleado dado de baja"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Empleado no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateEmployee(@PathVariable Long id) {
        employeeService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reactiva a un empleado dado de baja ({@code active = true}).
     *
     * @param id id del empleado
     * @return {@code 204} sin contenido
     */
    @Operation(summary = "Reactiva un empleado (ADMIN)",
            security = @SecurityRequirement(name = "sessionCookie"))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Empleado reactivado"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Empleado no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{id}/reactivate")
    public ResponseEntity<Void> reactivateEmployee(@PathVariable Long id) {
        employeeService.reactivate(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Resetea la contrasena de un empleado ({@code passwordMustChange = true}).
     *
     * @param id id del empleado
     * @return {@code 200} con la temporal (Fase 1) o sin ella (Fase 2)
     */
    @Operation(summary = "Reset administrativo de contrasena (ADMIN)",
            security = @SecurityRequirement(name = "sessionCookie"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contrasena reseteada",
                    content = @Content(
                            schema = @Schema(implementation = EmployeeResetPasswordResponse.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Empleado no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<EmployeeResetPasswordResponse> resetEmployeePassword(
            @PathVariable Long id) {
        return ResponseEntity.ok(employeeService.resetPassword(id));
    }

    /**
     * Exporta los empleados a CSV.
     *
     * <p>El detalle del formato (CSV/XLSX) pertenece a la capability
     * {@code exports}; en Fase 1 se entrega CSV.</p>
     *
     * @param format formato solicitado (aceptado por contrato; se entrega CSV)
     * @return {@code 200} con el fichero CSV adjunto
     */
    @Operation(summary = "Exporta empleados a CSV/XLSX (ADMIN)",
            security = @SecurityRequirement(name = "sessionCookie"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fichero exportado"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportEmployees(
            @Parameter(description = "Formato de exportacion")
            @RequestParam(name = "format", required = false, defaultValue = "csv") String format) {
        byte[] body = toCsv(employeeService.exportAll()).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + CSV_FILENAME + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }

    private String toCsv(List<EmployeeResponse> employees) {
        StringBuilder builder = new StringBuilder();
        builder.append(CSV_HEADER).append('\n');
        for (EmployeeResponse employee : employees) {
            appendRow(builder, employee);
        }
        return builder.toString();
    }

    private void appendRow(StringBuilder builder, EmployeeResponse employee) {
        builder.append(employee.id()).append(CSV_SEPARATOR)
                .append(csv(employee.firstName())).append(CSV_SEPARATOR)
                .append(csv(employee.lastName())).append(CSV_SEPARATOR)
                .append(csv(employee.login())).append(CSV_SEPARATOR)
                .append(csv(employee.email())).append(CSV_SEPARATOR)
                .append(csv(employee.department())).append(CSV_SEPARATOR)
                .append(csv(employee.mobilePhone())).append(CSV_SEPARATOR)
                .append(csv(employee.licensePlate())).append(CSV_SEPARATOR)
                .append(employee.corporate()).append(CSV_SEPARATOR)
                .append(employee.authOrigin()).append(CSV_SEPARATOR)
                .append(employee.role()).append(CSV_SEPARATOR)
                .append(employee.enabled()).append(CSV_SEPARATOR)
                .append(employee.active()).append('\n');
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return '"' + escaped + '"';
    }
}
