package com.aleatica.parking.employee.bootstrap;

import com.aleatica.parking.auth.domain.ClockPort;
import com.aleatica.parking.employee.AuthOrigin;
import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeCategory;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.employee.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Bootstrap idempotente del primer administrador al arrancar la aplicacion.
 *
 * <p>Se ejecuta como {@link ApplicationRunner} (despues de que Flyway haya migrado
 * el esquema y el contexto este listo). Solo actua si {@code parking.bootstrap.admin.enabled=true}
 * y aun no existe ningun empleado con rol {@code ADMIN}: en ese caso crea el admin
 * inicial con las credenciales de {@link AdminBootstrapProperties}, hasheando la
 * contrasena con el {@link PasswordEncoder} de la aplicacion (BCrypt coste 12) y
 * marcando {@code passwordMustChange=true} para forzar el cambio en el primer login.</p>
 *
 * <p>Idempotente y fail-safe: si ya hay un admin, faltan credenciales o el email/login
 * ya estan en uso, registra un mensaje y termina sin lanzar excepciones que tumben el
 * arranque. Reutiliza el repositorio de dominio ({@link EmployeeRepository}) en lugar de
 * SQL crudo.</p>
 */
@Component
@EnableConfigurationProperties(AdminBootstrapProperties.class)
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private static final String LOG_PREFIX = "[admin-bootstrap] ";
    private static final String MSG_MISSING_CREDENTIALS =
            LOG_PREFIX + "Activado pero faltan email/password; no se crea ningun admin (no-op).";
    private static final String MSG_ALREADY_EXISTS =
            LOG_PREFIX + "Ya existe al menos un administrador; no se crea ninguno (no-op).";
    private static final String MSG_LOGIN_TAKEN =
            LOG_PREFIX + "El login '{}' o el email ya estan en uso; no se crea el admin (no-op).";
    private static final String MSG_CREATED =
            LOG_PREFIX + "Administrador inicial creado (login='{}', email='{}'). "
                    + "Debera cambiar la contrasena en el primer inicio de sesion.";

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final ClockPort clock;
    private final AdminBootstrapProperties properties;

    /**
     * @param employeeRepository repositorio de empleados (adaptador de salida)
     * @param passwordEncoder    codificador BCrypt de la aplicacion (coste 12)
     * @param clock              reloj inyectable (UTC)
     * @param properties         propiedades del bootstrap ({@code parking.bootstrap.admin})
     */
    public AdminBootstrapRunner(
            EmployeeRepository employeeRepository,
            PasswordEncoder passwordEncoder,
            ClockPort clock,
            AdminBootstrapProperties properties) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.properties = properties;
    }

    /**
     * Crea el administrador inicial si procede. Ver la logica de guardas en la
     * descripcion de la clase. Nunca propaga excepciones de negocio.
     *
     * @param args argumentos de arranque (no usados)
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.enabled()) {
            return;
        }
        if (!hasCredentials()) {
            LOG.warn(MSG_MISSING_CREDENTIALS);
            return;
        }
        if (employeeRepository.existsByRole(Role.ADMIN)) {
            LOG.info(MSG_ALREADY_EXISTS);
            return;
        }
        createInitialAdmin();
    }

    private boolean hasCredentials() {
        return StringUtils.hasText(properties.email())
                && StringUtils.hasText(properties.password());
    }

    private void createInitialAdmin() {
        String login = resolveLogin();
        String email = properties.email().trim();
        if (employeeRepository.existsByLogin(login) || employeeRepository.existsByEmail(email)) {
            LOG.warn(MSG_LOGIN_TAKEN, login);
            return;
        }
        Employee admin = Employee.register(
                properties.firstName(), properties.lastName(), login,
                email, Role.ADMIN, AuthOrigin.LOCAL, EmployeeCategory.EMPLEADO);
        admin.setPasswordHash(passwordEncoder.encode(properties.password()));
        admin.setPasswordMustChange(true);
        admin.setLastPasswordChangeAt(clock.now());
        if (StringUtils.hasText(properties.phone())) {
            admin.setMobilePhone(properties.phone().trim());
        }
        employeeRepository.save(admin);
        LOG.info(MSG_CREATED, login, email);
    }

    /**
     * Resuelve el login: usa el configurado o, en su defecto, la parte local del
     * email (antes de la {@code @}), truncada a 100 caracteres (limite de columna).
     */
    private String resolveLogin() {
        if (StringUtils.hasText(properties.login())) {
            return properties.login().trim();
        }
        String email = properties.email().trim();
        int at = email.indexOf('@');
        String local = at > 0 ? email.substring(0, at) : email;
        return local.length() > 100 ? local.substring(0, 100) : local;
    }
}
