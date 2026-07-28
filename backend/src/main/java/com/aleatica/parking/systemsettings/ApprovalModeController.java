package com.aleatica.parking.systemsettings;

import com.aleatica.parking.exception.ApiError;
import com.aleatica.parking.systemsettings.application.SystemSettingsService;
import com.aleatica.parking.systemsettings.dto.ApprovalModeResponse;
import com.aleatica.parking.systemsettings.dto.ParkingAddressResponse;
import com.aleatica.parking.systemsettings.dto.WeekendReservableResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de lectura del modo de aprobacion global, accesible a <strong>cualquier</strong>
 * empleado autenticado (sin restriccion de rol: {@code EMPLOYEE}, {@code AGENCIA} o
 * {@code ADMIN}), a diferencia de {@link SystemSettingsController#getSettings()} (reservado a
 * {@code ADMIN}, que ademas expone la trazabilidad del ultimo cambio).
 *
 * <p>Un {@code EMPLOYEE} necesita saber si su proxima solicitud nacera {@code PENDING}
 * ({@code MANUAL}) o {@code APPROVED} ({@code AUTOMATIC}) antes de enviarla; este endpoint
 * expone SOLO ese dato (nunca {@code updatedById}/{@code updatedAt}, que si figuran en el
 * ajuste completo del ADMIN). El adaptador web no contiene logica de negocio: delega en
 * {@link SystemSettingsService#approvalMode()} y trabaja con un DTO propio (nunca la entidad
 * JPA, S4684).</p>
 */
@Tag(name = "SystemSettings", description = "Ajuste global del sistema (modo de aprobacion)")
@RestController
@RequestMapping("/api/v1/settings")
public class ApprovalModeController {

    private static final String SESSION_COOKIE = "sessionCookie";

    private final SystemSettingsService systemSettingsService;

    /**
     * @param systemSettingsService casos de uso del ajuste global
     */
    public ApprovalModeController(SystemSettingsService systemSettingsService) {
        this.systemSettingsService = systemSettingsService;
    }

    /**
     * Devuelve el modo de aprobacion global vigente, sin trazabilidad (cualquier empleado
     * autenticado: {@code EMPLOYEE}/{@code AGENCIA}/{@code ADMIN}).
     *
     * @return {@code 200} con el modo vigente
     */
    @Operation(summary = "Lee el modo de aprobacion global (cualquier autenticado)",
            description = "Expone solo el modo de aprobacion vigente (MANUAL/AUTOMATIC), sin la "
                    + "trazabilidad del ultimo cambio; a diferencia de GET /admin/settings, no "
                    + "requiere rol ADMIN.",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Modo de aprobacion vigente"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/approval-mode")
    public ResponseEntity<ApprovalModeResponse> getApprovalMode() {
        return ResponseEntity.ok(ApprovalModeResponse.of(systemSettingsService.approvalMode()));
    }

    /**
     * Devuelve la direccion del parking vigente, sin trazabilidad (cualquier empleado autenticado:
     * {@code EMPLOYEE}/{@code AGENCIA}/{@code ADMIN}); change
     * {@code reservas-employee-admin-reassign}. "Mi Semana" la usa para el boton "Ir al parking"
     * que abre Google Maps.
     *
     * @return {@code 200} con la direccion vigente ({@code null} si sin configurar)
     */
    @Operation(summary = "Lee la direccion del parking (cualquier autenticado)",
            description = "Expone solo la direccion postal del parking (para el boton 'Ir al "
                    + "parking'), sin el modo de aprobacion ni la trazabilidad; a diferencia de "
                    + "GET /admin/settings, no requiere rol ADMIN.",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Direccion del parking vigente"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/parking-address")
    public ResponseEntity<ParkingAddressResponse> getParkingAddress() {
        return ResponseEntity.ok(systemSettingsService.parkingAddress());
    }

    /**
     * Devuelve el permiso de reservas en fin de semana vigente, sin trazabilidad (cualquier
     * empleado autenticado); change {@code reservas-employee-admin-reassign}. La UI lo usa para
     * decidir si ofrecer sabado/domingo como fechas reservables.
     *
     * @return {@code 200} con el flag vigente
     */
    @Operation(summary = "Lee el permiso de reservas en fin de semana (cualquier autenticado)",
            description = "Expone solo el flag weekendReservable, sin el modo de aprobacion ni la "
                    + "trazabilidad; a diferencia de GET /admin/settings, no requiere rol ADMIN.",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Permiso de fin de semana vigente"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/weekend-reservable")
    public ResponseEntity<WeekendReservableResponse> getWeekendReservable() {
        return ResponseEntity.ok(systemSettingsService.weekendReservableView());
    }
}
