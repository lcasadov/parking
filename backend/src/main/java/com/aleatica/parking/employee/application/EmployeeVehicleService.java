package com.aleatica.parking.employee.application;

import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.employee.EmployeeVehicle;
import com.aleatica.parking.employee.EmployeeVehicleRepository;
import com.aleatica.parking.employee.VehicleStatus;
import com.aleatica.parking.employee.dto.EmployeeVehicleHistoryResponse.PreviousData;
import com.aleatica.parking.employee.dto.EmployeeVehicleRequest;
import com.aleatica.parking.employee.dto.EmployeeVehicleResponse;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso del CRUD de vehiculos de empleado (change {@code employee-vehicles}); reservado al
 * {@code ADMIN} por el controlador. Acota todo por {@code employeeId} (pertenencia 1:N), normaliza
 * la matricula (trim + mayusculas) y garantiza su unicidad por empleado (409 via
 * {@link EmployeeVehicleConflictException}). El borrado del empleado elimina los vehiculos por la
 * FK {@code ON DELETE CASCADE} de la BD, no aqui.
 */
@Service
public class EmployeeVehicleService {

    private static final String MSG_EMPLOYEE_NOT_FOUND = "Empleado no encontrado: ";
    private static final String MSG_VEHICLE_NOT_FOUND = "Vehiculo no encontrado: ";
    private static final String FIELD_PLATE = "licensePlate";
    private static final String MSG_PLATE_TAKEN = "El empleado ya tiene un vehiculo con esa matricula.";

    private final EmployeeVehicleRepository vehicleRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeVehicleHistoryRecorder historyRecorder;

    public EmployeeVehicleService(
            EmployeeVehicleRepository vehicleRepository,
            EmployeeRepository employeeRepository,
            EmployeeVehicleHistoryRecorder historyRecorder) {
        this.vehicleRepository = vehicleRepository;
        this.employeeRepository = employeeRepository;
        this.historyRecorder = historyRecorder;
    }

    @Transactional(readOnly = true)
    public List<EmployeeVehicleResponse> list(Long employeeId) {
        requireEmployee(employeeId);
        return vehicleRepository.findByEmployeeIdOrderByIdAsc(employeeId).stream()
                .map(EmployeeVehicleResponse::from)
                .toList();
    }

    /** Alta por el ADMIN: el vehiculo nace {@code APPROVED} (validado). */
    @Transactional
    public EmployeeVehicleResponse create(Long employeeId, EmployeeVehicleRequest request) {
        return createWithStatus(employeeId, request, VehicleStatus.APPROVED);
    }

    /**
     * Alta por el propio empleado (self-service): el vehiculo nace {@code PENDING} de validacion.
     */
    @Transactional
    public EmployeeVehicleResponse createForEmployee(Long employeeId, EmployeeVehicleRequest request) {
        EmployeeVehicleResponse created = createWithStatus(employeeId, request, VehicleStatus.PENDING);
        historyRecorder.recordCreated(created.id(), VehicleStatus.PENDING, employeeId, false);
        return created;
    }

    private EmployeeVehicleResponse createWithStatus(
            Long employeeId, EmployeeVehicleRequest request, VehicleStatus status) {
        requireEmployee(employeeId);
        String plate = normalizePlate(request.licensePlate());
        if (vehicleRepository.existsByEmployeeIdAndLicensePlate(employeeId, plate)) {
            throw new EmployeeVehicleConflictException(FIELD_PLATE, MSG_PLATE_TAKEN);
        }
        EmployeeVehicle vehicle = EmployeeVehicle.create(
                employeeId, plate, trimToNull(request.brand()), trimToNull(request.model()),
                trimToNull(request.color()), status);
        return EmployeeVehicleResponse.from(vehicleRepository.save(vehicle));
    }

    /** Edicion por el ADMIN: conserva el estado de validacion actual del vehiculo. */
    @Transactional
    public EmployeeVehicleResponse update(Long employeeId, Long vehicleId, EmployeeVehicleRequest request) {
        return applyUpdate(employeeId, vehicleId, request, false);
    }

    /**
     * Edicion por el propio empleado (self-service): tras aplicar los cambios, el vehiculo vuelve a
     * {@code PENDING} de validacion (cualquier cambio invalida una aprobacion/rechazo previos).
     */
    @Transactional
    public EmployeeVehicleResponse updateForEmployee(
            Long employeeId, Long vehicleId, EmployeeVehicleRequest request) {
        return applyUpdate(employeeId, vehicleId, request, true);
    }

    private EmployeeVehicleResponse applyUpdate(
            Long employeeId, Long vehicleId, EmployeeVehicleRequest request, boolean revalidate) {
        EmployeeVehicle vehicle = requireVehicle(employeeId, vehicleId);
        String plate = normalizePlate(request.licensePlate());
        if (vehicleRepository.existsByEmployeeIdAndLicensePlateAndIdNot(employeeId, plate, vehicleId)) {
            throw new EmployeeVehicleConflictException(FIELD_PLATE, MSG_PLATE_TAKEN);
        }
        PreviousData previous = revalidate ? historyRecorder.snapshotOf(vehicle) : null;
        vehicle.update(
                plate, trimToNull(request.brand()), trimToNull(request.model()), trimToNull(request.color()));
        if (revalidate) {
            vehicle.markPending();
        }
        EmployeeVehicleResponse saved = EmployeeVehicleResponse.from(vehicleRepository.save(vehicle));
        if (revalidate) {
            historyRecorder.recordEdited(vehicle.getId(), previous, VehicleStatus.PENDING, employeeId);
        }
        return saved;
    }

    /** Borrado por el ADMIN: borra el vehiculo directamente (autoridad del admin). */
    @Transactional
    public void delete(Long employeeId, Long vehicleId) {
        vehicleRepository.delete(requireVehicle(employeeId, vehicleId));
    }

    /**
     * Solicitud de borrado por el propio empleado. Si el vehiculo esta {@code PENDING} o
     * {@code REJECTED} (aun sin tramite ni validacion), se borra directamente y se devuelve
     * {@link Optional#empty()}. Si esta {@code IN_PROGRESS} o {@code APPROVED}, no se borra: queda
     * {@code PENDING_DELETION} y se devuelve el vehiculo (para avisar al admin de la solicitud).
     */
    @Transactional
    public Optional<EmployeeVehicleResponse> requestDeletionForEmployee(Long employeeId, Long vehicleId) {
        EmployeeVehicle vehicle = requireVehicle(employeeId, vehicleId);
        VehicleStatus status = vehicle.getStatus();
        if (status == VehicleStatus.PENDING || status == VehicleStatus.REJECTED) {
            vehicleRepository.delete(vehicle);
            return Optional.empty();
        }
        if (status != VehicleStatus.PENDING_DELETION) {
            vehicle.requestDeletion();
            vehicleRepository.save(vehicle);
            historyRecorder.recordDeletionRequested(vehicle.getId(), status, employeeId);
        }
        return Optional.of(EmployeeVehicleResponse.from(vehicle));
    }

    private void requireEmployee(Long employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new EntityNotFoundException(MSG_EMPLOYEE_NOT_FOUND + employeeId);
        }
    }

    // Carga el vehiculo acotado al empleado: 404 si el empleado o el vehiculo no existen, o si el
    // vehiculo pertenece a otro empleado (no se filtra un vehiculo ajeno).
    private EmployeeVehicle requireVehicle(Long employeeId, Long vehicleId) {
        requireEmployee(employeeId);
        return vehicleRepository.findByIdAndEmployeeId(vehicleId, employeeId)
                .orElseThrow(() -> new EntityNotFoundException(MSG_VEHICLE_NOT_FOUND + vehicleId));
    }

    // Normaliza la matricula (trim + mayusculas) para comparar/persistir sin duplicados equivalentes.
    private static String normalizePlate(String plate) {
        return plate.trim().toUpperCase(Locale.ROOT);
    }

    // Convierte un opcional en blanco a null (marca/modelo/color vacios se guardan como null).
    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
