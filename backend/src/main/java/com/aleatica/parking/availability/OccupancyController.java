package com.aleatica.parking.availability;

import com.aleatica.parking.availability.application.AvailabilityService;
import com.aleatica.parking.availability.dto.OccupancyResponse;
import com.aleatica.parking.exception.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de ocupacion por fecha (consulta-only): los recursos (plazas y puestos) OCUPADOS
 * una fecha con su titular y origen, base del pivote por-fecha del destino "Liberar" (change
 * {@code restructure-admin-workflows}, capability {@code releases}, design §D5).
 *
 * <p>El adaptador web no contiene logica de negocio: delega en {@link AvailabilityService} y
 * trabaja siempre con DTOs (nunca con la entidad JPA, S4684). Accesible a {@code ADMIN} y
 * {@code AGENCIA} (solo lectura, para orientarse en el pivote por-fecha de liberacion); cualquier
 * otro rol recibe {@code 403} (fail closed).</p>
 */
@Tag(name = "Occupancy", description = "Ocupacion de recursos por fecha (Liberar por fecha)")
@RestController
@RequestMapping("/api/v1/occupancy")
public class OccupancyController {

    private static final String SESSION_COOKIE = "sessionCookie";

    private final AvailabilityService availabilityService;

    /**
     * @param availabilityService servicio de dominio de disponibilidad y ocupacion
     */
    public OccupancyController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    /**
     * Devuelve los recursos (plazas y puestos) ocupados para una fecha, cada uno con su titular
     * y el origen de la ocupacion ({@code ADMIN} o {@code AGENCIA}, solo lectura).
     *
     * @param date fecha a consultar (ISO-8601)
     * @return {@code 200} con los recursos ocupados esa fecha
     */
    @Operation(summary = "Ocupacion de recursos por fecha (ADMIN/AGENCIA)",
            description = "Solo lectura. Base del pivote por-fecha del destino \"Liberar\"; "
                    + "accesible a ADMIN y AGENCIA.",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recursos ocupados para la fecha"),
            @ApiResponse(responseCode = "400", description = "date ausente o con formato invalido",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos (rol distinto de ADMIN/AGENCIA)",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','AGENCIA')")
    public ResponseEntity<OccupancyResponse> getOccupancy(
            @Parameter(description = "Fecha a consultar (ISO-8601)", required = true, example = "2026-07-10")
            @RequestParam(name = "date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(availabilityService.occupancyForDate(date));
    }
}
