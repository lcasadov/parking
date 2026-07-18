package com.aleatica.parking.employee.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.aleatica.parking.auth.domain.ClockPort;
import com.aleatica.parking.employee.AuthOrigin;
import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.employee.Role;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Tests unitarios de {@link AdminBootstrapRunner} con repositorio, encoder y reloj
 * mockeados. Cubre el opt-in, la idempotencia (no duplica si ya hay ADMIN), el
 * no-op por credenciales ausentes, el hasheo de la contrasena y el flag de cambio
 * forzado. No toca BD ni el reloj del sistema (S2925: sin {@code Thread.sleep}).
 */
@ExtendWith(MockitoExtension.class)
class AdminBootstrapRunnerTest {

    private static final String EMAIL = "admin@parking.aleatica.com";
    private static final String PLAIN_PASSWORD = "Admin#Parking2026";
    private static final String HASHED_PASSWORD = "$2a$12$hashedvalue";
    private static final String DERIVED_LOGIN = "admin";
    private static final Instant NOW = Instant.parse("2026-07-18T10:00:00Z");

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ClockPort clock;

    private AdminBootstrapRunner runnerFor(AdminBootstrapProperties properties) {
        return new AdminBootstrapRunner(employeeRepository, passwordEncoder, clock, properties);
    }

    private AdminBootstrapProperties enabledProps() {
        return new AdminBootstrapProperties(
                true, EMAIL, PLAIN_PASSWORD, "Admin", "Parking", null, null);
    }

    @Test
    void shouldCreateAdmin_whenEnabledAndNoAdminExists() {
        // Arrange
        given(employeeRepository.existsByRole(Role.ADMIN)).willReturn(false);
        given(employeeRepository.existsByLogin(DERIVED_LOGIN)).willReturn(false);
        given(employeeRepository.existsByEmail(EMAIL)).willReturn(false);
        given(passwordEncoder.encode(PLAIN_PASSWORD)).willReturn(HASHED_PASSWORD);
        given(clock.now()).willReturn(NOW);

        // Act
        runnerFor(enabledProps()).run(null);

        // Assert
        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(captor.capture());
        Employee saved = captor.getValue();
        assertThat(saved.getRole()).isEqualTo(Role.ADMIN);
        assertThat(saved.getAuthOrigin()).isEqualTo(AuthOrigin.LOCAL);
        assertThat(saved.getLogin()).isEqualTo(DERIVED_LOGIN);
        assertThat(saved.getEmail()).isEqualTo(EMAIL);
        assertThat(saved.getPasswordHash()).isEqualTo(HASHED_PASSWORD);
        assertThat(saved.getPasswordHash()).isNotEqualTo(PLAIN_PASSWORD);
        assertThat(saved.isPasswordMustChange()).isTrue();
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.isEnabled()).isTrue();
        assertThat(saved.getLastPasswordChangeAt()).isEqualTo(NOW);
    }

    @Test
    void shouldUseConfiguredLoginAndPhone_whenProvided() {
        // Arrange
        AdminBootstrapProperties props = new AdminBootstrapProperties(
                true, EMAIL, PLAIN_PASSWORD, "Ada", "Lovelace", "600123456", "root");
        given(employeeRepository.existsByRole(Role.ADMIN)).willReturn(false);
        given(employeeRepository.existsByLogin("root")).willReturn(false);
        given(employeeRepository.existsByEmail(EMAIL)).willReturn(false);
        given(passwordEncoder.encode(PLAIN_PASSWORD)).willReturn(HASHED_PASSWORD);
        given(clock.now()).willReturn(NOW);

        // Act
        runnerFor(props).run(null);

        // Assert
        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(captor.capture());
        Employee saved = captor.getValue();
        assertThat(saved.getLogin()).isEqualTo("root");
        assertThat(saved.getFirstName()).isEqualTo("Ada");
        assertThat(saved.getLastName()).isEqualTo("Lovelace");
        assertThat(saved.getMobilePhone()).isEqualTo("600123456");
    }

    @Test
    void shouldNotCreateAdmin_whenAdminAlreadyExists() {
        // Arrange
        given(employeeRepository.existsByRole(Role.ADMIN)).willReturn(true);

        // Act
        runnerFor(enabledProps()).run(null);

        // Assert
        verify(employeeRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void shouldBeNoOp_whenEnabledButCredentialsMissing() {
        // Arrange
        AdminBootstrapProperties props = new AdminBootstrapProperties(
                true, "  ", null, "Admin", "Parking", null, null);

        // Act
        runnerFor(props).run(null);

        // Assert
        verifyNoInteractions(employeeRepository, passwordEncoder, clock);
    }

    @Test
    void shouldBeNoOp_whenDisabledByDefault() {
        // Arrange
        AdminBootstrapProperties props = new AdminBootstrapProperties(
                false, EMAIL, PLAIN_PASSWORD, "Admin", "Parking", null, null);

        // Act
        runnerFor(props).run(null);

        // Assert
        verifyNoInteractions(employeeRepository, passwordEncoder, clock);
    }

    @Test
    void shouldBeNoOp_whenLoginOrEmailAlreadyTaken() {
        // Arrange
        given(employeeRepository.existsByRole(Role.ADMIN)).willReturn(false);
        given(employeeRepository.existsByLogin(DERIVED_LOGIN)).willReturn(true);

        // Act
        runnerFor(enabledProps()).run(null);

        // Assert
        verify(employeeRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }
}
