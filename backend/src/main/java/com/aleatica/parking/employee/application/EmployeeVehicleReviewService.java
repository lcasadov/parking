package com.aleatica.parking.employee.application;

import com.aleatica.parking.auth.domain.ClockPort;
import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.employee.EmployeeVehicle;
import com.aleatica.parking.employee.EmployeeVehicleRepository;
import com.aleatica.parking.employee.VehicleStatus;
import com.aleatica.parking.employee.dto.EmployeeVehicleHistoryResponse;
import com.aleatica.parking.employee.dto.EmployeeVehicleReviewResponse;
import com.aleatica.parking.employee.dto.EmployeeVehicleReviewResponse.Owner;
import com.aleatica.parking.employee.dto.PageResponse;
import com.aleatica.parking.request.application.RejectionReasonRequiredException;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de la bandeja de validación de vehículos del administrador (change
 * {@code employee-vehicle-self-service}, Fase 2): listar por estado con datos del empleado, contar
 * pendientes, poner en trámite / aprobar / rechazar (con motivo libre) y procesar los "pendiente de
 * borrado" (confirmar o restaurar). Cada decisión registra histórico y avisa al empleado. Una
 * acción no aplicable al estado actual (p.ej. ya procesado por otro admin) da
 * {@link VehicleReviewConflictException} (409).
 */
@Service
public class EmployeeVehicleReviewService {

    private static final String MSG_VEHICLE_NOT_FOUND = "Vehiculo no encontrado: ";
    private static final String MSG_ADMIN_NOT_FOUND = "Administrador no encontrado: ";
    private static final String MSG_NOT_REVIEWABLE = "El vehículo ya fue procesado.";
    private static final String MSG_NOT_PENDING_DELETION = "El vehículo no está pendiente de borrado.";
    private static final String NONE = "—";

    private final EmployeeVehicleRepository vehicleRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeVehicleHistoryRecorder historyRecorder;
    private final EmployeeVehicleAdminNotifier notifier;
    private final ClockPort clock;

    public EmployeeVehicleReviewService(
            EmployeeVehicleRepository vehicleRepository,
            EmployeeRepository employeeRepository,
            EmployeeVehicleHistoryRecorder historyRecorder,
            EmployeeVehicleAdminNotifier notifier,
            ClockPort clock) {
        this.vehicleRepository = vehicleRepository;
        this.employeeRepository = employeeRepository;
        this.historyRecorder = historyRecorder;
        this.notifier = notifier;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployeeVehicleReviewResponse> list(List<VehicleStatus> statuses, Pageable pageable) {
        Page<EmployeeVehicle> page = pageFor(statuses, pageable);
        Map<Long, Employee> owners = loadOwners(page.getContent());
        List<EmployeeVehicleReviewResponse> content = page.getContent().stream()
                .map(vehicle -> EmployeeVehicleReviewResponse.of(vehicle, ownerOf(vehicle, owners)))
                .toList();
        return new PageResponse<>(
                content, page.getTotalElements(), page.getTotalPages(),
                page.getSize(), page.getNumber(), page.isFirst(), page.isLast());
    }

    // Resuelve la página según los estados pedidos: ninguno -> todos; uno -> findByStatus; varios ->
    // findByStatusIn (el tab "Pendientes" combina PENDING + IN_PROGRESS + PENDING_DELETION).
    private Page<EmployeeVehicle> pageFor(List<VehicleStatus> statuses, Pageable pageable) {
        if (statuses == null || statuses.isEmpty()) {
            return vehicleRepository.findAll(pageable);
        }
        if (statuses.size() == 1) {
            return vehicleRepository.findByStatus(statuses.get(0), pageable);
        }
        return vehicleRepository.findByStatusIn(statuses, pageable);
    }

    @Transactional(readOnly = true)
    public long pendingCount() {
        return vehicleRepository.countByStatusIn(
                List.of(VehicleStatus.PENDING, VehicleStatus.PENDING_DELETION));
    }

    /** Recuento por estado (todos los estados presentes, con 0 por defecto) para los filtros. */
    @Transactional(readOnly = true)
    public Map<VehicleStatus, Long> countsByStatus() {
        Map<VehicleStatus, Long> counts = new EnumMap<>(VehicleStatus.class);
        for (VehicleStatus status : VehicleStatus.values()) {
            counts.put(status, 0L);
        }
        for (EmployeeVehicleRepository.StatusCount row : vehicleRepository.countGroupedByStatus()) {
            counts.put(row.getStatus(), row.getCount());
        }
        return counts;
    }

    @Transactional(readOnly = true)
    public List<EmployeeVehicleHistoryResponse> history(Long vehicleId) {
        requireVehicle(vehicleId);
        return historyRecorder.list(vehicleId);
    }

    @Transactional
    public EmployeeVehicleReviewResponse markInProgress(Long vehicleId, String adminLogin) {
        EmployeeVehicle vehicle = requireVehicle(vehicleId);
        if (vehicle.getStatus() == VehicleStatus.APPROVED
                || vehicle.getStatus() == VehicleStatus.PENDING_DELETION) {
            throw new VehicleReviewConflictException(MSG_NOT_REVIEWABLE);
        }
        VehicleStatus from = vehicle.getStatus();
        Long adminId = requireAdmin(adminLogin).getId();
        vehicle.markInProgress(adminId, now());
        vehicleRepository.save(vehicle);
        historyRecorder.recordStatusChange(vehicleId, from, VehicleStatus.IN_PROGRESS, null, adminId);
        notifier.vehicleInProgress(vehicle.getEmployeeId(), vehicle.getLicensePlate());
        return EmployeeVehicleReviewResponse.of(vehicle, ownerOf(vehicle));
    }

    @Transactional
    public EmployeeVehicleReviewResponse approve(Long vehicleId, String adminLogin) {
        EmployeeVehicle vehicle = requireReviewable(vehicleId);
        VehicleStatus from = vehicle.getStatus();
        Long adminId = requireAdmin(adminLogin).getId();
        vehicle.approve(adminId, now());
        vehicleRepository.save(vehicle);
        historyRecorder.recordStatusChange(vehicleId, from, VehicleStatus.APPROVED, null, adminId);
        notifier.vehicleApproved(vehicle.getEmployeeId(), vehicle.getLicensePlate());
        return EmployeeVehicleReviewResponse.of(vehicle, ownerOf(vehicle));
    }

    @Transactional
    public EmployeeVehicleReviewResponse reject(Long vehicleId, String reason, String adminLogin) {
        EmployeeVehicle vehicle = requireReviewable(vehicleId);
        VehicleStatus from = vehicle.getStatus();
        Long adminId = requireAdmin(adminLogin).getId();
        String trimmed = reason.trim();
        vehicle.reject(trimmed, adminId, now());
        vehicleRepository.save(vehicle);
        historyRecorder.recordStatusChange(vehicleId, from, VehicleStatus.REJECTED, trimmed, adminId);
        notifier.vehicleRejected(vehicle.getEmployeeId(), vehicle.getLicensePlate(), trimmed);
        return EmployeeVehicleReviewResponse.of(vehicle, ownerOf(vehicle));
    }

    /**
     * Cambio manual de estado por el administrador (corregir un estado, p.ej. un aprobado por
     * error). Admite PENDING / IN_PROGRESS / APPROVED / REJECTED (para REJECTED exige motivo). No
     * admite tocar un {@code PENDING_DELETION} (usar confirmar/restaurar) ni fijar
     * {@code PENDING_DELETION} (lo inicia el empleado).
     */
    @Transactional
    public EmployeeVehicleReviewResponse changeStatus(
            Long vehicleId, VehicleStatus target, String reason, String adminLogin) {
        if (target == VehicleStatus.PENDING_DELETION) {
            throw new VehicleReviewConflictException("Estado destino no permitido.");
        }
        EmployeeVehicle vehicle = requireVehicle(vehicleId);
        if (vehicle.getStatus() == VehicleStatus.PENDING_DELETION) {
            throw new VehicleReviewConflictException(MSG_NOT_PENDING_DELETION);
        }
        VehicleStatus from = vehicle.getStatus();
        Long adminId = requireAdmin(adminLogin).getId();
        String trimmedReason = applyStatus(vehicle, target, reason, adminId);
        vehicleRepository.save(vehicle);
        historyRecorder.recordStatusChange(vehicleId, from, target, trimmedReason, adminId);
        notifyStatusChange(vehicle, target, trimmedReason);
        return EmployeeVehicleReviewResponse.of(vehicle, ownerOf(vehicle));
    }

    private String applyStatus(EmployeeVehicle vehicle, VehicleStatus target, String reason, Long adminId) {
        switch (target) {
            case PENDING -> vehicle.markPending();
            case IN_PROGRESS -> vehicle.markInProgress(adminId, now());
            case APPROVED -> vehicle.approve(adminId, now());
            case REJECTED -> {
                String trimmed = reason == null ? "" : reason.trim();
                if (trimmed.isEmpty()) {
                    throw new RejectionReasonRequiredException("El motivo es obligatorio");
                }
                vehicle.reject(trimmed, adminId, now());
                return trimmed;
            }
            default -> throw new VehicleReviewConflictException("Estado destino no permitido.");
        }
        return null;
    }

    private void notifyStatusChange(EmployeeVehicle vehicle, VehicleStatus target, String reason) {
        Long employeeId = vehicle.getEmployeeId();
        String plate = vehicle.getLicensePlate();
        switch (target) {
            case IN_PROGRESS -> notifier.vehicleInProgress(employeeId, plate);
            case APPROVED -> notifier.vehicleApproved(employeeId, plate);
            case REJECTED -> notifier.vehicleRejected(employeeId, plate, reason);
            default -> {
                // PENDING (deshacer una decisión) no genera aviso al empleado.
            }
        }
    }

    @Transactional
    public void confirmDeletion(Long vehicleId, String adminLogin) {
        EmployeeVehicle vehicle = requireVehicle(vehicleId);
        requirePendingDeletion(vehicle);
        requireAdmin(adminLogin);
        vehicleRepository.delete(vehicle);
    }

    @Transactional
    public EmployeeVehicleReviewResponse restore(Long vehicleId, String adminLogin) {
        EmployeeVehicle vehicle = requireVehicle(vehicleId);
        requirePendingDeletion(vehicle);
        Long adminId = requireAdmin(adminLogin).getId();
        vehicle.restore();
        vehicleRepository.save(vehicle);
        historyRecorder.recordStatusChange(
                vehicleId, VehicleStatus.PENDING_DELETION, vehicle.getStatus(), null, adminId);
        return EmployeeVehicleReviewResponse.of(vehicle, ownerOf(vehicle));
    }

    private EmployeeVehicle requireVehicle(Long vehicleId) {
        return vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new EntityNotFoundException(MSG_VEHICLE_NOT_FOUND + vehicleId));
    }

    private EmployeeVehicle requireReviewable(Long vehicleId) {
        EmployeeVehicle vehicle = requireVehicle(vehicleId);
        if (!vehicle.isReviewable()) {
            throw new VehicleReviewConflictException(MSG_NOT_REVIEWABLE);
        }
        return vehicle;
    }

    private void requirePendingDeletion(EmployeeVehicle vehicle) {
        if (vehicle.getStatus() != VehicleStatus.PENDING_DELETION) {
            throw new VehicleReviewConflictException(MSG_NOT_PENDING_DELETION);
        }
    }

    private Employee requireAdmin(String login) {
        return employeeRepository.findByLogin(login)
                .orElseThrow(() -> new EntityNotFoundException(MSG_ADMIN_NOT_FOUND + login));
    }

    private Instant now() {
        return clock.now();
    }

    private Map<Long, Employee> loadOwners(List<EmployeeVehicle> vehicles) {
        List<Long> ids = vehicles.stream().map(EmployeeVehicle::getEmployeeId).distinct().toList();
        return employeeRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));
    }

    private Owner ownerOf(EmployeeVehicle vehicle) {
        return ownerOf(vehicle, loadOwners(List.of(vehicle)));
    }

    private Owner ownerOf(EmployeeVehicle vehicle, Map<Long, Employee> owners) {
        Employee employee = owners.get(vehicle.getEmployeeId());
        if (employee == null) {
            return new Owner(vehicle.getEmployeeId(), NONE, null);
        }
        String fullName = (employee.getFirstName() + " " + employee.getLastName()).trim();
        return new Owner(employee.getId(), fullName, employee.getDepartment());
    }
}
