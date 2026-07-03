package com.aleatica.parking.auth.application;

import com.aleatica.parking.auth.domain.ClockPort;
import com.aleatica.parking.auth.domain.LoginPhase;
import com.aleatica.parking.auth.domain.LoginResult;
import com.aleatica.parking.auth.domain.PasswordPolicy;
import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso de autenticacion local de Fase 1: verificacion BCrypt, contador de
 * intentos fallidos, bloqueo temporal y cambio de contrasena con politica.
 *
 * <p>Implementa el algoritmo de {@code docs/security-design.md} §2. El reloj es
 * un puerto inyectable ({@link ClockPort}) para testear la ventana de bloqueo sin
 * {@code Thread.sleep}. Los mensajes de fallo son genericos
 * ({@link AuthenticationFailedException}); el detalle fino solo va a
 * {@code login_log}.</p>
 */
@Service
public class AuthService {

    /** Numero de intentos fallidos consecutivos que disparan el bloqueo. */
    public static final int MAX_FAILED_ATTEMPTS = 5;

    /** Duracion del bloqueo temporal de la cuenta. */
    public static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginLogRecorder loginLogRecorder;
    private final ClockPort clock;

    /**
     * @param employeeRepository repositorio de empleados
     * @param passwordEncoder    codificador BCrypt (coste 12)
     * @param loginLogRecorder   registro de intentos de login
     * @param clock              reloj inyectable
     */
    public AuthService(
            EmployeeRepository employeeRepository,
            PasswordEncoder passwordEncoder,
            LoginLogRecorder loginLogRecorder,
            ClockPort clock) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginLogRecorder = loginLogRecorder;
        this.clock = clock;
    }

    /**
     * Autentica a un empleado por login y contrasena (Fase 1).
     *
     * <p>No es {@code @Transactional} a proposito: el incremento del contador de
     * fallos y el registro en {@code login_log} DEBEN persistir aunque el intento
     * termine en excepcion. Una transaccion unica los revertiria al lanzar
     * {@link AuthenticationFailedException}, anulando el bloqueo por fuerza bruta.
     * Cada escritura usa su propia transaccion (Spring Data por defecto).</p>
     *
     * @param login    login del empleado
     * @param password contrasena en claro
     * @return la identidad autenticada
     * @throws AuthenticationFailedException si las credenciales no son validas,
     *         la cuenta esta bloqueada o esta inactiva (mensaje siempre generico)
     */
    public AuthenticatedUser authenticate(String login, String password) {
        Optional<Employee> found = employeeRepository.findByLogin(login);
        if (found.isEmpty()) {
            loginLogRecorder.record(login, null, LoginResult.INVALID_CREDENTIALS, LoginPhase.PHASE_1);
            throw new AuthenticationFailedException();
        }
        Employee employee = found.get();
        Instant now = clock.now();

        rejectIfLocked(login, employee, now);
        rejectIfInactive(login, employee);
        verifyPasswordOrCountFailure(login, employee, password, now);

        resetFailureCounter(employee);
        loginLogRecorder.record(login, employee.getId(), LoginResult.OK, LoginPhase.PHASE_1);
        return toAuthenticatedUser(employee);
    }

    /**
     * Cambia la propia contrasena del empleado autenticado.
     *
     * @param login           login del empleado autenticado
     * @param currentPassword contrasena actual (debe coincidir)
     * @param newPassword     nueva contrasena (debe cumplir la politica)
     * @throws InvalidCurrentPasswordException si {@code currentPassword} no coincide
     * @throws PasswordPolicyException         si {@code newPassword} incumple la politica
     */
    @Transactional
    public void changePassword(String login, String currentPassword, String newPassword) {
        Employee employee = employeeRepository.findByLogin(login)
                .orElseThrow(AuthenticationFailedException::new);

        if (employee.getPasswordHash() == null
                || !passwordEncoder.matches(currentPassword, employee.getPasswordHash())) {
            throw new InvalidCurrentPasswordException();
        }

        List<String> violations =
                PasswordPolicy.validate(newPassword, employee.getLogin(), employee.getEmail());
        if (!violations.isEmpty()) {
            throw new PasswordPolicyException(violations);
        }

        employee.setPasswordHash(passwordEncoder.encode(newPassword));
        employee.setPasswordMustChange(false);
        employee.setLastPasswordChangeAt(clock.now());
        employeeRepository.save(employee);
    }

    /**
     * Carga la identidad de un empleado ya autenticado por su login, sin
     * reverificar credenciales. Lo usa {@code /auth/me} para reconstruir el
     * {@code CurrentUser} a partir del principal de la sesion.
     *
     * @param login login del empleado autenticado
     * @return la identidad del empleado
     * @throws AuthenticationFailedException si el empleado ya no existe
     */
    @Transactional(readOnly = true)
    public AuthenticatedUser loadByLogin(String login) {
        Employee employee = employeeRepository.findByLogin(login)
                .orElseThrow(AuthenticationFailedException::new);
        return toAuthenticatedUser(employee);
    }

    private void rejectIfLocked(String login, Employee employee, Instant now) {
        if (employee.isLockedAt(now)) {
            loginLogRecorder.record(login, employee.getId(), LoginResult.LOCKED, LoginPhase.PHASE_1);
            throw new AuthenticationFailedException();
        }
    }

    private void rejectIfInactive(String login, Employee employee) {
        if (!employee.isLoginAllowed()) {
            loginLogRecorder.record(login, employee.getId(), LoginResult.INACTIVE, LoginPhase.PHASE_1);
            throw new AuthenticationFailedException();
        }
    }

    private void verifyPasswordOrCountFailure(
            String login, Employee employee, String password, Instant now) {
        boolean matches = employee.getPasswordHash() != null
                && passwordEncoder.matches(password, employee.getPasswordHash());
        if (matches) {
            return;
        }
        registerFailedAttempt(login, employee, now);
        throw new AuthenticationFailedException();
    }

    private void registerFailedAttempt(String login, Employee employee, Instant now) {
        int attempts = employee.getFailedLoginAttempts() + 1;
        employee.setFailedLoginAttempts(attempts);
        LoginResult result = LoginResult.INVALID_CREDENTIALS;
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            employee.setLockedUntil(now.plus(LOCK_DURATION));
            result = LoginResult.LOCKED;
        }
        employeeRepository.save(employee);
        loginLogRecorder.record(login, employee.getId(), result, LoginPhase.PHASE_1);
    }

    private void resetFailureCounter(Employee employee) {
        if (employee.getFailedLoginAttempts() != 0 || employee.getLockedUntil() != null) {
            employee.setFailedLoginAttempts(0);
            employee.setLockedUntil(null);
            employeeRepository.save(employee);
        }
    }

    private AuthenticatedUser toAuthenticatedUser(Employee employee) {
        return new AuthenticatedUser(
                employee.getId(),
                employee.getLogin(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getRole(),
                employee.isPasswordMustChange());
    }
}
