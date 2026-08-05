package com.aleatica.parking.visitor;

import com.aleatica.parking.exception.ApiError;
import com.aleatica.parking.visitor.application.VisitorVehicleService;
import com.aleatica.parking.visitor.dto.VisitorVehicleRequest;
import com.aleatica.parking.visitor.dto.VisitorVehicleResponse;
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
 * CRUD de vehiculos de un visitante (change {@code visitor-vehicles}), reservado al rol
 * {@code ADMIN} ({@code @PreAuthorize("hasRole('ADMIN')")}). Endpoints anidados bajo el visitante
 * ({@code /visitors/{visitorId}/vehicles}) para expresar la pertenencia 1:N.
 *
 * <p>El adaptador web no contiene logica de negocio: delega en {@link VisitorVehicleService} y
 * trabaja siempre con DTOs (nunca con la entidad JPA, S4684). Un acceso de un rol distinto de
 * {@code ADMIN} produce {@code 403} (fail closed). Forma de error uniforme
 * {@code { error, message, fields, timestamp }}.</p>
 */
@Tag(name = "VisitorVehicles", description = "Vehiculos de un visitante (solo ADMIN)")
@RestController
@RequestMapping("/api/v1/visitors/{visitorId}/vehicles")
@PreAuthorize("hasRole('ADMIN')")
public class VisitorVehicleController {

    private static final String SESSION_COOKIE = "sessionCookie";

    private final VisitorVehicleService vehicleService;

    /**
     * @param vehicleService casos de uso de vehiculos de visitante
     */
    public VisitorVehicleController(VisitorVehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    /**
     * Lista los vehiculos de un visitante.
     *
     * @param visitorId visitante propietario
     * @return {@code 200} con la lista (posiblemente vacia) de vehiculos
     */
    @Operation(summary = "Lista los vehiculos de un visitante (ADMIN)",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de vehiculos del visitante"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Visitante no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public ResponseEntity<List<VisitorVehicleResponse>> listVehicles(
            @Parameter(description = "Id del visitante", required = true, example = "15")
            @PathVariable Long visitorId) {
        return ResponseEntity.ok(vehicleService.list(visitorId));
    }

    /**
     * Da de alta un vehiculo para un visitante. Solo la matricula es obligatoria.
     *
     * @param visitorId visitante propietario
     * @param request   datos del vehiculo (matricula obligatoria)
     * @return {@code 201} con el vehiculo creado
     */
    @Operation(summary = "Alta de un vehiculo de visitante (ADMIN)",
            description = "Solo la matricula es obligatoria. La matricula debe ser unica por visitante.",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Vehiculo creado"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos (matricula ausente)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Visitante no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Matricula ya registrada para el visitante",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<VisitorVehicleResponse> createVehicle(
            @Parameter(description = "Id del visitante", required = true, example = "15")
            @PathVariable Long visitorId,
            @Valid @RequestBody VisitorVehicleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleService.create(visitorId, request));
    }

    /**
     * Edita un vehiculo de un visitante.
     *
     * @param visitorId visitante propietario
     * @param vehicleId vehiculo a editar
     * @param request   nuevos datos (matricula obligatoria)
     * @return {@code 200} con el vehiculo actualizado
     */
    @Operation(summary = "Edicion de un vehiculo de visitante (ADMIN)",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vehiculo actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos (matricula ausente)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Visitante o vehiculo no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Matricula ya registrada para el visitante",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{vehicleId}")
    public ResponseEntity<VisitorVehicleResponse> updateVehicle(
            @Parameter(description = "Id del visitante", required = true, example = "15")
            @PathVariable Long visitorId,
            @Parameter(description = "Id del vehiculo", required = true, example = "3")
            @PathVariable Long vehicleId,
            @Valid @RequestBody VisitorVehicleRequest request) {
        return ResponseEntity.ok(vehicleService.update(visitorId, vehicleId, request));
    }

    /**
     * Borra un vehiculo de un visitante.
     *
     * @param visitorId visitante propietario
     * @param vehicleId vehiculo a borrar
     * @return {@code 204} sin contenido
     */
    @Operation(summary = "Borrado de un vehiculo de visitante (ADMIN)",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Vehiculo borrado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Visitante o vehiculo no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{vehicleId}")
    public ResponseEntity<Void> deleteVehicle(
            @Parameter(description = "Id del visitante", required = true, example = "15")
            @PathVariable Long visitorId,
            @Parameter(description = "Id del vehiculo", required = true, example = "3")
            @PathVariable Long vehicleId) {
        vehicleService.delete(visitorId, vehicleId);
        return ResponseEntity.noContent().build();
    }
}
