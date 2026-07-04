package com.aleatica.parking.visitor;

import com.aleatica.parking.employee.dto.PageResponse;
import com.aleatica.parking.exception.ApiError;
import com.aleatica.parking.visitor.application.VisitorService;
import com.aleatica.parking.visitor.dto.VisitorCreateRequest;
import com.aleatica.parking.visitor.dto.VisitorResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de gestion de fichas de visitante, reservados al rol {@code ADMIN}
 * ({@code @PreAuthorize("hasRole('ADMIN')")}).
 *
 * <p>El adaptador web no contiene logica de negocio: delega en {@link VisitorService} y
 * trabaja siempre con DTOs (nunca con la entidad JPA, S4684). Un acceso de un
 * {@code EMPLOYEE} produce {@code 403} (fail closed): el visitante es un dato solo del
 * {@code ADMIN}. La forma de error es uniforme
 * {@code { error, message, fields, timestamp }}.</p>
 */
@Tag(name = "Visitors", description = "Gestion de fichas de visitante (solo ADMIN)")
@RestController
@RequestMapping("/api/v1/visitors")
@PreAuthorize("hasRole('ADMIN')")
public class VisitorController {

    private static final String SESSION_COOKIE = "sessionCookie";

    private final VisitorService visitorService;

    /**
     * @param visitorService casos de uso de fichas de visitante
     */
    public VisitorController(VisitorService visitorService) {
        this.visitorService = visitorService;
    }

    /**
     * Lista fichas de visitante de forma paginada con busqueda libre opcional.
     *
     * @param q        texto de busqueda libre por {@code nationalId}/nombre/apellidos/matricula
     * @param pageable pagina y tamano (parametros {@code page}/{@code size})
     * @return {@code 200} con la pagina de fichas
     */
    @Operation(summary = "Lista visitantes (ADMIN)", operationId = "listVisitors",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de visitantes"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public ResponseEntity<PageResponse<VisitorResponse>> listVisitors(
            @Parameter(description = "Texto de busqueda libre")
            @RequestParam(name = "q", required = false) String q,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(visitorService.list(q, pageable));
    }

    /**
     * Crea una ficha de visitante; el {@code nationalId} debe ser unico.
     *
     * @param request        datos de alta validados
     * @param authentication autenticacion resuelta de la sesion ({@code ADMIN} creador)
     * @return {@code 201} con la ficha creada
     */
    @Operation(summary = "Crea una ficha de visitante (ADMIN)", operationId = "createVisitor",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Visitante creado"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "nationalId ya en uso",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<VisitorResponse> createVisitor(
            @Valid @RequestBody VisitorCreateRequest request, Authentication authentication) {
        VisitorResponse created = visitorService.create(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Devuelve el detalle de una ficha de visitante.
     *
     * @param id identificador de la ficha
     * @return {@code 200} con la ficha
     */
    @Operation(summary = "Detalle de un visitante (ADMIN)", operationId = "getVisitor",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Visitante"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Visitante no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<VisitorResponse> getVisitor(
            @Parameter(description = "Id del visitante") @PathVariable Long id) {
        return ResponseEntity.ok(visitorService.get(id));
    }

    /**
     * Modifica una ficha de visitante; el cambio afecta solo a futuras reservas.
     *
     * @param id      identificador de la ficha
     * @param request nuevos datos validados
     * @return {@code 200} con la ficha actualizada
     */
    @Operation(summary = "Modifica una ficha de visitante (ADMIN)", operationId = "updateVisitor",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Visitante actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Visitante no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "nationalId ya en uso",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<VisitorResponse> updateVisitor(
            @Parameter(description = "Id del visitante") @PathVariable Long id,
            @Valid @RequestBody VisitorCreateRequest request) {
        return ResponseEntity.ok(visitorService.update(id, request));
    }
}
