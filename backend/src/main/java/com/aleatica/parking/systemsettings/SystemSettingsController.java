package com.aleatica.parking.systemsettings;

import com.aleatica.parking.exception.ApiError;
import com.aleatica.parking.systemsettings.application.SystemSettingsService;
import com.aleatica.parking.systemsettings.dto.SystemSettingsResponse;
import com.aleatica.parking.systemsettings.dto.UpdateApprovalModeRequest;
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
}
