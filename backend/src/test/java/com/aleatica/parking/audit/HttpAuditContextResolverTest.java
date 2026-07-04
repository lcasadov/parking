package com.aleatica.parking.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.employee.Role;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Tests unitarios del resolutor de contexto de auditoria: resuelve actor + IP + user-agent
 * cuando hay autenticacion y peticion, y devuelve el contexto del sistema (actor nulo) cuando
 * no los hay (accion del sistema, spec Req 3).
 */
@ExtendWith(MockitoExtension.class)
class HttpAuditContextResolverTest {

    private static final String LOGIN = "admin";

    @Mock
    private EmployeeRepository employeeRepository;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void should_resolve_actor_and_request_metadata_when_authenticated() {
        // Given
        HttpAuditContextResolver resolver = new HttpAuditContextResolver(employeeRepository);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(LOGIN, "n/a", java.util.List.of()));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        request.addHeader("User-Agent", "JUnit");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        given(employeeRepository.findByLogin(LOGIN)).willReturn(Optional.of(employee()));

        // When
        AuditContext context = resolver.resolve();

        // Then
        assertThat(context.actorEmployeeId()).isEqualTo(42L);
        assertThat(context.actorLogin()).isEqualTo(LOGIN);
        assertThat(context.ip()).isEqualTo("10.0.0.5");
        assertThat(context.userAgent()).isEqualTo("JUnit");
    }

    @Test
    void should_return_system_context_when_not_authenticated() {
        // Given: sin autenticacion en el contexto (accion del sistema)
        HttpAuditContextResolver resolver = new HttpAuditContextResolver(employeeRepository);

        // When
        AuditContext context = resolver.resolve();

        // Then
        assertThat(context.actorEmployeeId()).isNull();
        assertThat(context.actorLogin()).isNull();
        assertThat(context.ip()).isNull();
    }

    private static Employee employee() {
        Employee employee = Employee.register(
                "Ada", "Admin", LOGIN, "admin@aleatica.com", Role.ADMIN,
                com.aleatica.parking.employee.AuthOrigin.LOCAL);
        org.springframework.test.util.ReflectionTestUtils.setField(employee, "id", 42L);
        return employee;
    }
}
