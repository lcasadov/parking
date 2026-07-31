package com.aleatica.parking.employee;

import com.aleatica.parking.employee.application.EmployeeVehicleReviewService;
import com.aleatica.parking.employee.dto.EmployeeVehicleHistoryResponse;
import com.aleatica.parking.employee.dto.EmployeeVehicleRejectRequest;
import com.aleatica.parking.employee.dto.EmployeeVehicleReviewResponse;
import com.aleatica.parking.employee.dto.EmployeeVehicleStatusChangeRequest;
import com.aleatica.parking.employee.dto.PageResponse;
import com.aleatica.parking.exception.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bandeja de validación de vehículos de empleado, reservada al {@code ADMIN} (change
 * {@code employee-vehicle-self-service}, Fase 2). Listar por estado con datos del empleado, contar
 * pendientes, ver el histórico y decidir: en trámite / aprobar / rechazar (motivo libre) / procesar
 * los "pendiente de borrado" (confirmar o restaurar). Trabaja siempre con DTOs (S4684).
 */
@Tag(name = "EmployeeVehicleReview", description = "Validación de vehículos de empleado (solo ADMIN)")
@RestController
@RequestMapping("/api/v1/employee-vehicles")
@PreAuthorize("hasRole('ADMIN')")
public class EmployeeVehicleReviewController {

    private static final String SESSION_COOKIE = "sessionCookie";

    private final EmployeeVehicleReviewService reviewService;

    public EmployeeVehicleReviewController(EmployeeVehicleReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Operation(summary = "Lista vehículos para validar (filtrable por estado)",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de vehículos"),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public ResponseEntity<PageResponse<EmployeeVehicleReviewResponse>> list(
            @RequestParam(name = "status", required = false) List<VehicleStatus> status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC)
            Pageable pageable) {
        return ResponseEntity.ok(reviewService.list(status, pageable));
    }

    @Operation(summary = "Cuenta de vehículos pendientes de acción (badge)",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @GetMapping("/pending-count")
    public ResponseEntity<Map<String, Long>> pendingCount() {
        return ResponseEntity.ok(Map.of("count", reviewService.pendingCount()));
    }

    @Operation(summary = "Recuento de vehículos por estado (contadores de filtros)",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @GetMapping("/counts")
    public ResponseEntity<Map<VehicleStatus, Long>> counts() {
        return ResponseEntity.ok(reviewService.countsByStatus());
    }

    @Operation(summary = "Histórico de cambios de un vehículo",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Histórico del vehículo"),
            @ApiResponse(responseCode = "404", description = "Vehículo no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{vehicleId}/history")
    public ResponseEntity<List<EmployeeVehicleHistoryResponse>> history(@PathVariable Long vehicleId) {
        return ResponseEntity.ok(reviewService.history(vehicleId));
    }

    @Operation(summary = "Marcar un vehículo en trámite",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vehículo en trámite"),
            @ApiResponse(responseCode = "404", description = "Vehículo no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "El vehículo ya fue procesado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{vehicleId}/in-progress")
    public ResponseEntity<EmployeeVehicleReviewResponse> markInProgress(
            @PathVariable Long vehicleId, Authentication authentication) {
        return ResponseEntity.ok(reviewService.markInProgress(vehicleId, authentication.getName()));
    }

    @Operation(summary = "Aprobar un vehículo", security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vehículo aprobado"),
            @ApiResponse(responseCode = "404", description = "Vehículo no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "El vehículo ya fue procesado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{vehicleId}/approve")
    public ResponseEntity<EmployeeVehicleReviewResponse> approve(
            @PathVariable Long vehicleId, Authentication authentication) {
        return ResponseEntity.ok(reviewService.approve(vehicleId, authentication.getName()));
    }

    @Operation(summary = "Rechazar un vehículo con motivo (texto libre)",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vehículo rechazado"),
            @ApiResponse(responseCode = "400", description = "Motivo ausente",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Vehículo no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "El vehículo ya fue procesado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{vehicleId}/reject")
    public ResponseEntity<EmployeeVehicleReviewResponse> reject(
            @PathVariable Long vehicleId,
            @Valid @RequestBody EmployeeVehicleRejectRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(
                reviewService.reject(vehicleId, request.reason(), authentication.getName()));
    }

    @Operation(summary = "Cambiar manualmente el estado de un vehículo (corregir un estado)",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado cambiado"),
            @ApiResponse(responseCode = "400", description = "Estado ausente o motivo requerido",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Vehículo no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Transición no permitida",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{vehicleId}/status")
    public ResponseEntity<EmployeeVehicleReviewResponse> changeStatus(
            @PathVariable Long vehicleId,
            @Valid @RequestBody EmployeeVehicleStatusChangeRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(reviewService.changeStatus(
                vehicleId, request.status(), request.reason(), authentication.getName()));
    }

    @Operation(summary = "Confirmar el borrado de un vehículo pendiente de borrado",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Vehículo borrado"),
            @ApiResponse(responseCode = "404", description = "Vehículo no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "El vehículo no está pendiente de borrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{vehicleId}/confirm-deletion")
    public ResponseEntity<Void> confirmDeletion(
            @PathVariable Long vehicleId, Authentication authentication) {
        reviewService.confirmDeletion(vehicleId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Restaurar un vehículo pendiente de borrado a su estado previo",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vehículo restaurado"),
            @ApiResponse(responseCode = "404", description = "Vehículo no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "El vehículo no está pendiente de borrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{vehicleId}/restore")
    public ResponseEntity<EmployeeVehicleReviewResponse> restore(
            @PathVariable Long vehicleId, Authentication authentication) {
        return ResponseEntity.ok(reviewService.restore(vehicleId, authentication.getName()));
    }
}
