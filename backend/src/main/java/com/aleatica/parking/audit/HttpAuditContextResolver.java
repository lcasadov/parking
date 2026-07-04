package com.aleatica.parking.audit;

import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Adaptador de {@link AuditContextResolver} respaldado por el contexto de seguridad de
 * Spring y la peticion HTTP en curso.
 *
 * <p>Resuelve el login del actor desde {@link SecurityContextHolder}, su id de empleado
 * consultando {@link EmployeeRepository}, y la IP/user-agent desde la peticion asociada al
 * hilo ({@link RequestContextHolder}). Cuando no hay autenticacion o no hay peticion (accion
 * del sistema, p. ej. la purga programada) devuelve {@link AuditContext#system()}: el actor
 * de la entrada queda vacio (spec Req 3, actor del sistema).</p>
 */
@Component
public class HttpAuditContextResolver implements AuditContextResolver {

    private static final String HEADER_USER_AGENT = "User-Agent";

    private final EmployeeRepository employeeRepository;

    /**
     * @param employeeRepository repositorio para resolver el id del empleado por su login
     */
    public HttpAuditContextResolver(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public AuditContext resolve() {
        String login = currentLogin();
        if (login == null) {
            return AuditContext.system();
        }
        Long actorId = employeeRepository.findByLogin(login).map(Employee::getId).orElse(null);
        HttpServletRequest request = currentRequest();
        String ip = request == null ? null : request.getRemoteAddr();
        String userAgent = request == null ? null : request.getHeader(HEADER_USER_AGENT);
        return new AuditContext(actorId, login, ip, userAgent);
    }

    private static String currentLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String name = authentication.getName();
        if (name == null || "anonymousUser".equals(name)) {
            return null;
        }
        return name;
    }

    private static HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }
}
