package com.aleatica.parking.systemsettings.application;

import com.aleatica.parking.auth.domain.ClockPort;
import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.systemsettings.domain.ApprovalMode;
import com.aleatica.parking.systemsettings.domain.SystemSettings;
import com.aleatica.parking.systemsettings.domain.SystemSettingsRepositoryPort;
import com.aleatica.parking.systemsettings.dto.SystemSettingsResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso del ajuste global del sistema: leer el modo de aprobacion vigente y cambiarlo
 * (accion del {@code ADMIN}).
 *
 * <p>Arquitectura hexagonal: la logica no depende de la web y nunca devuelve entidades JPA
 * (OWASP API3). El ajuste es un singleton (fila unica {@code id = 1}); si aun no existiera la
 * fila, {@link #approvalMode()} devuelve {@link ApprovalMode#MANUAL} (retrocompatible, sin
 * asumir silenciosamente {@code AUTOMATIC}). El cambio de modo registra el actor y el instante,
 * y dispara la auditoria {@code AFTER_COMMIT} via {@link SystemSettingsAuditEvent}.</p>
 */
@Service
public class SystemSettingsService {

    private static final String MSG_ACTOR_NOT_FOUND = "Usuario de sesion no encontrado: ";

    private final SystemSettingsRepositoryPort settingsRepository;
    private final EmployeeRepository employeeRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ClockPort clock;

    /**
     * @param settingsRepository puerto de persistencia del ajuste global (dominio)
     * @param employeeRepository repositorio de empleados (resolucion del actor)
     * @param eventPublisher     publicador de eventos de auditoria
     * @param clock              reloj inyectable para la marca de tiempo del cambio
     */
    public SystemSettingsService(
            SystemSettingsRepositoryPort settingsRepository,
            EmployeeRepository employeeRepository,
            ApplicationEventPublisher eventPublisher,
            ClockPort clock) {
        this.settingsRepository = settingsRepository;
        this.employeeRepository = employeeRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    /**
     * Devuelve el modo de aprobacion global vigente. Si la fila unica aun no existiera,
     * devuelve {@link ApprovalMode#MANUAL} (por defecto retrocompatible).
     *
     * @return el modo de aprobacion vigente
     */
    @Transactional(readOnly = true)
    public ApprovalMode approvalMode() {
        return settingsRepository.find()
                .map(SystemSettings::getApprovalMode)
                .orElse(ApprovalMode.MANUAL);
    }

    /**
     * Devuelve el ajuste global vigente (modo + trazabilidad) para el endpoint de lectura del
     * ADMIN.
     *
     * @return el ajuste vigente (DTO)
     */
    @Transactional(readOnly = true)
    public SystemSettingsResponse current() {
        return SystemSettingsResponse.from(settingsRepository.find().orElseGet(SystemSettings::defaults));
    }

    /**
     * Cambia el modo de aprobacion global, registra el actor y el instante y dispara la
     * auditoria del cambio.
     *
     * @param mode       nuevo modo de aprobacion global
     * @param adminLogin login del administrador que ejecuta el cambio (principal de la sesion)
     * @return el ajuste actualizado (DTO)
     * @throws EntityNotFoundException si el login de sesion no corresponde a ningun empleado
     */
    @Transactional
    public SystemSettingsResponse updateApprovalMode(ApprovalMode mode, String adminLogin) {
        Long actorId = resolveEmployeeId(adminLogin);
        SystemSettings settings = settingsRepository.find().orElseGet(SystemSettings::defaults);
        settings.changeMode(mode, actorId, clock.now());
        SystemSettingsResponse response =
                SystemSettingsResponse.from(settingsRepository.save(settings));
        eventPublisher.publishEvent(new SystemSettingsAuditEvent(response));
        return response;
    }

    private Long resolveEmployeeId(String login) {
        return employeeRepository.findByLogin(login)
                .map(Employee::getId)
                .orElseThrow(() -> new EntityNotFoundException(MSG_ACTOR_NOT_FOUND + login));
    }
}
