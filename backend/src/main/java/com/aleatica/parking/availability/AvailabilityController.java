package com.aleatica.parking.availability;

import com.aleatica.parking.availability.application.AvailabilityService;
import com.aleatica.parking.availability.dto.AvailabilityResponse;
import com.aleatica.parking.exception.ApiError;
import com.aleatica.parking.resource.ResourceType;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de disponibilidad de recursos por fecha (consulta-only).
 *
 * <p>El adaptador web no contiene logica de negocio: delega en {@link AvailabilityService}
 * y trabaja siempre con DTOs (nunca con la entidad JPA, S4684). Cualquier usuario
 * autenticado puede consultar la disponibilidad de una fecha; la validacion del parametro
 * {@code date} (ausente o con formato invalido) se traduce a {@code 400} con la forma de
 * error estandar en {@code GlobalExceptionHandler}.</p>
 */
@Tag(name = "Availability", description = "Disponibilidad de recursos por fecha")
@RestController
@RequestMapping("/api/v1/availability")
public class AvailabilityController {

    private static final String SESSION_COOKIE = "sessionCookie";

    private final AvailabilityService availabilityService;

    /**
     * @param availabilityService servicio de dominio de disponibilidad y calendario
     */
    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    /**
     * Devuelve los recursos disponibles para la fecha indicada (cualquier usuario
     * autenticado). Consulta-only: no modifica ningun estado.
     *
     * @param date         fecha a consultar (ISO-8601 {@code YYYY-MM-DD})
     * @param resourceType tipo de recurso ({@code PARKING} por defecto, {@code DESK} para puestos)
     * @return {@code 200} con la disponibilidad de la fecha
     */
    @Operation(summary = "Recursos disponibles para una fecha",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Disponibilidad de la fecha"),
            @ApiResponse(responseCode = "400", description = "Fecha ausente o con formato invalido",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public ResponseEntity<AvailabilityResponse> getAvailability(
            @Parameter(description = "Fecha a consultar (ISO-8601)", required = true, example = "2026-07-10")
            @RequestParam(name = "date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Tipo de recurso; por defecto PARKING", example = "DESK")
            @RequestParam(name = "resourceType", required = false, defaultValue = "PARKING")
            ResourceType resourceType) {
        return ResponseEntity.ok(availabilityService.availabilityForDate(date, resourceType));
    }
}
