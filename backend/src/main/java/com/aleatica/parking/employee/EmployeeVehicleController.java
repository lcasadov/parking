package com.aleatica.parking.employee;

import com.aleatica.parking.employee.application.EmployeeVehicleService;
import com.aleatica.parking.employee.dto.EmployeeVehicleRequest;
import com.aleatica.parking.employee.dto.EmployeeVehicleResponse;
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
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CRUD de vehiculos de un empleado (change {@code employee-vehicles}), reservado al rol
 * {@code ADMIN} ({@code @PreAuthorize("hasRole('ADMIN')")}). Endpoints anidados bajo el empleado
 * ({@code /employees/{employeeId}/vehicles}) para expresar la pertenencia 1:N.
 *
 * <p>El adaptador web no contiene logica de negocio: delega en {@link EmployeeVehicleService} y
 * trabaja siempre con DTOs (nunca con la entidad JPA, S4684). Un acceso de un rol distinto de
 * {@code ADMIN} produce {@code 403} (fail closed). Forma de error uniforme
 * {@code { error, message, fields, timestamp }}.</p>
 */
@Tag(name = "EmployeeVehicles", description = "Vehiculos de un empleado (solo ADMIN)")
@RestController
@RequestMapping("/api/v1/employees/{employeeId}/vehicles")
@PreAuthorize("hasRole('ADMIN')")
public class EmployeeVehicleController {

    private static final String SESSION_COOKIE = "sessionCookie";

    private final EmployeeVehicleService vehicleService;

    /**
     * @param vehicleService casos de uso de vehiculos de empleado
     */
    public EmployeeVehicleController(EmployeeVehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    /**
     * Lista los vehiculos de un empleado.
     *
     * @param employeeId empleado propietario
     * @return {@code 200} con la lista (posiblemente vacia) de vehiculos
     */
    @Operation(summary = "Lista los vehiculos de un empleado (ADMIN)",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de vehiculos del empleado"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Empleado no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public ResponseEntity<List<EmployeeVehicleResponse>> listVehicles(
            @Parameter(description = "Id del empleado", required = true, example = "15")
            @PathVariable Long employeeId) {
        return ResponseEntity.ok(vehicleService.list(employeeId));
    }

    /**
     * Da de alta un vehiculo para un empleado. Solo la matricula es obligatoria.
     *
     * @param employeeId empleado propietario
     * @param request    datos del vehiculo (matricula obligatoria)
     * @return {@code 201} con el vehiculo creado
     */
    @Operation(summary = "Alta de un vehiculo de empleado (ADMIN)",
            description = "Solo la matricula es obligatoria. La matricula debe ser unica por empleado.",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Vehiculo creado"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos (matricula ausente)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Empleado no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Matricula ya registrada para el empleado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<EmployeeVehicleResponse> createVehicle(
            @Parameter(description = "Id del empleado", required = true, example = "15")
            @PathVariable Long employeeId,
            @Valid @RequestBody EmployeeVehicleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleService.create(employeeId, request));
    }

    /**
     * Edita un vehiculo de un empleado.
     *
     * @param employeeId empleado propietario
     * @param vehicleId  vehiculo a editar
     * @param request    nuevos datos (matricula obligatoria)
     * @return {@code 200} con el vehiculo actualizado
     */
    @Operation(summary = "Edicion de un vehiculo de empleado (ADMIN)",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vehiculo actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos (matricula ausente)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Empleado o vehiculo no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Matricula ya registrada para el empleado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{vehicleId}")
    public ResponseEntity<EmployeeVehicleResponse> updateVehicle(
            @Parameter(description = "Id del empleado", required = true, example = "15")
            @PathVariable Long employeeId,
            @Parameter(description = "Id del vehiculo", required = true, example = "3")
            @PathVariable Long vehicleId,
            @Valid @RequestBody EmployeeVehicleRequest request) {
        return ResponseEntity.ok(vehicleService.update(employeeId, vehicleId, request));
    }

    /**
     * Borra un vehiculo de un empleado.
     *
     * @param employeeId empleado propietario
     * @param vehicleId  vehiculo a borrar
     * @return {@code 204} sin contenido
     */
    @Operation(summary = "Borrado de un vehiculo de empleado (ADMIN)",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Vehiculo borrado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Empleado o vehiculo no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{vehicleId}")
    public ResponseEntity<Void> deleteVehicle(
            @Parameter(description = "Id del empleado", required = true, example = "15")
            @PathVariable Long employeeId,
            @Parameter(description = "Id del vehiculo", required = true, example = "3")
            @PathVariable Long vehicleId) {
        vehicleService.delete(employeeId, vehicleId);
        return ResponseEntity.noContent().build();
    }
}
