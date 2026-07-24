package com.aleatica.parking.employee.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.aleatica.parking.auth.domain.ClockPort;
import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeCategory;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.employee.Role;
import com.aleatica.parking.employee.dto.EmployeeCreateRequest;
import com.aleatica.parking.employee.dto.EmployeeOptionResponse;
import com.aleatica.parking.employee.dto.EmployeeResetPasswordResponse;
import com.aleatica.parking.employee.dto.EmployeeResponse;
import com.aleatica.parking.employee.dto.EmployeeUpdateRequest;
import com.aleatica.parking.support.EmployeeTestFactory;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Tests unitarios de {@link EmployeeService} con repositorio y colaboradores
 * mockeados: alta con unicidad, edicion con colision excluyendo al propio,
 * baja/reactivacion y reset segun fase. No toca BD ni el reloj del sistema.
 */
@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    private static final Long ID = 7L;
    private static final String LOGIN = "jperez";
    private static final String EMAIL = "jperez@aleatica.com";
    private static final String OTHER_EMAIL = "otro@aleatica.com";
    private static final Instant NOW = Instant.parse("2026-07-03T10:00:00Z");
    private static final String HASH = "$2a$12$hash";

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ClockPort clock;
    @Mock
    private TemporaryPasswordGenerator temporaryPasswordGenerator;
    @Mock
    private PasswordResetNotifier passwordResetNotifier;

    private EmployeeService phase1Service;

    @BeforeEach
    void setUp() {
        phase1Service = newService(Phase.PHASE_1);
    }

    private EmployeeService newService(Phase phase) {
        return new EmployeeService(
                employeeRepository, passwordEncoder, clock,
                temporaryPasswordGenerator, passwordResetNotifier, phase);
    }

    @Test
    void shouldCreateEmployee_whenLoginAndEmailAreUnique() {
        // Arrange
        given(employeeRepository.existsByLogin(LOGIN)).willReturn(false);
        given(employeeRepository.existsByEmail(EMAIL)).willReturn(false);
        given(employeeRepository.save(any(Employee.class))).willAnswer(inv -> inv.getArgument(0));

        // Act
        EmployeeResponse created = phase1Service.create(validCreateRequest());

        // Assert
        assertThat(created.login()).isEqualTo(LOGIN);
        assertThat(created.email()).isEqualTo(EMAIL);
        assertThat(created.active()).isTrue();
        assertThat(created.enabled()).isTrue();
        assertThat(created.category()).isEqualTo(EmployeeCategory.DIRECTOR_N1);
    }

    @Test
    void shouldThrowConflict_whenCreatingEmployeeWithExistingLogin() {
        // Arrange
        given(employeeRepository.existsByLogin(LOGIN)).willReturn(true);

        // Act / Assert
        assertThatThrownBy(() -> phase1Service.create(validCreateRequest()))
                .isInstanceOf(EmployeeConflictException.class)
                .extracting(ex -> ((EmployeeConflictException) ex).getField())
                .isEqualTo("login");
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void shouldThrowConflict_whenCreatingEmployeeWithExistingEmail() {
        // Arrange
        given(employeeRepository.existsByLogin(LOGIN)).willReturn(false);
        given(employeeRepository.existsByEmail(EMAIL)).willReturn(true);

        // Act / Assert
        assertThatThrownBy(() -> phase1Service.create(validCreateRequest()))
                .isInstanceOf(EmployeeConflictException.class)
                .extracting(ex -> ((EmployeeConflictException) ex).getField())
                .isEqualTo("email");
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void shouldUpdateEmployee_whenDataIsValid() {
        // Arrange
        given(employeeRepository.findById(ID)).willReturn(java.util.Optional.of(existing()));
        given(employeeRepository.existsByEmailAndIdNot(OTHER_EMAIL, ID)).willReturn(false);
        given(clock.now()).willReturn(NOW);
        given(employeeRepository.save(any(Employee.class))).willAnswer(inv -> inv.getArgument(0));

        // Act
        EmployeeResponse updated = phase1Service.update(ID, validUpdateRequest(OTHER_EMAIL));

        // Assert
        assertThat(updated.email()).isEqualTo(OTHER_EMAIL);
        assertThat(updated.firstName()).isEqualTo("Nuevo");
        assertThat(updated.updatedAt()).isEqualTo(NOW);
        assertThat(updated.category()).isEqualTo(EmployeeCategory.GERENTE);
    }

    @Test
    void shouldThrowConflict_whenUpdatingEmailToAnotherEmployeeEmail() {
        // NOTE: EmployeeUpdate no incluye 'login' (inmutable por contrato openapi);
        // la colision de unicidad editable aplica al 'email' (task 1.6).
        // Arrange
        given(employeeRepository.findById(ID)).willReturn(java.util.Optional.of(existing()));
        given(employeeRepository.existsByEmailAndIdNot(OTHER_EMAIL, ID)).willReturn(true);

        // Act / Assert
        assertThatThrownBy(() -> phase1Service.update(ID, validUpdateRequest(OTHER_EMAIL)))
                .isInstanceOf(EmployeeConflictException.class)
                .extracting(ex -> ((EmployeeConflictException) ex).getField())
                .isEqualTo("email");
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void shouldThrowNotFound_whenUpdatingMissingEmployee() {
        // Arrange
        given(employeeRepository.findById(ID)).willReturn(java.util.Optional.empty());

        // Act / Assert
        assertThatThrownBy(() -> phase1Service.update(ID, validUpdateRequest(OTHER_EMAIL)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void shouldSetActiveFalse_whenDeactivatingEmployee() {
        // Arrange
        Employee employee = existing();
        given(employeeRepository.findById(ID)).willReturn(java.util.Optional.of(employee));

        // Act
        phase1Service.deactivate(ID);

        // Assert
        assertThat(employee.isActive()).isFalse();
        verify(employeeRepository).save(employee);
    }

    @Test
    void shouldSetActiveTrue_whenReactivatingEmployee() {
        // Arrange
        Employee employee = existing();
        employee.setActive(false);
        given(employeeRepository.findById(ID)).willReturn(java.util.Optional.of(employee));

        // Act
        phase1Service.reactivate(ID);

        // Assert
        assertThat(employee.isActive()).isTrue();
        verify(employeeRepository).save(employee);
    }

    @Test
    void shouldReturnTempPasswordAndSetMustChange_whenResettingInPhase1() {
        // Arrange
        Employee employee = existing();
        String temp = "TempPass#12Word";
        given(employeeRepository.findById(ID)).willReturn(java.util.Optional.of(employee));
        given(temporaryPasswordGenerator.generate()).willReturn(temp);
        given(passwordEncoder.encode(temp)).willReturn(HASH);
        given(clock.now()).willReturn(NOW);

        // Act
        EmployeeResetPasswordResponse response = phase1Service.resetPassword(ID);

        // Assert
        assertThat(response.temporaryPassword()).isEqualTo(temp);
        assertThat(response.mustChange()).isTrue();
        assertThat(employee.isPasswordMustChange()).isTrue();
        assertThat(employee.getPasswordHash()).isEqualTo(HASH);
        verify(passwordResetNotifier, never()).notifyReset(any(), any());
    }

    @Test
    void shouldSendEmailAndSetMustChange_whenResettingInPhase2() {
        // Arrange
        EmployeeService phase2Service = newService(Phase.PHASE_2);
        Employee employee = existing();
        String temp = "TempPass#34Word";
        given(employeeRepository.findById(ID)).willReturn(java.util.Optional.of(employee));
        given(temporaryPasswordGenerator.generate()).willReturn(temp);
        given(passwordEncoder.encode(temp)).willReturn(HASH);
        given(clock.now()).willReturn(NOW);

        // Act
        EmployeeResetPasswordResponse response = phase2Service.resetPassword(ID);

        // Assert
        assertThat(response.temporaryPassword()).isNull();
        assertThat(response.mustChange()).isTrue();
        assertThat(employee.isPasswordMustChange()).isTrue();
        verify(passwordResetNotifier).notifyReset(eq(employee), eq(temp));
    }

    @Test
    void shouldReturnCategory_whenListingSelectableEmployeesForRelease() {
        // Arrange
        Employee employee = existing();
        EmployeeTestFactory.set(employee, "category", EmployeeCategory.GERENTE);
        given(employeeRepository.findByActiveTrueOrderByFirstNameAscLastNameAsc())
                .willReturn(List.of(employee));

        // Act
        List<EmployeeOptionResponse> options = phase1Service.listSelectableForRelease();

        // Assert
        assertThat(options).singleElement().satisfies(option -> {
            assertThat(option.id()).isEqualTo(ID);
            assertThat(option.category()).isEqualTo(EmployeeCategory.GERENTE);
        });
    }

    private EmployeeCreateRequest validCreateRequest() {
        return new EmployeeCreateRequest(
                "Juan", "Perez", LOGIN, EMAIL,
                "IT", "600100200", "1234ABC", true, null, Role.EMPLOYEE,
                EmployeeCategory.DIRECTOR_N1);
    }

    private EmployeeUpdateRequest validUpdateRequest(String email) {
        return new EmployeeUpdateRequest(
                "Nuevo", "Apellido", email,
                "RRHH", "600300400", "5678DEF", false, Role.ADMIN,
                EmployeeCategory.GERENTE);
    }

    private Employee existing() {
        return EmployeeTestFactory.active(ID, LOGIN, EMAIL, HASH, Role.EMPLOYEE);
    }
}
