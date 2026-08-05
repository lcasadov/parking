package com.aleatica.parking.systemsettings;

import com.aleatica.parking.exception.ApiError;
import com.aleatica.parking.systemsettings.application.SystemSettingsService;
import com.aleatica.parking.systemsettings.dto.SystemSettingsResponse;
import com.aleatica.parking.systemsettings.dto.UpdateApprovalModeRequest;
import com.aleatica.parking.systemsettings.dto.UpdateParkingAddressRequest;
import com.aleatica.parking.systemsettings.dto.UpdateNotificationChannelsRequest;
import com.aleatica.parking.systemsettings.dto.UpdateWeekendReservableRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de administracion del ajuste global del sistema (leer y cambiar el modo de
 * aprobacion de solicitudes).
 *
 * <p>El adaptador web no contiene logica de negocio: delega en {@link SystemSettingsService} y
 * trabaja siempre con DTOs (nunca con la entidad JPA, S4684). Ambos endpoints estan restringidos
 * al rol {@code ADMIN} ({@code @PreAuthorize}); un {@code EMPLOYEE} recibe {@code 403}.</p>
 */
@Tag(name = "SystemSettings", description = "Ajuste global del sistema (modo de aprobacion)")
@RestController
@RequestMapping("/api/v1/admin/settings")
public class SystemSettingsController {

    private static final String SESSION_COOKIE = "sessionCookie";

    private final SystemSettingsService systemSettingsService;

    /**
     * @param systemSettingsService casos de uso del ajuste global
     */
    public SystemSettingsController(SystemSettingsService systemSettingsService) {
        this.systemSettingsService = systemSettingsService;
    }

    /**
     * Devuelve el ajuste global vigente (modo de aprobacion + trazabilidad) (solo {@code ADMIN}).
     *
     * @return {@code 200} con el ajuste vigente
     */
    @Operation(summary = "Lee el ajuste global del sistema (ADMIN)",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ajuste global vigente"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SystemSettingsResponse> getSettings() {
        return ResponseEntity.ok(systemSettingsService.current());
    }

    /**
     * Cambia el modo de aprobacion global (solo {@code ADMIN}). Un valor fuera del dominio del
     * enum se rechaza con {@code 400}.
     *
     * @param request        nuevo modo de aprobacion
     * @param authentication autenticacion resuelta de la sesion (actor del cambio)
     * @return {@code 200} con el ajuste actualizado
     */
    @Operation(summary = "Cambia el modo de aprobacion global (ADMIN)",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ajuste global actualizado"),
            @ApiResponse(responseCode = "400", description = "Modo de aprobacion invalido o ausente",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SystemSettingsResponse> updateSettings(
            @Valid @RequestBody UpdateApprovalModeRequest request, Authentication authentication) {
        return ResponseEntity.ok(systemSettingsService.updateApprovalMode(
                request.approvalMode(), authentication.getName()));
    }

    /**
     * Cambia la direccion postal del parking (solo {@code ADMIN}); change
     * {@code reservas-employee-admin-reassign}. La direccion es opcional: enviar {@code null} o
     * cadena en blanco la borra. La usa el empleado en "Mi Semana" (boton "Ir al parking").
     *
     * @param request        nueva direccion del parking (opcional)
     * @param authentication autenticacion resuelta de la sesion (actor del cambio)
     * @return {@code 200} con el ajuste actualizado (incluye {@code parkingAddress})
     */
    @Operation(summary = "Cambia la direccion del parking (ADMIN)",
            description = "Establece o borra (null/blanco) la direccion postal del parking usada "
                    + "por el boton 'Ir al parking' del empleado.",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ajuste global actualizado"),
            @ApiResponse(responseCode = "400", description = "Direccion demasiado larga (>500)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/parking-address")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SystemSettingsResponse> updateParkingAddress(
            @Valid @RequestBody UpdateParkingAddressRequest request, Authentication authentication) {
        return ResponseEntity.ok(systemSettingsService.updateParkingAddress(
                request.parkingAddress(), request.parkingLat(), request.parkingLng(),
                authentication.getName()));
    }

    /**
     * Cambia el permiso de reservas en fin de semana (solo {@code ADMIN}); change
     * {@code reservas-employee-admin-reassign}. Con {@code false} (por defecto) se rechaza crear
     * solicitudes para sabado/domingo; con {@code true} se permiten.
     *
     * @param request        nuevo valor del flag (obligatorio)
     * @param authentication autenticacion resuelta de la sesion (actor del cambio)
     * @return {@code 200} con el ajuste actualizado (incluye {@code weekendReservable})
     */
    @Operation(summary = "Cambia el permiso de reservas en fin de semana (ADMIN)",
            description = "Activa o desactiva la posibilidad de crear solicitudes en sabado/domingo.",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ajuste global actualizado"),
            @ApiResponse(responseCode = "400", description = "Valor ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/weekend-reservable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SystemSettingsResponse> updateWeekendReservable(
            @Valid @RequestBody UpdateWeekendReservableRequest request, Authentication authentication) {
        return ResponseEntity.ok(systemSettingsService.updateWeekendReservable(
                request.weekendReservable(), authentication.getName()));
    }

    /**
     * Cambia los interruptores globales de canal de notificacion (email/push), solo {@code ADMIN}
     * (change {@code push-notifications}).
     *
     * @param request        los dos flags (obligatorios)
     * @param authentication autenticacion de la sesion (actor del cambio)
     * @return {@code 200} con el ajuste actualizado
     */
    @Operation(summary = "Cambia los interruptores globales de canal (email/push) (ADMIN)",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ajuste actualizado"),
            @ApiResponse(responseCode = "400", description = "Validacion",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "No es ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/notification-channels")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SystemSettingsResponse> updateNotificationChannels(
            @Valid @RequestBody UpdateNotificationChannelsRequest request, Authentication authentication) {
        return ResponseEntity.ok(systemSettingsService.updateNotificationChannels(
                request.emailNotificationsEnabled(), request.pushNotificationsEnabled(),
                authentication.getName()));
    }
}
