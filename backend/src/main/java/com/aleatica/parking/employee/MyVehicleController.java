package com.aleatica.parking.employee;

import com.aleatica.parking.employee.application.MyVehicleService;
import com.aleatica.parking.employee.dto.EmployeeVehicleRequest;
import com.aleatica.parking.employee.dto.EmployeeVehicleResponse;
import com.aleatica.parking.exception.ApiError;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Self-service de vehículos del empleado autenticado (change {@code employee-vehicle-self-service},
 * Fase 1): el empleado gestiona SUS vehículos desde el portal. El empleado se resuelve del
 * principal autenticado ({@code authentication.getName()}), nunca de la ruta, de modo que un
 * empleado no puede operar sobre vehículos ajenos. El alta y la edición dejan el vehículo
 * {@code PENDING} de validación y avisan a los administradores.
 *
 * <p>Accesible a cualquier empleado autenticado (no requiere {@code ADMIN}). Trabaja siempre con
 * DTOs (S4684). Forma de error uniforme {@code { error, message, fields, timestamp }}.</p>
 */
@Tag(name = "MyVehicles", description = "Vehículos del empleado autenticado (self-service)")
@RestController
@RequestMapping("/api/v1/me/vehicles")
public class MyVehicleController {

    private static final String SESSION_COOKIE = "sessionCookie";

    private final MyVehicleService vehicleService;

    public MyVehicleController(MyVehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    /**
     * Lista los vehículos del empleado autenticado.
     *
     * @param authentication principal del empleado autenticado
     * @return {@code 200} con la lista (posiblemente vacía) de sus vehículos
     */
    @Operation(summary = "Lista mis vehículos", security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de mis vehículos"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public ResponseEntity<List<EmployeeVehicleResponse>> listVehicles(Authentication authentication) {
        return ResponseEntity.ok(vehicleService.list(authentication.getName()));
    }

    /**
     * Da de alta un vehículo propio. Solo la matrícula es obligatoria; el vehículo queda
     * {@code PENDING} de validación y se avisa a los administradores.
     *
     * @param request        datos del vehículo (matrícula obligatoria)
     * @param authentication principal del empleado autenticado
     * @return {@code 201} con el vehículo creado en estado {@code PENDING}
     */
    @Operation(summary = "Alta de un vehículo propio (queda pendiente de validación)",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Vehículo creado (PENDING)"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (matrícula ausente)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Matrícula ya registrada para el empleado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<EmployeeVehicleResponse> createVehicle(
            @Valid @RequestBody EmployeeVehicleRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vehicleService.create(authentication.getName(), request));
    }

    /**
     * Edita un vehículo propio. Tras la edición el vehículo vuelve a {@code PENDING} de validación
     * y se avisa a los administradores.
     *
     * @param vehicleId      vehículo a editar (debe pertenecer al empleado autenticado)
     * @param request        nuevos datos (matrícula obligatoria)
     * @param authentication principal del empleado autenticado
     * @return {@code 200} con el vehículo actualizado en estado {@code PENDING}
     */
    @Operation(summary = "Edición de un vehículo propio (vuelve a pendiente de validación)",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vehículo actualizado (PENDING)"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (matrícula ausente)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Vehículo no encontrado o ajeno",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Matrícula ya registrada para el empleado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{vehicleId}")
    public ResponseEntity<EmployeeVehicleResponse> updateVehicle(
            @PathVariable Long vehicleId,
            @Valid @RequestBody EmployeeVehicleRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(vehicleService.update(authentication.getName(), vehicleId, request));
    }

    /**
     * Borra un vehículo propio.
     *
     * @param vehicleId      vehículo a borrar (debe pertenecer al empleado autenticado)
     * @param authentication principal del empleado autenticado
     * @return {@code 204} sin contenido
     */
    @Operation(summary = "Borrado de un vehículo propio",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Vehículo borrado"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Vehículo no encontrado o ajeno",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{vehicleId}")
    public ResponseEntity<Void> deleteVehicle(
            @PathVariable Long vehicleId, Authentication authentication) {
        vehicleService.delete(authentication.getName(), vehicleId);
        return ResponseEntity.noContent().build();
    }
}
