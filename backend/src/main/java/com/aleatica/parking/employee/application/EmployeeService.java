package com.aleatica.parking.employee.application;

import com.aleatica.parking.auth.domain.ClockPort;
import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.employee.dto.EmployeeCreateRequest;
import com.aleatica.parking.employee.dto.EmployeeResetPasswordResponse;
import com.aleatica.parking.employee.dto.EmployeeResponse;
import com.aleatica.parking.employee.dto.EmployeeUpdateRequest;
import com.aleatica.parking.employee.dto.PageResponse;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Casos de uso de gestion de empleados (alta, edicion, baja logica, reactivacion,
 * reset de contrasena y listado), reservados al rol {@code ADMIN}.
 *
 * <p>Arquitectura hexagonal: la logica de negocio no depende de la web y nunca
 * devuelve entidades JPA (convierte a DTO antes de salir, OWASP API3). La
 * unicidad de {@code login}/{@code email} se comprueba aqui (mensaje claro) y la
 * garantiza el indice unico en BD frente a concurrencia (design §Decisions).</p>
 */
@Service
public class EmployeeService {

    private static final String FIELD_LOGIN = "login";
    private static final String FIELD_EMAIL = "email";
    private static final String MSG_LOGIN_TAKEN = "El login ya esta en uso";
    private static final String MSG_EMAIL_TAKEN = "El email ya esta en uso";
    private static final String MSG_NOT_FOUND = "Empleado no encontrado: ";

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final ClockPort clock;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;
    private final PasswordResetNotifier passwordResetNotifier;
    private final Phase phase;

    /**
     * @param employeeRepository         repositorio de empleados
     * @param passwordEncoder            codificador BCrypt (coste 12)
     * @param clock                      reloj inyectable (UTC)
     * @param temporaryPasswordGenerator generador de contrasenas temporales seguras
     * @param passwordResetNotifier      notificador de reset (email en Fase 2)
     * @param phase                      fase de despliegue ({@code parking.phase})
     */
    public EmployeeService(
            EmployeeRepository employeeRepository,
            PasswordEncoder passwordEncoder,
            ClockPort clock,
            TemporaryPasswordGenerator temporaryPasswordGenerator,
            PasswordResetNotifier passwordResetNotifier,
            @Value("${parking.phase:PHASE_1}") Phase phase) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.temporaryPasswordGenerator = temporaryPasswordGenerator;
        this.passwordResetNotifier = passwordResetNotifier;
        this.phase = phase;
    }

    /**
     * Lista empleados de forma paginada, filtrando por texto libre y estado.
     *
     * @param query    texto de busqueda libre; en blanco o {@code null} no filtra
     * @param active   filtro por estado de baja logica; {@code null} no filtra
     * @param pageable pagina y orden solicitados
     * @return pagina de empleados (DTO) con sus metadatos
     */
    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponse> list(String query, Boolean active, Pageable pageable) {
        String normalized = StringUtils.hasText(query) ? query.trim() : null;
        Page<Employee> page = employeeRepository.search(normalized, active, pageable);
        return PageResponse.from(page, EmployeeResponse::from);
    }

    /**
     * Da de alta un empleado validando la unicidad de {@code login} y {@code email}.
     *
     * @param request datos de alta ya validados sintacticamente
     * @return el empleado creado ({@code active = true}, {@code enabled = true})
     * @throws EmployeeConflictException si el {@code login} o el {@code email} ya existen
     */
    @Transactional
    public EmployeeResponse create(EmployeeCreateRequest request) {
        if (employeeRepository.existsByLogin(request.login())) {
            throw new EmployeeConflictException(FIELD_LOGIN, MSG_LOGIN_TAKEN);
        }
        if (employeeRepository.existsByEmail(request.email())) {
            throw new EmployeeConflictException(FIELD_EMAIL, MSG_EMAIL_TAKEN);
        }
        Employee employee = Employee.register(
                request.firstName(), request.lastName(), request.login(),
                request.email(), request.role(), request.authOriginOrDefault(),
                request.category());
        employee.setDepartment(request.department());
        employee.setMobilePhone(request.mobilePhone());
        employee.setLicensePlate(request.licensePlate());
        employee.setCorporate(request.corporate());
        return EmployeeResponse.from(employeeRepository.save(employee));
    }

    /**
     * Modifica un empleado existente, validando que el {@code email} no colisione
     * con el de otro empleado (excluyendo al propio). El {@code login} es inmutable.
     *
     * @param id      id del empleado a modificar
     * @param request datos de edicion ya validados sintacticamente
     * @return el empleado actualizado
     * @throws EntityNotFoundException   si el empleado no existe
     * @throws EmployeeConflictException si el {@code email} lo usa otro empleado
     */
    @Transactional
    public EmployeeResponse update(Long id, EmployeeUpdateRequest request) {
        Employee employee = findOrThrow(id);
        if (employeeRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new EmployeeConflictException(FIELD_EMAIL, MSG_EMAIL_TAKEN);
        }
        employee.setFirstName(request.firstName());
        employee.setLastName(request.lastName());
        employee.setEmail(request.email());
        employee.setDepartment(request.department());
        employee.setMobilePhone(request.mobilePhone());
        employee.setLicensePlate(request.licensePlate());
        employee.setCorporate(request.corporate());
        employee.setRole(request.role());
        employee.setCategory(request.category());
        employee.setUpdatedAt(clock.now());
        return EmployeeResponse.from(employeeRepository.save(employee));
    }

    /**
     * Da de baja logicamente a un empleado ({@code active = false}) sin borrar la
     * fila. Idempotente: dar de baja a uno ya inactivo no produce error.
     *
     * @param id id del empleado
     * @throws EntityNotFoundException si el empleado no existe
     */
    @Transactional
    public void deactivate(Long id) {
        Employee employee = findOrThrow(id);
        employee.setActive(false);
        employeeRepository.save(employee);
    }

    /**
     * Reactiva a un empleado dado de baja ({@code active = true}). Idempotente:
     * reactivar a uno ya activo no produce error.
     *
     * @param id id del empleado
     * @throws EntityNotFoundException si el empleado no existe
     */
    @Transactional
    public void reactivate(Long id) {
        Employee employee = findOrThrow(id);
        employee.setActive(true);
        employeeRepository.save(employee);
    }

    /**
     * Resetea la contrasena de un empleado: genera una temporal, la persiste como
     * hash y fija {@code passwordMustChange = true}. No altera {@code enabled} ni
     * {@code active}. Segun la fase, devuelve la temporal (Fase 1) o la envia por
     * email (Fase 2).
     *
     * @param id id del empleado
     * @return la respuesta con la temporal (Fase 1) o sin ella (Fase 2)
     * @throws EntityNotFoundException si el empleado no existe
     */
    @Transactional
    public EmployeeResetPasswordResponse resetPassword(Long id) {
        Employee employee = findOrThrow(id);
        String temporaryPassword = temporaryPasswordGenerator.generate();
        employee.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        employee.setPasswordMustChange(true);
        employee.setLastPasswordChangeAt(clock.now());
        employeeRepository.save(employee);

        if (phase == Phase.PHASE_2) {
            passwordResetNotifier.notifyReset(employee, temporaryPassword);
            return EmployeeResetPasswordResponse.phase2();
        }
        return EmployeeResetPasswordResponse.phase1(temporaryPassword);
    }

    /**
     * Recupera todos los empleados para exportacion (ordenados por id).
     *
     * <p>El detalle del formato (CSV/XLSX) pertenece a la capability
     * {@code exports}; aqui solo se provee el conjunto de datos.</p>
     *
     * @return la lista completa de empleados como DTO
     */
    @Transactional(readOnly = true)
    public List<EmployeeResponse> exportAll() {
        return employeeRepository.findAll().stream().map(EmployeeResponse::from).toList();
    }

    private Employee findOrThrow(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(MSG_NOT_FOUND + id));
    }
}
