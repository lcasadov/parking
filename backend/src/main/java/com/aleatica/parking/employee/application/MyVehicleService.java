package com.aleatica.parking.employee.application;

import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.employee.dto.EmployeeVehicleRequest;
import com.aleatica.parking.employee.dto.EmployeeVehicleResponse;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso self-service de vehículos: el propio empleado gestiona SUS vehículos desde el
 * portal (change {@code employee-vehicle-self-service}, Fase 1). Resuelve el empleado por su
 * {@code login} (nunca por un id de la ruta), delega el CRUD acotado en
 * {@link EmployeeVehicleService} (alta/edición dejan el vehículo {@code PENDING}) y, tras un alta o
 * edición, avisa a los administradores vía {@link EmployeeVehicleAdminNotifier}.
 */
@Service
public class MyVehicleService {

    private static final String MSG_EMPLOYEE_NOT_FOUND = "Empleado autenticado no encontrado: ";

    private final EmployeeRepository employeeRepository;
    private final EmployeeVehicleService vehicleService;
    private final EmployeeVehicleAdminNotifier adminNotifier;

    public MyVehicleService(
            EmployeeRepository employeeRepository,
            EmployeeVehicleService vehicleService,
            EmployeeVehicleAdminNotifier adminNotifier) {
        this.employeeRepository = employeeRepository;
        this.vehicleService = vehicleService;
        this.adminNotifier = adminNotifier;
    }

    @Transactional(readOnly = true)
    public List<EmployeeVehicleResponse> list(String login) {
        return vehicleService.list(requireEmployee(login).getId());
    }

    @Transactional
    public EmployeeVehicleResponse create(String login, EmployeeVehicleRequest request) {
        Employee employee = requireEmployee(login);
        EmployeeVehicleResponse created = vehicleService.createForEmployee(employee.getId(), request);
        adminNotifier.vehicleSubmitted(fullName(employee), created.licensePlate());
        return created;
    }

    @Transactional
    public EmployeeVehicleResponse update(String login, Long vehicleId, EmployeeVehicleRequest request) {
        Employee employee = requireEmployee(login);
        EmployeeVehicleResponse updated =
                vehicleService.updateForEmployee(employee.getId(), vehicleId, request);
        adminNotifier.vehicleSubmitted(fullName(employee), updated.licensePlate());
        return updated;
    }

    /**
     * Solicitud de borrado por el empleado. Si el vehículo aún estaba PENDING/REJECTED se borra;
     * si estaba en trámite o aprobado, queda "pendiente de borrado" y se avisa al admin.
     */
    @Transactional
    public void delete(String login, Long vehicleId) {
        Employee employee = requireEmployee(login);
        vehicleService.requestDeletionForEmployee(employee.getId(), vehicleId)
                .ifPresent(pending -> adminNotifier.deletionRequested(fullName(employee), pending.licensePlate()));
    }

    private Employee requireEmployee(String login) {
        return employeeRepository.findByLogin(login)
                .orElseThrow(() -> new EntityNotFoundException(MSG_EMPLOYEE_NOT_FOUND + login));
    }

    private static String fullName(Employee employee) {
        return (employee.getFirstName() + " " + employee.getLastName()).trim();
    }
}
