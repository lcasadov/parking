package com.aleatica.parking.floorplan;

import com.aleatica.parking.concurrency.ConcurrencyRetry;
import com.aleatica.parking.exception.ApiError;
import com.aleatica.parking.floorplan.application.FloorPlanCommandService;
import com.aleatica.parking.floorplan.application.FloorPlanQueryService;
import com.aleatica.parking.floorplan.dto.DeskPositionRequest;
import com.aleatica.parking.floorplan.dto.DeskRequestResponse;
import com.aleatica.parking.floorplan.dto.FloorPlanRequestBody;
import com.aleatica.parking.floorplan.dto.FloorPlanResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints del plano interactivo de puestos: consultar el plano por fecha, solicitar un
 * puesto pinchandolo y (solo {@code ADMIN}) reposicionar los marcadores.
 *
 * <p>El adaptador web no contiene logica de negocio: delega en {@link FloorPlanQueryService}
 * y {@link FloorPlanCommandService} y trabaja siempre con DTOs (nunca con la entidad JPA,
 * S4684). La autorizacion sigue la matriz de {@code docs/security-design.md}: ver el plano lo
 * puede hacer cualquier usuario autenticado; solicitar es de {@code EMPLOYEE}; editar
 * posiciones es exclusivo del {@code ADMIN} (un intento de {@code EMPLOYEE} produce 403, fail
 * closed). La solicitud se envuelve en {@link ConcurrencyRetry} para traducir la carrera de
 * insercion concurrente (dos empleados sobre el mismo puesto libre) a 409 en vez de 500
 * (issue #57).</p>
 */
@Tag(name = "Floor Plan", description = "Plano interactivo de puestos de oficina")
@RestController
@RequestMapping("/api/v1/floor-plan")
public class FloorPlanController {

    private static final String SESSION_COOKIE = "sessionCookie";

    private final FloorPlanQueryService floorPlanQueryService;
    private final FloorPlanCommandService floorPlanCommandService;
    private final ConcurrencyRetry concurrencyRetry;

    /**
     * @param floorPlanQueryService   casos de uso de lectura del plano
     * @param floorPlanCommandService casos de uso de solicitud y reposicionamiento
     * @param concurrencyRetry        reintento acotado ante victima de deadlock (issue #57)
     */
    public FloorPlanController(
            FloorPlanQueryService floorPlanQueryService,
            FloorPlanCommandService floorPlanCommandService,
            ConcurrencyRetry concurrencyRetry) {
        this.floorPlanQueryService = floorPlanQueryService;
        this.floorPlanCommandService = floorPlanCommandService;
        this.concurrencyRetry = concurrencyRetry;
    }

    /**
     * Devuelve el plano de puestos para una fecha: posicion y estado de cada puesto activo,
     * relativo al empleado de la sesion (cualquier usuario autenticado).
     *
     * @param date           fecha a consultar (ISO-8601 {@code YYYY-MM-DD}, ventana hoy..+14d)
     * @param authentication autenticacion resuelta de la sesion (solicitante)
     * @return {@code 200} con el plano de la fecha
     */
    @Operation(summary = "Plano de puestos por fecha",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plano de la fecha",
                    content = @Content(schema = @Schema(implementation = FloorPlanResponse.class))),
            @ApiResponse(responseCode = "400", description = "Fecha ausente, mal formada o fuera de ventana",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<FloorPlanResponse> getFloorPlan(
            @Parameter(description = "Fecha a consultar (ISO-8601)", required = true, example = "2026-07-10")
            @RequestParam(name = "date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {
        return ResponseEntity.ok(
                floorPlanQueryService.floorPlanForDate(authentication.getName(), date));
    }

    /**
     * Solicita un puesto pinchandolo en el plano para una fecha (solo {@code EMPLOYEE}); solo
     * procede si el puesto esta libre esa fecha.
     *
     * @param deskId         identificador del puesto pinchado
     * @param body           fecha solicitada
     * @param authentication autenticacion resuelta de la sesion (solicitante)
     * @return {@code 201} con el identificador de la solicitud y el nuevo estado del puesto
     */
    @Operation(summary = "Solicita un puesto desde el plano (EMPLOYEE)",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Solicitud de puesto creada (PENDING)",
                    content = @Content(schema = @Schema(implementation = DeskRequestResponse.class))),
            @ApiResponse(responseCode = "400", description = "Fecha fuera de la ventana o invalida",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Puesto no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Puesto no libre o ya hay una solicitud pendiente",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/desks/{deskId}/request")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<DeskRequestResponse> requestDeskFromFloorPlan(
            @Parameter(description = "Id del puesto") @PathVariable Long deskId,
            @Valid @RequestBody FloorPlanRequestBody body,
            Authentication authentication) {
        DeskRequestResponse created = concurrencyRetry.execute(
                () -> floorPlanCommandService.requestDesk(authentication.getName(), deskId, body.date()));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Persiste la posicion (coordenadas relativas 0-100) de un puesto en el plano (solo
     * {@code ADMIN}).
     *
     * @param deskId  identificador del puesto a reposicionar
     * @param request nuevas coordenadas validadas
     * @return {@code 204} sin cuerpo
     */
    @Operation(summary = "Reposiciona un puesto en el plano (ADMIN)",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Posicion persistida"),
            @ApiResponse(responseCode = "400", description = "Coordenadas ausentes o fuera de 0-100",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Puesto no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/desks/{deskId}/position")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateDeskPosition(
            @Parameter(description = "Id del puesto") @PathVariable Long deskId,
            @Valid @RequestBody DeskPositionRequest request) {
        floorPlanCommandService.updatePosition(deskId, request.coordX(), request.coordY());
        return ResponseEntity.noContent().build();
    }
}
