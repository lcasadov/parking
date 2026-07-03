package com.aleatica.parking.parkingspace;

import com.aleatica.parking.employee.dto.PageResponse;
import com.aleatica.parking.exception.ApiError;
import com.aleatica.parking.parkingspace.application.ParkingSpaceService;
import com.aleatica.parking.parkingspace.dto.ParkingSpaceConfigureRequest;
import com.aleatica.parking.parkingspace.dto.ParkingSpaceRequest;
import com.aleatica.parking.parkingspace.dto.ParkingSpaceResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de gestion de plazas de parking, reservados al rol {@code ADMIN}
 * ({@code @PreAuthorize("hasRole('ADMIN')")}).
 *
 * <p>El adaptador web no contiene logica de negocio: delega en
 * {@link ParkingSpaceService} y trabaja siempre con DTOs (nunca con la entidad JPA,
 * S4684). Un acceso de un {@code EMPLOYEE} produce {@code 403} (fail closed): el
 * empleado solo percibe plazas via disponibilidad, no via el catalogo.</p>
 */
@Tag(name = "ParkingSpaces", description = "Gestion de plazas de parking (solo ADMIN)")
@RestController
@RequestMapping("/api/v1/parking-spaces")
@PreAuthorize("hasRole('ADMIN')")
public class ParkingSpaceController {

    private final ParkingSpaceService parkingSpaceService;

    /**
     * @param parkingSpaceService casos de uso de gestion de plazas
     */
    public ParkingSpaceController(ParkingSpaceService parkingSpaceService) {
        this.parkingSpaceService = parkingSpaceService;
    }

    /**
     * Lista plazas de forma paginada con filtro opcional por estado.
     *
     * @param active   filtro por estado activo (opcional)
     * @param pageable pagina y tamano (parametros {@code page}/{@code size})
     * @return {@code 200} con la pagina de plazas
     */
    @Operation(summary = "Lista plazas (ADMIN)",
            security = @SecurityRequirement(name = "sessionCookie"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de plazas"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public ResponseEntity<PageResponse<ParkingSpaceResponse>> listParkingSpaces(
            @Parameter(description = "Filtro por estado activo")
            @RequestParam(name = "active", required = false) Boolean active,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(parkingSpaceService.list(active, pageable));
    }

    /**
     * Crea una plaza; el {@code label} debe ser unico.
     *
     * @param request datos de alta validados
     * @return {@code 201} con la plaza creada
     */
    @Operation(summary = "Crea una plaza (ADMIN)",
            security = @SecurityRequirement(name = "sessionCookie"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Plaza creada",
                    content = @Content(schema = @Schema(implementation = ParkingSpaceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Etiqueta ya en uso",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<ParkingSpaceResponse> createParkingSpace(
            @Valid @RequestBody ParkingSpaceRequest request) {
        ParkingSpaceResponse created = parkingSpaceService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Modifica una plaza existente ({@code label} y estado {@code active}).
     *
     * @param id      id de la plaza
     * @param request datos de edicion validados
     * @return {@code 200} con la plaza actualizada
     */
    @Operation(summary = "Modifica una plaza (ADMIN)",
            security = @SecurityRequirement(name = "sessionCookie"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plaza actualizada",
                    content = @Content(schema = @Schema(implementation = ParkingSpaceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Plaza no encontrada",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Etiqueta ya en uso por otra plaza",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ParkingSpaceResponse> updateParkingSpace(
            @PathVariable Long id, @Valid @RequestBody ParkingSpaceRequest request) {
        return ResponseEntity.ok(parkingSpaceService.update(id, request));
    }

    /**
     * Configura el numero total de plazas activas del parque, preservando el
     * historico de las plazas existentes.
     *
     * @param request cuerpo con el {@code total} deseado ({@code >= 0})
     * @return {@code 200} con el listado de plazas resultante
     */
    @Operation(summary = "Configura el total de plazas (ADMIN)",
            security = @SecurityRequirement(name = "sessionCookie"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Configuracion aplicada",
                    content = @Content(schema = @Schema(implementation = ParkingSpaceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Total invalido",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/configure")
    public ResponseEntity<List<ParkingSpaceResponse>> configureParkingSpaces(
            @Valid @RequestBody ParkingSpaceConfigureRequest request) {
        return ResponseEntity.ok(parkingSpaceService.configure(request.total()));
    }
}
