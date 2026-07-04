package com.aleatica.parking.desk;

import com.aleatica.parking.desk.application.DeskService;
import com.aleatica.parking.desk.dto.DeskActivationRequest;
import com.aleatica.parking.desk.dto.DeskCreateRequest;
import com.aleatica.parking.desk.dto.DeskResponse;
import com.aleatica.parking.desk.dto.DeskUpdateRequest;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de gestion de puestos de oficina.
 *
 * <p>El adaptador web no contiene logica de negocio: delega en {@link DeskService} y
 * trabaja siempre con DTOs (nunca con la entidad JPA, S4684). La lectura (listar/detalle)
 * la puede hacer cualquier usuario autenticado ({@code ADMIN} o {@code EMPLOYEE}); las
 * mutaciones (alta, edicion, activacion) estan reservadas al {@code ADMIN}
 * ({@code @PreAuthorize("hasRole('ADMIN')")}), y un intento de un {@code EMPLOYEE} produce
 * {@code 403} (fail closed).</p>
 */
@Tag(name = "Desks", description = "Gestion de puestos de oficina")
@RestController
@RequestMapping("/api/v1/desks")
public class DeskController {

    private static final String SESSION_COOKIE = "sessionCookie";

    private final DeskService deskService;

    /**
     * @param deskService casos de uso de gestion de puestos
     */
    public DeskController(DeskService deskService) {
        this.deskService = deskService;
    }

    /**
     * Lista puestos de forma paginada con filtro opcional por estado.
     *
     * @param active   filtro por estado activo (opcional)
     * @param pageable pagina y tamano (parametros {@code page}/{@code size})
     * @return {@code 200} con la pagina de puestos
     */
    @Operation(summary = "Lista puestos", security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de puestos"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<PageResponse<DeskResponse>> listDesks(
            @Parameter(description = "Filtro por estado activo")
            @RequestParam(name = "active", required = false) Boolean active,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(deskService.list(active, pageable));
    }

    /**
     * Devuelve el detalle de un puesto por su id.
     *
     * @param id id del puesto
     * @return {@code 200} con el puesto; {@code 404} si no existe
     */
    @Operation(summary = "Detalle de un puesto", security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Puesto",
                    content = @Content(schema = @Schema(implementation = DeskResponse.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Puesto no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<DeskResponse> getDesk(
            @Parameter(description = "Id del puesto") @PathVariable Long id) {
        return ResponseEntity.ok(deskService.get(id));
    }

    /**
     * Crea un puesto; el {@code number} debe ser unico y estar en el rango 1-65
     * (solo {@code ADMIN}).
     *
     * @param request datos de alta validados
     * @return {@code 201} con el puesto creado
     */
    @Operation(summary = "Crea un puesto (ADMIN)", security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Puesto creado",
                    content = @Content(schema = @Schema(implementation = DeskResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos (numero fuera de rango 1-65)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Numero de puesto ya en uso",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeskResponse> createDesk(@Valid @RequestBody DeskCreateRequest request) {
        DeskResponse created = deskService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Modifica la categoria y, opcionalmente, las coordenadas de un puesto (solo
     * {@code ADMIN}). El {@code number} es inmutable.
     *
     * @param id      id del puesto
     * @param request datos de edicion validados
     * @return {@code 200} con el puesto actualizado
     */
    @Operation(summary = "Modifica un puesto (ADMIN)", security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Puesto actualizado",
                    content = @Content(schema = @Schema(implementation = DeskResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos (coordenadas fuera de 0-100)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Puesto no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeskResponse> updateDesk(
            @PathVariable Long id, @Valid @RequestBody DeskUpdateRequest request) {
        return ResponseEntity.ok(deskService.update(id, request));
    }

    /**
     * Activa o desactiva un puesto (solo {@code ADMIN}). Un puesto inactivo deja de
     * aparecer como disponible.
     *
     * @param id      id del puesto
     * @param request nuevo estado activo
     * @return {@code 200} con el puesto actualizado
     */
    @Operation(summary = "Activa/desactiva un puesto (ADMIN)",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado del puesto actualizado",
                    content = @Content(schema = @Schema(implementation = DeskResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Puesto no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PatchMapping("/{id}/activation")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeskResponse> setDeskActivation(
            @PathVariable Long id, @Valid @RequestBody DeskActivationRequest request) {
        return ResponseEntity.ok(deskService.setActivation(id, request.active()));
    }
}
