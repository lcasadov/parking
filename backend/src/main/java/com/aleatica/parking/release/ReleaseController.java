package com.aleatica.parking.release;

import com.aleatica.parking.concurrency.ConcurrencyRetry;
import com.aleatica.parking.employee.dto.PageResponse;
import com.aleatica.parking.exception.ApiError;
import com.aleatica.parking.release.application.ReleaseService;
import com.aleatica.parking.release.dto.AdministrativeReleaseRequest;
import com.aleatica.parking.release.dto.ReleaseCreateRequest;
import com.aleatica.parking.release.dto.ReleaseResponse;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de liberacion de recursos con asignacion fija: listar propias, liberacion
 * voluntaria del titular, cancelacion de una futura propia y liberacion administrativa
 * por el {@code ADMIN}.
 *
 * <p>El adaptador web no contiene logica de negocio: delega en {@link ReleaseService} y
 * trabaja siempre con DTOs (nunca con la entidad JPA, S4684). La autorizacion es de dos
 * niveles: el RBAC ({@code @PreAuthorize}) concede la <em>funcion</em> (voluntaria/
 * cancelar/listar solo {@code EMPLOYEE}; administrativa solo {@code ADMIN}), y la
 * verificacion de pertenencia (BOLA) del servicio concede el <em>dato concreto</em> (un
 * {@code EMPLOYEE} solo lista/cancela las suyas), rechazando con 403 el acceso a las de
 * otro empleado.</p>
 */
@Tag(name = "Releases", description = "Liberacion de recursos con asignacion fija")
@RestController
@RequestMapping("/api/v1/releases")
public class ReleaseController {

    private static final String SESSION_COOKIE = "sessionCookie";

    private final ReleaseService releaseService;
    private final ConcurrencyRetry concurrencyRetry;

    /**
     * @param releaseService   casos de uso de liberaciones
     * @param concurrencyRetry reintento acotado ante victima de deadlock (issue #57)
     */
    public ReleaseController(ReleaseService releaseService, ConcurrencyRetry concurrencyRetry) {
        this.releaseService = releaseService;
        this.concurrencyRetry = concurrencyRetry;
    }

    /**
     * Lista de forma paginada las liberaciones propias del empleado de la sesion (solo
     * {@code EMPLOYEE}; verificacion de pertenencia implicita).
     *
     * @param pageable       pagina y tamano (parametros {@code page}/{@code size})
     * @param authentication autenticacion resuelta de la sesion (propietario)
     * @return {@code 200} con la pagina de liberaciones propias
     */
    @Operation(summary = "Mis liberaciones (EMPLOYEE)",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de liberaciones propias"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/mine")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<PageResponse<ReleaseResponse>> listMyReleases(
            @PageableDefault(size = 20) Pageable pageable, Authentication authentication) {
        return ResponseEntity.ok(releaseService.listMyReleases(authentication.getName(), pageable));
    }

    /**
     * Crea una liberacion voluntaria del recurso fijo propio para una fecha presente o
     * futura (solo {@code EMPLOYEE}).
     *
     * @param request        fecha a liberar y, opcionalmente, el recurso
     * @param authentication autenticacion resuelta de la sesion (titular = ejecutor)
     * @return {@code 201} con la liberacion creada de tipo {@code VOLUNTARY}
     */
    @Operation(summary = "Liberacion voluntaria (EMPLOYEE)",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Liberacion creada"),
            @ApiResponse(responseCode = "400", description = "Fecha en el pasado o invalida",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Sin asignacion fija ese dia o recurso ya liberado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ReleaseResponse> createRelease(
            @Valid @RequestBody ReleaseCreateRequest request, Authentication authentication) {
        ReleaseResponse created = concurrencyRetry.execute(
                () -> releaseService.createRelease(authentication.getName(), request));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Anula (borrado fisico) una liberacion futura propia del empleado de la sesion (solo
     * {@code EMPLOYEE}; verificacion de pertenencia en el servicio).
     *
     * @param id             identificador de la liberacion
     * @param authentication autenticacion resuelta de la sesion (propietario)
     * @return {@code 204} sin cuerpo
     */
    @Operation(summary = "Anula una liberacion futura propia (EMPLOYEE)",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Liberacion anulada"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos o liberacion ajena",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Liberacion no encontrada",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "La liberacion es de una fecha pasada",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Void> cancelRelease(
            @Parameter(description = "Id de la liberacion") @PathVariable Long id,
            Authentication authentication) {
        releaseService.cancelRelease(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    /**
     * Crea una liberacion administrativa del recurso fijo de un empleado para una fecha
     * presente o futura, con motivo obligatorio (solo {@code ADMIN}).
     *
     * @param request        empleado, recurso, fecha y motivo
     * @param authentication autenticacion resuelta de la sesion (ejecutor {@code ADMIN})
     * @return {@code 201} con la liberacion creada de tipo {@code ADMINISTRATIVE}
     */
    @Operation(summary = "Liberacion administrativa (ADMIN)",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Liberacion administrativa creada"),
            @ApiResponse(responseCode = "400", description = "Motivo ausente, fecha en el pasado o datos invalidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Empleado o recurso no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Sin asignacion fija ese dia o recurso ya liberado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/administrative")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReleaseResponse> createAdministrativeRelease(
            @Valid @RequestBody AdministrativeReleaseRequest request, Authentication authentication) {
        ReleaseResponse created = concurrencyRetry.execute(
                () -> releaseService.createAdministrativeRelease(authentication.getName(), request));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
