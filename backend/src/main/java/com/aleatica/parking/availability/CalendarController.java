package com.aleatica.parking.availability;

import com.aleatica.parking.availability.application.AvailabilityService;
import com.aleatica.parking.availability.dto.AdminWeeklyCalendarResponse;
import com.aleatica.parking.availability.dto.MyWeekResponse;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de calendario semanal (consulta-only): el calendario completo del {@code ADMIN}
 * y la vista personal "Mi Semana".
 *
 * <p>El adaptador web no contiene logica de negocio: delega en {@link AvailabilityService}
 * y trabaja siempre con DTOs (nunca con la entidad JPA, S4684). La autorizacion es de dos
 * niveles: {@code getAdminCalendar} exige rol {@code ADMIN} ({@code @PreAuthorize}) y
 * devuelve {@code 403} a {@code EMPLOYEE}; {@code getMyWeek} lo puede consultar cualquier
 * usuario autenticado, y el servicio filtra a sus recursos propios sin exponer nombres
 * ajenos (privacidad, {@code docs/security-design.md}).</p>
 */
@Tag(name = "Calendar", description = "Calendario semanal admin y 'Mi Semana'")
@RestController
@RequestMapping("/api/v1/calendar")
public class CalendarController {

    private static final String SESSION_COOKIE = "sessionCookie";

    private final AvailabilityService availabilityService;

    /**
     * @param availabilityService servicio de dominio de disponibilidad y calendario
     */
    public CalendarController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    /**
     * Devuelve el calendario semanal completo de todos los recursos activos de un tipo (plazas
     * por defecto, o puestos) con su estado y titular por dia (solo {@code ADMIN}); change
     * {@code restructure-admin-workflows}, design §D4.
     *
     * @param weekStart    lunes de la semana a mostrar (se normaliza al lunes de esa semana)
     * @param resourceType tipo de recurso a mostrar; {@code null} = {@code PARKING} por defecto
     *                     (retrocompatibilidad)
     * @return {@code 200} con el calendario semanal admin del tipo de recurso solicitado
     */
    @Operation(summary = "Calendario semanal completo (ADMIN)",
            description = "Cubre ambos tipos de recurso segun `resourceType` (por defecto PARKING, "
                    + "retrocompatible).",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Calendario semanal admin"),
            @ApiResponse(responseCode = "400", description = "weekStart ausente o con formato invalido",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos (rol EMPLOYEE)",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminWeeklyCalendarResponse> getAdminCalendar(
            @Parameter(description = "Lunes de la semana (ISO-8601)", required = true, example = "2026-07-06")
            @RequestParam(name = "weekStart")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @Parameter(description = "Tipo de recurso a mostrar; por defecto PARKING", example = "DESK")
            @RequestParam(name = "resourceType", required = false) ResourceType resourceType) {
        ResourceType effectiveType = resourceType == null ? ResourceType.PARKING : resourceType;
        return ResponseEntity.ok(availabilityService.adminCalendar(weekStart, effectiveType));
    }

    /**
     * Devuelve la vista personal de la semana del solicitante: el estado diario de sus
     * recursos propios y los huecos libres, sin nombres de terceros (cualquier usuario
     * autenticado). Sin {@code weekStart} usa la semana actual.
     *
     * @param weekStart      lunes de la semana; opcional (semana actual si se omite)
     * @param authentication autenticacion resuelta de la sesion (solicitante)
     * @return {@code 200} con la vista "Mi Semana"
     */
    @Operation(summary = "Mi Semana — recursos propios y huecos (EMPLOYEE/ADMIN)",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vista personal de la semana"),
            @ApiResponse(responseCode = "400", description = "weekStart con formato invalido",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/my-week")
    public ResponseEntity<MyWeekResponse> getMyWeek(
            @Parameter(description = "Lunes de la semana (ISO-8601); opcional", example = "2026-07-06")
            @RequestParam(name = "weekStart", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            Authentication authentication) {
        return ResponseEntity.ok(availabilityService.myWeek(authentication.getName(), weekStart));
    }
}
