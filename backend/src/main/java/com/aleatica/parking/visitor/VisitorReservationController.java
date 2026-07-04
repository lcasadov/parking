package com.aleatica.parking.visitor;

import com.aleatica.parking.employee.dto.PageResponse;
import com.aleatica.parking.exception.ApiError;
import com.aleatica.parking.visitor.application.VisitorReservationService;
import com.aleatica.parking.visitor.dto.VisitorReservationCreateRequest;
import com.aleatica.parking.visitor.dto.VisitorReservationResponse;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de gestion de reservas de plaza para visitantes, reservados al rol
 * {@code ADMIN} ({@code @PreAuthorize("hasRole('ADMIN')")}).
 *
 * <p>El adaptador web no contiene logica de negocio: delega en
 * {@link VisitorReservationService} y trabaja siempre con DTOs (nunca con la entidad JPA,
 * S4684). Un acceso de un {@code EMPLOYEE} produce {@code 403} (fail closed). La forma de
 * error es uniforme {@code { error, message, fields, timestamp }}.</p>
 */
@Tag(name = "VisitorReservations", description = "Reservas de plaza para visitantes (solo ADMIN)")
@RestController
@RequestMapping("/api/v1/visitor-reservations")
@PreAuthorize("hasRole('ADMIN')")
public class VisitorReservationController {

    private static final String SESSION_COOKIE = "sessionCookie";

    private final VisitorReservationService reservationService;

    /**
     * @param reservationService casos de uso de reservas de visitante
     */
    public VisitorReservationController(VisitorReservationService reservationService) {
        this.reservationService = reservationService;
    }

    /**
     * Lista reservas de visitante de forma paginada con filtros opcionales por fecha y
     * plaza.
     *
     * @param date           filtro por fecha (opcional)
     * @param parkingSpaceId filtro por plaza (opcional)
     * @param pageable       pagina y tamano (parametros {@code page}/{@code size})
     * @return {@code 200} con la pagina de reservas
     */
    @Operation(summary = "Lista reservas de visita (ADMIN)", operationId = "listVisitorReservations",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de reservas de visita"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public ResponseEntity<PageResponse<VisitorReservationResponse>> listVisitorReservations(
            @Parameter(description = "Filtro por fecha (ISO-8601)")
            @RequestParam(name = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Filtro por plaza")
            @RequestParam(name = "parkingSpaceId", required = false) Long parkingSpaceId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(reservationService.list(date, parkingSpaceId, pageable));
    }

    /**
     * Crea una reserva de plaza para un visitante en una fecha; rechaza la reserva si la
     * plaza no esta disponible ese dia.
     *
     * @param request        visitante, plaza, fecha y anotaciones validados
     * @param authentication autenticacion resuelta de la sesion ({@code ADMIN} creador)
     * @return {@code 201} con la reserva creada
     */
    @Operation(summary = "Crea una reserva de plaza para un visitante (ADMIN)",
            operationId = "createVisitorReservation",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reserva creada"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Visitante o plaza no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Plaza no disponible esa fecha",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<VisitorReservationResponse> createVisitorReservation(
            @Valid @RequestBody VisitorReservationCreateRequest request, Authentication authentication) {
        VisitorReservationResponse created =
                reservationService.create(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Anula una reserva de visitante futura (borrado fisico), liberando la plaza ese dia.
     *
     * @param id identificador de la reserva
     * @return {@code 204} sin cuerpo
     */
    @Operation(summary = "Anula una reserva de visita futura (ADMIN)",
            operationId = "cancelVisitorReservation",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Reserva anulada"),
            @ApiResponse(responseCode = "400", description = "La reserva es de una fecha pasada",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelVisitorReservation(
            @Parameter(description = "Id de la reserva") @PathVariable Long id) {
        reservationService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}
